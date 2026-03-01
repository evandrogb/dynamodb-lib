package com.dynamodb.lib.repository

import com.dynamodb.lib.entity.DynamoEntity
import com.dynamodb.lib.model.PageResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Implementação base genérica para repositórios DynamoDB
 *
 * Exemplo de uso:
 * ```
 * class OrderRepository(
 *     enhancedClient: DynamoDbEnhancedAsyncClient,
 *     dynamoClient: DynamoDbAsyncClient
 * ) : BaseRepository<Order>(
 *     tableName = "orders",
 *     entityClass = Order::class.java,
 *     enhancedClient = enhancedClient,
 *     dynamoClient = dynamoClient
 * )
 * ```
 */
abstract class BaseRepository<T : DynamoEntity>(
    protected val tableName: String,
    protected val entityClass: Class<T>,
    protected val enhancedClient: DynamoDbEnhancedAsyncClient,
    protected val dynamoClient: DynamoDbAsyncClient
) : DynamoRepository<T> {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    protected val table = enhancedClient.table(
        tableName,
        TableSchema.fromBean(entityClass)
    )

    override suspend fun save(entity: T) {
        runCatching {
            require(!entity.id.isNullOrBlank()) { "Entity ID cannot be null or blank" }
            table.putItem(entity).await()
        }
        .onSuccess {
            logger.debug("Entity saved successfully: ${entity.id} in table $tableName")
        }
        .onFailure { e ->
            logger.error("Error saving entity: ${entity.id} in table $tableName", e)
        }
        .getOrThrow()
    }

    override suspend fun getById(id: String): T? {
        return runCatching {
            table.getItem { it.key { kb -> kb.partitionValue(id) } }.await()
        }
        .onSuccess { item ->
            if (item != null) {
                logger.debug("Entity retrieved: $id from table $tableName")
            }
        }
        .onFailure { e ->
            logger.error("Error retrieving entity: $id from table $tableName", e)
        }
        .getOrNull()
    }

    override suspend fun update(entity: T) {
        // DynamoDB putItem with overwrite = update
        save(entity)
    }

    override suspend fun deleteById(id: String) {
        runCatching {
            table.deleteItem { it.key { kb -> kb.partitionValue(id) } }.await()
        }
        .onSuccess {
            logger.debug("Entity deleted: $id from table $tableName")
        }
        .onFailure { e ->
            logger.error("Error deleting entity: $id from table $tableName", e)
        }
        .getOrThrow()
    }

    override suspend fun getAll(): List<T> {
        return runCatching {
            table.scan()
                .asFlow()
                .toList()           // ← Coleta TODAS as páginas (não só .first())
                .flatMap { it.items() }
        }
        .onSuccess { items ->
            logger.debug("Retrieved ${items.size} items from table $tableName")
        }
        .onFailure { e ->
            logger.error("Error retrieving all items from table $tableName", e)
        }
        .getOrThrow()
    }

    /**
     * Paginação com suporte nativo a sorting
     *
     * Estratégia:
     * - Sem sortBy: Scan nativo (O(limit) - eficiente)
     * - Com sortBy: Carrega nativamente todos os itens, ordena, pagina em memória (O(n))
     */
    override suspend fun getAllPaginated(
        limit: Int,
        cursor: String?,
        sortBy: String?,
        sortOrder: SortOrder
    ): PageResult<T> {
        return if (sortBy.isNullOrBlank()) {
            scanNative(limit, cursor)
        } else {
            scanSorted(limit, cursor, sortBy, sortOrder)
        }
    }

    /**
     * Caminho 1: Scan nativo sem sorting
     * Usa ScanEnhancedRequest com limit + exclusiveStartKey
     * Eficiente: O(limit) — lê apenas o necessário do DynamoDB
     */
    private suspend fun scanNative(limit: Int, cursor: String?): PageResult<T> {
        return runCatching {
            val requestBuilder = ScanEnhancedRequest.builder().limit(limit)

            // Restaurar exclusiveStartKey se houver cursor nativo
            cursor?.let { c ->
                if (c.isNativeCursor()) {
                    c.toNativeLastEvaluatedKey()?.let { requestBuilder.exclusiveStartKey(it) }
                }
            }

            val page = table.scan(requestBuilder.build()).asFlow().first()
            val items = page.items()
            val totalItems = count()

            // Serializar LastEvaluatedKey para Base64 cursor
            val nextCursor = page.lastEvaluatedKey()
                ?.takeIf { it.isNotEmpty() }
                ?.toNativeCursor()

            PageResult(
                items = items,
                itemCount = items.size,
                totalItems = totalItems,
                nextCursor = nextCursor,
                previousCursor = cursor  // ← Indica que há página anterior se cursor foi fornecido
            )
        }
        .onSuccess { result ->
            logger.debug("Retrieved ${result.itemCount}/${result.totalItems} items from $tableName via native scan")
        }
        .onFailure { e ->
            logger.error("Error retrieving paginated items from table $tableName via native scan", e)
        }
        .getOrThrow()
    }

    /**
     * Caminho 2: Coleta nativamente todos os itens, ordena, e pagina em memória
     * Necessário para sorting sem GSI Query
     * O(n) — carrega todos os itens da tabela
     */
    private suspend fun scanSorted(
        limit: Int,
        cursor: String?,
        sortBy: String,
        sortOrder: SortOrder
    ): PageResult<T> {
        return runCatching {
            val allItems = getAll()  // ← Coleta nativamente todas as páginas do DynamoDB
            val totalItems = count()
            val sorted = applySorting(allItems, sortBy, sortOrder)

            // Encontrar índice inicial baseado no cursor
            val startIndex = cursor
                ?.takeIf { !it.isNativeCursor() }
                ?.let { parseCursor(it, sorted) } ?: 0

            // Extrair página
            val endIndex = (startIndex + limit).coerceAtMost(sorted.size)
            val pageItems = sorted.subList(startIndex, endIndex)

            // Calcular próximo cursor
            val nextCursor = if (endIndex < sorted.size) {
                encodeCursor(sorted[endIndex])
            } else {
                null
            }

            PageResult(
                items = pageItems,
                itemCount = pageItems.size,
                totalItems = totalItems,
                nextCursor = nextCursor,
                previousCursor = cursor  // ← Indica que há página anterior se cursor foi fornecido
            )
        }
        .onSuccess { result ->
            logger.debug("Retrieved ${result.itemCount}/${result.totalItems} items from $tableName via sorted scan")
        }
        .onFailure { e ->
            logger.error("Error retrieving paginated items from table $tableName via sorted scan", e)
        }
        .getOrThrow()
    }

    /**
     * Override este método para customizar a ordenação
     * Implementação padrão: suporta 'id' e 'createdAt' (campos comuns de DynamoEntity)
     */
    protected open fun applySorting(
        items: List<T>,
        sortBy: String,
        sortOrder: SortOrder
    ): List<T> {
        logger.debug("Applying sort by '$sortBy' in ${sortOrder.name} order")
        val sorted = when (sortBy.lowercase()) {
            "id" -> items.sortedBy { it.id ?: "" }
            "createdat", "created_at" -> items.sortedBy { it.createdAt ?: 0 }
            else -> {
                logger.warn("Unknown sort field: $sortBy, returning unsorted")
                items
            }
        }
        return if (sortOrder == SortOrder.DESC) sorted.reversed() else sorted
    }

    /**
     * Override este método para customizar o parsing do cursor
     * Implementação padrão: extrai ID do cursor e encontra o índice na lista
     * @param cursor String do cursor (pode ser Base64 ou outro formato)
     * @param items Lista ordenada de items para buscar a posição do cursor
     */
    protected open fun parseCursor(cursor: String, items: List<T>): Int {
        return runCatching {
            val cursorId = cursor.extractCursorId() ?: return 0
            val index = items.indexOfFirst { it.id == cursorId }
            if (index >= 0) index else 0
        }.onFailure { e ->
            logger.warn("Failed to parse cursor: $cursor", e)
        }.getOrDefault(0)
    }

    /**
     * Override este método para customizar a codificação do cursor
     * Implementação padrão: codifica ID em Base64 usando toCursor()
     */
    protected open fun encodeCursor(item: T): String? {
        return item.id?.toCursor()
    }

    override suspend fun count(): Long {
        return runCatching {
            DescribeTableRequest.builder()
                .tableName(tableName)
                .build()
                .let { dynamoClient.describeTable(it).await() }
                .table()
                .itemCount()
        }
        .onSuccess { count ->
            logger.debug("Total items in table $tableName: $count")
        }
        .onFailure { e ->
            logger.error("Error counting items in table $tableName", e)
        }
        .getOrThrow()
    }

    /**
     * Busca UM item por uma propriedade específica (method scan genérico)
     *
     * ⚠️ IMPORTANTE: Para usar, certifique-se que a propriedade tem um índice no DynamoDB!
     *
     * Exemplo:
     * ```
     * val order = getByProperty("email", "user@example.com")
     * val product = getByProperty("sku", "PROD-123")
     * ```
     *
     * @param propertyName Nome da propriedade (campo da entidade)
     * @param value Valor para filtrar
     * @return A primeira entidade que corresponder ao filtro, ou null
     */
    override suspend fun getByProperty(propertyName: String, value: Any): T? {
        return runCatching {
            val allItems = getAll()
            allItems.firstOrNull { item ->
                try {
                    val field = entityClass.getDeclaredField(propertyName)
                    field.isAccessible = true
                    field.get(item) == value
                } catch (e: Exception) {
                    logger.warn("Property '$propertyName' not found on ${entityClass.simpleName}", e)
                    false
                }
            }
        }
        .onSuccess { item ->
            if (item != null) {
                logger.debug("Found item by property '$propertyName'=$value in table $tableName")
            } else {
                logger.debug("No item found by property '$propertyName'=$value in table $tableName")
            }
        }
        .onFailure { e ->
            logger.error("Error searching by property '$propertyName' in table $tableName", e)
        }
        .getOrNull()
    }

    /**
     * Busca TODOS os items que correspondem a uma propriedade específica
     *
     * ⚠️ IMPORTANTE: Para usar com performance, certifique-se que a propriedade tem um índice!
     *
     * Exemplo:
     * ```
     * val orders = getAllByProperty("status", "PENDING")
     * val users = getAllByProperty("country", "BR")
     * ```
     *
     * @param propertyName Nome da propriedade (campo da entidade)
     * @param value Valor para filtrar
     * @return Lista de todas as entidades que correspondem ao filtro
     */
    override suspend fun getAllByProperty(propertyName: String, value: Any): List<T> {
        return runCatching {
            val allItems = getAll()
            allItems.filter { item ->
                try {
                    val field = entityClass.getDeclaredField(propertyName)
                    field.isAccessible = true
                    field.get(item) == value
                } catch (e: Exception) {
                    logger.warn("Property '$propertyName' not found on ${entityClass.simpleName}", e)
                    false
                }
            }
        }
        .onSuccess { items ->
            logger.debug("Found ${items.size} items by property '$propertyName'=$value in table $tableName")
        }
        .onFailure { e ->
            logger.error("Error searching by property '$propertyName' in table $tableName", e)
        }
        .getOrDefault(emptyList())
    }

    /**
     * Busca items com um filtro customizado (usando função lambda)
     *
     * Exemplo:
     * ```
     * val recentOrders = scanWithFilter { item -> item.createdAt > oneWeekAgo }
     * val expensiveItems = scanWithFilter { item -> item.price > 1000 }
     * ```
     *
     * @param predicate Função que retorna true para items que devem ser inclusos
     * @return Lista de items que satisfazem o predicado
     */
    override suspend fun scanWithFilter(predicate: (T) -> Boolean): List<T> {
        return runCatching {
            getAll().filter(predicate)
        }
        .onSuccess { items ->
            logger.debug("Scan with filter returned ${items.size} items from table $tableName")
        }
        .onFailure { e ->
            logger.error("Error executing scan with filter on table $tableName", e)
        }
        .getOrDefault(emptyList())
    }

    /**
     * Busca items com paginação E filtro customizado
     *
     * Exemplo:
     * ```
     * val result = scanWithFilterPaginated(
     *     limit = 20,
     *     predicate = { item -> item.status == "ACTIVE" }
     * )
     * ```
     *
     * @param limit Quantos items retornar
     * @param cursor Cursor para paginação
     * @param predicate Função que retorna true para items que devem ser inclusos
     * @return PageResult com items filtrados e paginados
     */
    override suspend fun scanWithFilterPaginated(
        limit: Int,
        cursor: String?,
        predicate: (T) -> Boolean
    ): PageResult<T> {
        return runCatching {
            val allItems = getAll()
            val filteredItems = allItems.filter(predicate)
            val totalItems = count()

            // Encontrar índice inicial baseado no cursor
            val startIndex = cursor?.let { parseCursor(it, filteredItems) } ?: 0

            // Extrair página
            val endIndex = (startIndex + limit).coerceAtMost(filteredItems.size)
            val pageItems = filteredItems.subList(startIndex, endIndex)

            // Calcular próximo cursor
            val nextCursor = if (endIndex < filteredItems.size && pageItems.isNotEmpty()) {
                encodeCursor(pageItems.last())
            } else {
                null
            }

            PageResult(
                items = pageItems,
                itemCount = pageItems.size,
                totalItems = filteredItems.size.toLong(),
                nextCursor = nextCursor,
                previousCursor = cursor
            )
        }
        .onSuccess { result ->
            logger.debug("Scan with filter paginated returned ${result.itemCount}/${result.totalItems} items from $tableName")
        }
        .onFailure { e ->
            logger.error("Error executing scan with filter paginated on table $tableName", e)
        }
        .getOrThrow()
    }

}