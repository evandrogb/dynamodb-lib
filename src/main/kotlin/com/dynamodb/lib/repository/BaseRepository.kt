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
     */
    protected open fun applySorting(
        items: List<T>,
        sortBy: String,
        sortOrder: SortOrder
    ): List<T> {
        logger.debug("Applying sort by '$sortBy' in ${sortOrder.name} order")
        return items // Override em subclasses
    }

    /**
     * Override este método para customizar o parsing do cursor
     * @param cursor String do cursor (pode ser Base64 ou outro formato)
     * @param items Lista ordenada de items para buscar a posição do cursor
     */
    protected open fun parseCursor(cursor: String, items: List<T>): Int {
        return 0 // Override em subclasses
    }

    /**
     * Override este método para customizar a codificação do cursor
     */
    protected open fun encodeCursor(item: T): String? {
        return item.id // Override em subclasses
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

}