package com.dynamodb.lib.model

/**
 * Resultado paginado genérico para qualquer tipo de entidade
 *
 * Suporta cursor-based pagination (padrão nativo do DynamoDB)
 *
 * Exemplo de navegação:
 * 1. GET /items?limit=10
 *    → Retorna: items[1-10], nextCursor, hasNextPage=true
 * 2. GET /items?limit=10&cursor=abc123
 *    → Retorna: items[11-20], nextCursor, hasNextPage=true, previousCursor (mantido pelo cliente)
 * 3. GET /items?limit=10&cursor=def456&previousCursor=abc123
 *    → Retorna: items[21-30], hasPreviousPage=true (indica que há página anterior)
 */
data class PageResult<T>(
    val items: List<T>,
    val itemCount: Int = items.size,
    val totalItems: Long = 0,
    val nextCursor: String? = null,
    val previousCursor: String? = null
) {
    /**
     * Verifica se há próxima página
     */
    val hasNextPage: Boolean
        get() = nextCursor != null

    /**
     * Verifica se há página anterior
     */
    val hasPreviousPage: Boolean
        get() = previousCursor != null

    /**
     * Legado: verifica se há próxima página
     */
    val hasMore: Boolean
        get() = hasNextPage

    /**
     * Legado: verifica se é a última página
     */
    val isLastPage: Boolean
        get() = !hasNextPage

    /**
     * Map os itens para outro tipo
     */
    inline fun <R> map(transform: (T) -> R): PageResult<R> =
        PageResult(
            items = items.map(transform),
            totalItems = totalItems,
            nextCursor = nextCursor,
            previousCursor = previousCursor
        )

    /**
     * Filter os itens
     */
    fun filter(predicate: (T) -> Boolean): PageResult<T> =
        copy(items = items.filter(predicate))

    companion object {
        /**
         * Cria resultado vazio
         */
        fun <T> empty(): PageResult<T> = PageResult(emptyList(), 0, 0, null, null)

        /**
         * Cria resultado com um único item
         */
        fun <T> of(item: T): PageResult<T> = PageResult(listOf(item), 1, 1, null, null)

        /**
         * Cria resultado com lista de itens
         */
        fun <T> of(items: List<T>, totalItems: Long = 0): PageResult<T> =
            PageResult(items, items.size, totalItems, null, null)
    }
}

/**
 * Builder para PageResult
 */
class PageResultBuilder<T> {
    var items: List<T> = emptyList()
    var totalItems: Long = 0
    var nextCursor: String? = null
    var previousCursor: String? = null

    fun build(): PageResult<T> = PageResult(
        items = items,
        itemCount = items.size,
        totalItems = totalItems,
        nextCursor = nextCursor,
        previousCursor = previousCursor
    )
}