package com.dynamodb.lib.repository

import com.dynamodb.lib.entity.DynamoEntity
import com.dynamodb.lib.model.PageResult

/**
 * Interface genérica para operações CRUD no DynamoDB
 */
interface DynamoRepository<T : DynamoEntity> {

    /**
     * CREATE: Salva uma entidade
     */
    suspend fun save(entity: T)

    /**
     * CREATE (BATCH): Salva múltiplas entidades
     */
    suspend fun saveAll(entities: List<T>) {
        entities.forEach { save(it) }
    }

    /**
     * READ: Obtém por ID (Partition Key)
     */
    suspend fun getById(id: String): T?

    /**
     * READ (BATCH): Obtém múltiplos itens por IDs
     */
    suspend fun getAllById(ids: List<String>): List<T> {
        return ids.mapNotNull { getById(it) }
    }

    /**
     * UPDATE: Atualiza uma entidade
     */
    suspend fun update(entity: T)

    /**
     * DELETE: Remove uma entidade por ID
     */
    suspend fun deleteById(id: String)

    /**
     * DELETE (BATCH): Remove múltiplas entidades
     */
    suspend fun deleteAll(ids: List<String>) {
        ids.forEach { deleteById(it) }
    }

    /**
     * READ (ALL): Obtém todos os itens (não paginado)
     * ⚠️ Use com cuidado em tabelas grandes!
     */
    suspend fun getAll(): List<T>

    /**
     * READ (PAGINADO): Obtém itens com paginação cursor-based
     *
     * @param limit Quantos itens retornar
     * @param cursor Cursor para buscar itens DEPOIS deste (LastEvaluatedKey)
     * @param sortBy Campo para ordenação (se nulo, retorna em ordem aleatória do DynamoDB)
     * @param sortOrder Direção da ordenação (ASC ou DESC)
     *
     * Response inclui:
     * - nextCursor: Use para buscar próxima página
     * - previousCursor: Informação apenas (para indicar que há página anterior)
     * - hasNextPage/hasPreviousPage: Flags booleanas para UI
     */
    suspend fun getAllPaginated(
        limit: Int = 10,
        cursor: String? = null,
        sortBy: String? = null,
        sortOrder: SortOrder = SortOrder.ASC
    ): PageResult<T>

    /**
     * COUNT: Conta total de itens
     */
    suspend fun count(): Long

    /**
     * EXISTS: Verifica se entidade existe
     */
    suspend fun existsById(id: String): Boolean = getById(id) != null

    /**
     * SEARCH (GENÉRICO): Busca UM item por propriedade
     * ⚠️ Requer índice na propriedade para melhor performance
     */
    suspend fun getByProperty(propertyName: String, value: Any): T?

    /**
     * SEARCH (GENÉRICO): Busca TODOS os items por propriedade
     * ⚠️ Requer índice na propriedade para melhor performance
     */
    suspend fun getAllByProperty(propertyName: String, value: Any): List<T>

    /**
     * SCAN (GENÉRICO): Busca com filtro customizado (lambda)
     * Mais flexível que getByProperty, mas menos performático
     */
    suspend fun scanWithFilter(predicate: (T) -> Boolean): List<T>

    /**
     * SCAN PAGINADO (GENÉRICO): Busca com filtro E paginação
     */
    suspend fun scanWithFilterPaginated(
        limit: Int,
        cursor: String?,
        predicate: (T) -> Boolean
    ): PageResult<T>
}

/**
 * Enum para direção de ordenação
 */
enum class SortOrder {
    ASC, DESC;

    companion object {
        fun fromString(value: String?): SortOrder =
            when (value?.lowercase()) {
                "desc", "descending" -> DESC
                else -> ASC
            }
    }
}

/**
 * Extensão para validação antes de salvar
 */
suspend inline fun <T : DynamoEntity> DynamoRepository<T>.saveIfValid(
    entity: T,
    validate: (T) -> Boolean = { !it.id.isNullOrBlank() }
): Boolean {
    return if (validate(entity)) {
        save(entity)
        true
    } else {
        false
    }
}

/**
 * Extensão para operações em lote
 */
suspend inline fun <T : DynamoEntity> DynamoRepository<T>.batchOperation(
    entities: List<T>,
    operation: suspend (T) -> Unit
) {
    entities.forEach { operation(it) }
}