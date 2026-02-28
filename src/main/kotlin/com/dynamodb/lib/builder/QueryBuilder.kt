package com.dynamodb.lib.builder

import com.dynamodb.lib.repository.SortOrder

/**
 * Builder para construir queries de forma fluente
 * Exemplo:
 * ```
 * query {
 *     limit = 10
 *     sortBy = "createdAt"
 *     sortOrder = SortOrder.DESC
 *     cursor = lastCursor
 * }
 * ```
 */
class QueryBuilder {
    var limit: Int = 10
    var cursor: String? = null
    var sortBy: String? = null
    var sortOrder: SortOrder = SortOrder.ASC

    /**
     * Valida o builder
     */
    fun validate() {
        check(limit in 1..100) { "Limit must be between 1 and 100" }
    }

    /**
     * Construir object de query
     */
    fun build(): Query = Query(
        limit = limit,
        cursor = cursor,
        sortBy = sortBy,
        sortOrder = sortOrder
    ).also { it.validate() }

    /**
     * Métodos encadeados para fluent API
     */
    fun withLimit(limit: Int) = apply { this.limit = limit }
    fun withCursor(cursor: String?) = apply { this.cursor = cursor }
    fun sortBy(field: String, order: SortOrder = SortOrder.ASC) = apply {
        this.sortBy = field
        this.sortOrder = order
    }
    fun ascending() = apply { this.sortOrder = SortOrder.ASC }
    fun descending() = apply { this.sortOrder = SortOrder.DESC }
}

/**
 * Data class para representar uma query
 */
data class Query(
    val limit: Int = 10,
    val cursor: String? = null,
    val sortBy: String? = null,
    val sortOrder: SortOrder = SortOrder.ASC
) {
    fun validate() {
        check(limit in 1..100) { "Limit must be between 1 and 100" }
    }
}

/**
 * DSL para criar queries
 */
inline fun query(block: QueryBuilder.() -> Unit): Query =
    QueryBuilder().apply(block).build()

/**
 * Filter builder para queries mais complexas (futuro)
 */
class FilterBuilder {
    private val conditions = mutableListOf<String>()

    fun eq(field: String, value: Any) = apply {
        conditions.add("$field = $value")
    }

    fun gt(field: String, value: Any) = apply {
        conditions.add("$field > $value")
    }

    fun lt(field: String, value: Any) = apply {
        conditions.add("$field < $value")
    }

    fun between(field: String, start: Any, end: Any) = apply {
        conditions.add("$field BETWEEN $start AND $end")
    }

    fun contains(field: String, value: String) = apply {
        conditions.add("contains($field, $value)")
    }

    fun build(): String = conditions.joinToString(" AND ")
}