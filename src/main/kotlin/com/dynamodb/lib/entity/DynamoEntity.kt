package com.dynamodb.lib.entity

/**
 * Interface base para todas as entidades do DynamoDB
 * Garante que cada entidade tem uma chave primária
 */
interface DynamoEntity {
    /**
     * Partition key (Primary Key)
     */
    val id: String?

    /**
     * Timestamp de criação
     */
    val createdAt: Long?

    /**
     * Timestamp de última atualização
     */
    val updatedAt: Long?
}

/**
 * Extensão para validar entidade antes de salvar
 */
fun <T : DynamoEntity> T.validate(): T {
    require(!id.isNullOrBlank()) { "Entity ID cannot be null or blank" }
    return this
}

/**
 * Extensão para adicionar timestamp de criação
 */
fun <T : DynamoEntity> T.withTimestamps(): T {
    val now = System.currentTimeMillis()
    return when (this) {
        is MutableDynamoEntity -> {
            this.createdAt = createdAt ?: now
            this.updatedAt = now
            this as T
        }
        else -> this
    }
}

/**
 * Interface mutável para entidades que suportam atualização de timestamps
 */
interface MutableDynamoEntity : DynamoEntity {
    override var id: String?
    override var createdAt: Long?
    override var updatedAt: Long?
}