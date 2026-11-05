package com.ehs.finances.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/** Executa uma chamada e traduz falhas HTTP para [ApiException] com a `message` do envelope. */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        throw ApiException(e.envelopeMessage(), e.code())
    } catch (e: IOException) {
        throw ApiException("Não foi possível conectar ao servidor.", null)
    }

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun HttpException.envelopeMessage(): String {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    if (!raw.isNullOrBlank()) {
        val parsed = runCatching {
            errorJson.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull()
        if (!parsed.isNullOrBlank()) return parsed
    }
    return when (code()) {
        401, 403 -> "Sessão expirada. Entre novamente."
        404 -> "Registro não encontrado."
        else -> "A API respondeu com erro ${code()}."
    }
}
