package com.bifrostorbit.careers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/** Decodes Careers Orbit answers into the SDK's models, or your own `@Serializable` types. */
public object CareersJson {
    /** Ignores fields your types do not declare, and uses your defaults for missing or `null` values. */
    public val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    internal val encoder: Json = Json { explicitNulls = false }

    public inline fun <reified T> decode(element: JsonElement): T = decode(serializer<T>(), element)

    @JvmStatic
    public fun <T> decode(serializer: KSerializer<T>, element: JsonElement): T =
        try {
            json.decodeFromJsonElement(serializer, element)
        } catch (error: SerializationException) {
            throw CareersDecodingException(error.message ?: "The answer could not be decoded", error)
        } catch (error: IllegalArgumentException) {
            throw CareersDecodingException(error.message ?: "The answer could not be decoded", error)
        }
}
