package com.ehs.finances.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Resposta padrão da API: tudo chega embrulhado em `DefaultResponse`. */
@Serializable
data class Envelope<T>(
    val message: String? = null,
    val data: T? = null,
    val links: Map<String, String>? = null,
)

typealias EmptyEnvelope = Envelope<JsonElement?>

/** Página devolvida pelos endpoints paginados. */
@Serializable
data class PageDto<T>(
    val items: List<T> = emptyList(),
    val total: Long = 0,
    val page: Int = 0,
    val size: Int = 0,
)

class ApiException(
    override val message: String,
    val status: Int? = null,
) : Exception(message)

fun <T> Envelope<T>.unwrap(): T = data ?: throw ApiException(message ?: "Resposta inesperada da API")
