package com.dynamodb.lib.repository

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Base64
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Extension function para converter Map em Cursor Base64
 */
fun Map<String, String>.toBase64Cursor(mapper: ObjectMapper = ObjectMapper()): String {
    val jsonBytes = mapper.writeValueAsBytes(this)
    return Base64.getEncoder().encodeToString(jsonBytes)
}

/**
 * Extension function para decodificar Cursor Base64
 */
fun String.fromBase64Cursor(mapper: ObjectMapper = ObjectMapper()): Map<String, String>? {
    return runCatching {
        val decodedBytes = Base64.getDecoder().decode(this)
        @Suppress("UNCHECKED_CAST")
        mapper.readValue(decodedBytes, Map::class.java) as Map<String, String>
    }.onFailure { e ->
        // Logging será feito na camada superior
    }.getOrNull()
}

/**
 * Extension function para validar e extrair ID do Cursor
 */
fun String.extractCursorId(): String? = this.fromBase64Cursor()?.get("id")

/**
 * Extension function para criar cursor a partir de um ID
 */
fun String.toCursor(): String = mapOf("id" to this).toBase64Cursor()

/**
 * Serializa LastEvaluatedKey (Map<String, AttributeValue>) para Base64
 * Usado para paginação nativa do DynamoDB sem sorting
 */
fun Map<String, AttributeValue>.toNativeCursor(mapper: ObjectMapper = ObjectMapper()): String {
    val simplified = mapValues { (_, v) ->
        when {
            v.s() != null -> mapOf("S" to v.s()!!)
            v.n() != null -> mapOf("N" to v.n()!!)
            v.b() != null -> mapOf("B" to Base64.getEncoder().encodeToString(v.b().asByteArray()))
            else -> mapOf("S" to v.toString())
        }
    }
    val json = mapper.writeValueAsBytes(mapOf("_type" to "native", "key" to simplified))
    return Base64.getEncoder().encodeToString(json)
}

/**
 * Deserializa Base64 para Map<String, AttributeValue>
 * Usado para restaurar o exclusiveStartKey nativo do DynamoDB
 */
fun String.toNativeLastEvaluatedKey(mapper: ObjectMapper = ObjectMapper()): Map<String, AttributeValue>? {
    return runCatching {
        val json = Base64.getDecoder().decode(this)
        val root = mapper.readValue(json, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        if ((root as Map<String, Any>)["_type"] != "native") return null
        @Suppress("UNCHECKED_CAST")
        val key = (root["key"] as Map<String, Map<String, String>>)
        key.mapValues { (_, v) ->
            when {
                v.containsKey("S") -> AttributeValue.builder().s(v["S"]).build()
                v.containsKey("N") -> AttributeValue.builder().n(v["N"]).build()
                else -> AttributeValue.builder().s(v.values.first()).build()
            }
        }
    }.getOrNull()
}

/**
 * Helper para detectar tipo do cursor
 */
fun String.isNativeCursor(): Boolean =
    runCatching {
        val json = Base64.getDecoder().decode(this)
        ObjectMapper().readTree(json).get("_type")?.asText() == "native"
    }.getOrDefault(false)