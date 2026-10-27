package com.bifrostorbit.careers

import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

internal class Connection(
    baseUrl: String,
    private val timeoutMillis: Long,
    retries: Int,
    private val retryDelays: List<Long>,
    okHttpClient: OkHttpClient?,
) {
    val base: String = baseUrl.trim().trimEnd('/')
    private val retries: Int = maxOf(0, retries)
    private val http: OkHttpClient = (okHttpClient ?: sharedClient).newBuilder()
        .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    init {
        require(base.isNotEmpty()) { "A baseUrl is required" }
        base.toHttpUrl()
    }

    fun url(segments: List<String>, query: List<Pair<String, Any?>> = emptyList()): HttpUrl {
        val builder = base.toHttpUrl().newBuilder()
        segments.forEach { builder.addPathSegment(it) }
        for ((name, value) in query) {
            if (value == null || value == "") continue
            builder.addQueryParameter(name, value.toString())
        }
        return builder.build()
    }

    fun request(method: String, url: HttpUrl, headers: Map<String, String>, body: RequestBody? = null): Request =
        Request.Builder().url(url).header("User-Agent", USER_AGENT).apply {
            headers.forEach { (name, value) -> header(name, value) }
            method(method, body ?: if (method == "POST") ByteArray(0).toRequestBody(null) else null)
        }.build()

    suspend fun send(request: Request, onUnauthorized: ((String, String) -> CareersOrbitException)? = null): RawResponse {
        var lastError: CareersOrbitException? = null
        val attempts = if (request.method == "GET") retries else 0
        for (attempt in 0..attempts) {
            if (attempt > 0) delay(retryDelays.getOrElse(attempt - 1) { 2_000 })
            val response = try {
                http.newCall(request).awaitRaw()
            } catch (error: IOException) {
                lastError = CareersServerException(
                    if (error is InterruptedIOException) "Careers Orbit did not answer within ${timeoutMillis}ms" else "Careers Orbit could not be reached",
                    cause = error,
                )
                continue
            }
            if (response.isSuccessful) return response

            val error = (parse(response.body) as? JsonObject)?.get("error") as? JsonObject
            val code = (error?.get("code") as? JsonPrimitive)?.contentOrNull
            val message = (error?.get("message") as? JsonPrimitive)?.contentOrNull ?: "Careers Orbit answered ${response.code}"
            when {
                response.code >= 500 -> {
                    lastError = CareersServerException(message, response.code)
                    continue
                }
                response.code == 401 -> throw onUnauthorized?.invoke(message, code ?: "unauthorized") ?: CareersUnauthorizedException(message, code ?: "unauthorized")
                response.code == 404 -> throw CareersNotFoundException(message, code ?: "not_found")
                response.code == 429 -> throw CareersRateLimitedException(message, retryAfter(response.retryAfter))
                else -> throw CareersOrbitException(response.code, code ?: "error", message)
            }
        }
        throw lastError ?: CareersServerException("Careers Orbit could not be reached")
    }

    companion object {
        const val VERSION: String = "0.1.0"
        const val USER_AGENT: String = "careers-orbit-kotlin/$VERSION"

        private val sharedClient: OkHttpClient by lazy { OkHttpClient() }
        val jsonType = "application/json".toMediaType()

        fun checkPublicKey(key: String) {
            require(key.isNotEmpty()) { "A publicKey is required" }
            require(key.startsWith("careers_pk_")) { "publicKey must be a Careers Orbit public key, starting with careers_pk_" }
        }

        fun json(element: JsonElement): RequestBody = element.toString().toRequestBody(jsonType)

        fun multipart(fields: Map<String, String>, files: List<Pair<String, CareersFile>>): RequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                fields.forEach { (name, value) -> addFormDataPart(name, value) }
                files.forEach { (name, file) -> addFormDataPart(name, file.name, file.bytes.toRequestBody(file.mimeType.toMediaType())) }
            }.build()

        fun data(response: RawResponse): JsonElement =
            (parse(response.body) as? JsonObject)?.get("data") ?: throw CareersDecodingException("Careers Orbit answered ${response.code} without data")

        fun <T> decode(response: RawResponse, serializer: KSerializer<T>): T = CareersJson.decode(serializer, data(response))

        private fun retryAfter(header: String?): Int = header?.trim()?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 } ?: 60

        fun parse(text: String): JsonElement? =
            try {
                Json.parseToJsonElement(text)
            } catch (_: Exception) {
                null
            }
    }
}
