package com.bifrostorbit.careers

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import kotlin.time.Duration

/**
 * Reads live jobs from the Careers Orbit delivery API and sends applications and talent pool sign-ups.
 *
 * ```
 * val careers = CareersOrbitClient("careers_pk_...") { locale = "de" }
 * val job = careers.findJob("mountain-guide-lofoten")
 * ```
 *
 * Every call is a suspend function. Java callers use [async], which answers with `CompletableFuture`s.
 */
public class CareersOrbitClient private constructor(builder: Builder) {
    /** The delivery API address, such as `https://api.bifrostorbit.com/careers/v1`. */
    public val apiUrl: String

    public val locale: String? = builder.locale

    private val publicKey: String
    private val connection: Connection

    /** The same calls for Java, answering with `CompletableFuture`s. */
    public val async: CareersOrbitAsyncClient by lazy { CareersOrbitAsyncClient(this) }

    init {
        val key = builder.publicKey.trim()
        Connection.checkPublicKey(key)
        publicKey = key
        connection = Connection(builder.baseUrl, builder.timeoutMillis, builder.retries, builder.retryDelays, builder.okHttpClient)
        apiUrl = connection.base
    }

    /** Settings for a [CareersOrbitClient]. Set properties in Kotlin, or chain the methods of the same name in Java. */
    public class Builder(public var publicKey: String) {
        /** The API address. Defaults to production, `https://api.bifrostorbit.com/careers/v1`. */
        public var baseUrl: String = CareersEnvironment.PRODUCTION.baseUrl

        /** Used when a call names no language, such as `de`. Without it, the career site's main language answers. */
        public var locale: String? = null

        /** Per attempt, in milliseconds. 5 seconds by default. */
        public var timeoutMillis: Long = 5_000

        /** Extra attempts at a read after a network error or a server error. 2 by default. Applications are sent once. */
        public var retries: Int = 2

        /** Your own OkHttp client, to share its connection pool, add interceptors or set a proxy. */
        public var okHttpClient: OkHttpClient? = null

        internal var retryDelays: List<Long> = listOf(250, 750, 2_000)

        public fun publicKey(publicKey: String): Builder = apply { this.publicKey = publicKey }
        public fun baseUrl(baseUrl: String): Builder = apply { this.baseUrl = baseUrl }
        public fun environment(environment: CareersEnvironment): Builder = apply { this.baseUrl = environment.baseUrl }
        public fun locale(locale: String?): Builder = apply { this.locale = locale }
        public fun timeoutMillis(millis: Long): Builder = apply { this.timeoutMillis = millis }
        public fun timeout(timeout: Duration): Builder = apply { this.timeoutMillis = timeout.inWholeMilliseconds }
        public fun retries(retries: Int): Builder = apply { this.retries = retries }
        public fun okHttpClient(client: OkHttpClient?): Builder = apply { this.okHttpClient = client }

        public fun build(): CareersOrbitClient = CareersOrbitClient(this)
    }

    public companion object {
        public const val PUBLIC_KEY_PREFIX: String = "careers_pk_"
        public const val VERSION: String = Connection.VERSION

        @JvmStatic
        public fun builder(publicKey: String): Builder = Builder(publicKey)

        public operator fun invoke(publicKey: String, configure: Builder.() -> Unit = {}): CareersOrbitClient =
            Builder(publicKey).apply(configure).build()
    }

    /** Live jobs, newest first, filtered and paged. */
    @JvmSynthetic
    public suspend fun getJobs(query: JobQuery = JobQuery.DEFAULT): CareersJobList {
        val locale = query.locale ?: locale
        val response = read(
            listOf("jobs"),
            listOf(
                "locale" to locale,
                "department" to query.department,
                "location" to query.location,
                "remote" to query.remote,
                "type" to query.type,
                "q" to query.q,
                "page" to query.page,
                "perPage" to query.perPage,
            ),
        )
        val body = Connection.parse(response.body) as? JsonObject ?: throw CareersDecodingException("Careers Orbit answered without data")
        val items = CareersJson.decode(ListSerializer(CareersJobSummary.serializer()), body["data"] as? JsonArray ?: JsonArray(emptyList()))
        val meta = body["meta"] as? JsonObject
        return CareersJobList(
            items = items,
            locale = (meta?.get("locale") as? JsonPrimitive)?.contentOrNull ?: locale ?: "en",
            total = (meta?.get("total") as? JsonPrimitive)?.intOrNull ?: items.size,
            page = (meta?.get("page") as? JsonPrimitive)?.intOrNull ?: 1,
            perPage = (meta?.get("perPage") as? JsonPrimitive)?.intOrNull ?: items.size,
        )
    }

    /** One live job. Throws [CareersNotFoundException] when it is closed or gone. */
    @JvmSynthetic
    public suspend fun getJob(slug: String, locale: String? = null): CareersJob =
        Connection.decode(read(listOf("jobs", slug), listOf("locale" to (locale ?: this.locale))), CareersJob.serializer())

    /** Like [getJob], but answers `null` when the job is closed or gone. */
    @JvmSynthetic
    public suspend fun findJob(slug: String, locale: String? = null): CareersJob? =
        try {
            getJob(slug, locale)
        } catch (_: CareersNotFoundException) {
            null
        }

    /** The career site's name, introduction, logo, colours, languages and current privacy notice. */
    @JvmSynthetic
    public suspend fun getSite(locale: String? = null): CareersSite =
        Connection.decode(read(listOf("site"), listOf("locale" to (locale ?: this.locale))), CareersSite.serializer())

    /** Departments, locations, job types and workplaces that have live jobs, with counts. */
    @JvmSynthetic
    public suspend fun getFilters(locale: String? = null): CareersFilters =
        Connection.decode(read(listOf("filters"), listOf("locale" to (locale ?: this.locale))), CareersFilters.serializer())

    /** Every live job with its address in each language. */
    @JvmSynthetic
    public suspend fun getSitemap(): List<CareersSitemapEntry> =
        Connection.decode(read(listOf("sitemap"), listOf("locale" to locale)), ListSerializer(CareersSitemapEntry.serializer()))

    /** Where applications for a job are sent. */
    public fun applicationUrl(slug: String): String = connection.url(listOf("jobs", slug, "applications")).toString()

    /** Sends an application. A second application to the same job from the same email updates the first. */
    @JvmSynthetic
    public suspend fun apply(
        slug: String,
        application: CareersApplication,
        files: CareersApplicationFiles = CareersApplicationFiles(),
    ): CareersApplicationResult {
        require(application.privacyAccepted) { "An application needs privacyAccepted = true, once the candidate has agreed to the privacy notice" }
        val uploads = listOfNotNull(files.cv?.let { "cv" to it }) + files.questions.map { (id, file) -> "file:$id" to file }
        val body = Connection.multipart(
            mapOf("data" to CareersJson.encoder.encodeToString(CareersApplication.serializer(), application)),
            uploads,
        )
        val request = connection.request("POST", connection.url(listOf("jobs", slug, "applications")), headers(), body)
        return Connection.decode(connection.send(request), CareersApplicationResult.serializer())
    }

    /** Keeps someone's details for future jobs, and with `jobAlerts` emails them when one opens. */
    @JvmSynthetic
    public suspend fun joinTalentPool(signup: CareersTalentPoolSignup) {
        require(signup.privacyAccepted) { "A sign-up needs privacyAccepted = true, once the person has agreed to the privacy notice" }
        val body = Connection.json(CareersJson.encoder.encodeToJsonElement(CareersTalentPoolSignup.serializer(), signup))
        connection.send(connection.request("POST", connection.url(listOf("talent-pool")), headers(), body))
    }

    /** Counts a view of a job or the start of an application, for the reports' channels. Carries nothing about the person, and never throws. */
    @JvmSynthetic
    public suspend fun trackJob(slug: String, kind: TrackKind, source: String? = null, utmSource: String? = null) {
        val body = buildJsonObject {
            put("kind", kind.value)
            if (source != null) put("source", source)
            if (utmSource != null) put("utmSource", utmSource)
        }
        try {
            connection.send(connection.request("POST", connection.url(listOf("jobs", slug, "track")), headers(), Connection.json(body)))
        } catch (_: CareersOrbitException) {
        }
    }

    private fun headers(): Map<String, String> = mapOf("Authorization" to "Bearer $publicKey", "Accept" to "application/json")

    private suspend fun read(segments: List<String>, query: List<Pair<String, Any?>>): RawResponse =
        connection.send(connection.request("GET", connection.url(segments, query), headers()))
}
