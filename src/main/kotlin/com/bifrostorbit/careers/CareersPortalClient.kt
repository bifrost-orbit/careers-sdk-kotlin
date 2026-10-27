package com.bifrostorbit.careers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import kotlin.time.Duration

/**
 * The candidate portal: sign-in with an emailed code, applications, documents, offers, the talent pool,
 * a data download and requests for a correction or erasure.
 *
 * ```
 * val portal = CareersPortalClient { publicKey = "careers_pk_..."; locale = "de" }
 * portal.requestCode("solveig@northwilds.com")
 * portal.signIn("solveig@northwilds.com", "123456")
 * val me = portal.me()
 * ```
 *
 * Calls that change something answer with the whole portal again, so a screen can show it straight away.
 */
public class CareersPortalClient private constructor(builder: Builder) {
    public val apiUrl: String
    public val locale: String? = builder.locale
    public val session: CareersSessionStore = builder.session

    private val publicKey: String? = builder.publicKey?.trim()?.takeIf { it.isNotEmpty() }
    private val connection: Connection

    /** The same calls for Java, answering with `CompletableFuture`s. */
    public val async: CareersPortalAsyncClient by lazy { CareersPortalAsyncClient(this) }

    init {
        publicKey?.let(Connection::checkPublicKey)
        connection = Connection(builder.baseUrl, builder.timeoutMillis, builder.retries, builder.retryDelays, builder.okHttpClient)
        apiUrl = connection.base
    }

    /** Settings for a [CareersPortalClient]. */
    public class Builder {
        /** A public key (`careers_pk_…`). Leave it out when [baseUrl] is your own server, which adds the key. */
        public var publicKey: String? = null

        public var baseUrl: String = CareersEnvironment.PRODUCTION.baseUrl

        /** The language for emails and stage names, such as `de`. */
        public var locale: String? = null

        /** Where the candidate's session token is kept. In memory by default. */
        public var session: CareersSessionStore = CareersMemorySession()

        public var timeoutMillis: Long = 5_000
        public var retries: Int = 2
        public var okHttpClient: OkHttpClient? = null

        internal var retryDelays: List<Long> = listOf(250, 750, 2_000)

        public fun publicKey(publicKey: String?): Builder = apply { this.publicKey = publicKey }
        public fun baseUrl(baseUrl: String): Builder = apply { this.baseUrl = baseUrl }
        public fun environment(environment: CareersEnvironment): Builder = apply { this.baseUrl = environment.baseUrl }
        public fun locale(locale: String?): Builder = apply { this.locale = locale }
        public fun session(session: CareersSessionStore): Builder = apply { this.session = session }
        public fun timeoutMillis(millis: Long): Builder = apply { this.timeoutMillis = millis }
        public fun timeout(timeout: Duration): Builder = apply { this.timeoutMillis = timeout.inWholeMilliseconds }
        public fun retries(retries: Int): Builder = apply { this.retries = retries }
        public fun okHttpClient(client: OkHttpClient?): Builder = apply { this.okHttpClient = client }

        public fun build(): CareersPortalClient = CareersPortalClient(this)
    }

    public companion object {
        @JvmStatic
        public fun builder(): Builder = Builder()

        public operator fun invoke(configure: Builder.() -> Unit = {}): CareersPortalClient = Builder().apply(configure).build()
    }

    /** Emails a six-digit code if the address has applied. Answers the same either way. */
    @JvmSynthetic
    public suspend fun requestCode(email: String) {
        call("POST", listOf("code"), buildJsonObject {
            put("email", email)
            put("locale", locale)
        })
    }

    /** Checks the code and keeps the session token, which lasts two hours. Answers with the token. */
    @JvmSynthetic
    public suspend fun signIn(email: String, code: String): String? {
        val response = call("POST", listOf("session"), buildJsonObject {
            put("email", email)
            put("code", code)
            put("locale", locale)
        })
        val token = ((Connection.data(response) as? JsonObject)?.get("token") as? JsonPrimitive)?.contentOrNull
        session.set(token)
        return token
    }

    @JvmSynthetic
    public suspend fun signOut() {
        try {
            call("DELETE", listOf("session"))
        } catch (_: CareersOrbitException) {
        }
        session.set(null)
    }

    /** The candidate's applications, offers, talent pool and requests, or `null` when signed out. */
    @JvmSynthetic
    public suspend fun me(): CareersPortal? {
        val response = try {
            call("GET", listOf("me"))
        } catch (_: CareersSignedOutException) {
            return null
        }
        val data = Connection.data(response)
        if (data is JsonNull) {
            session.set(null)
            return null
        }
        return CareersJson.decode(CareersPortal.serializer(), data)
    }

    @JvmSynthetic
    public suspend fun withdraw(applicationId: String, reason: String? = null): CareersPortal =
        view("POST", listOf("applications", applicationId, "withdraw"), buildJsonObject { put("reason", reason) })

    /** Adds a PDF or Word document, up to 10 MB, to an open application. */
    @JvmSynthetic
    public suspend fun addDocument(applicationId: String, file: CareersFile): CareersPortal {
        val body = Connection.multipart(emptyMap(), listOf("file" to file))
        return Connection.decode(send("POST", listOf("applications", applicationId, "documents"), body), CareersPortal.serializer())
    }

    /** The candidate's data as JSON, with links to their files that work for 15 minutes. */
    @JvmSynthetic
    public suspend fun exportData(): ByteArray = call("GET", listOf("export")).bytes

    @JvmSynthetic
    public suspend fun askToErase(): CareersPortal =
        view("POST", listOf("requests"), buildJsonObject {
            put("kind", "erasure")
            put("detail", null as String?)
        })

    @JvmSynthetic
    public suspend fun askToCorrect(detail: String): CareersPortal =
        view("POST", listOf("requests"), buildJsonObject {
            put("kind", "correction")
            put("detail", detail)
        })

    /** Notes that the candidate has seen their open offers. */
    @JvmSynthetic
    public suspend fun openOffers(): CareersPortal = view("POST", listOf("offers"))

    /** Accepts an offer with a simple electronic signature. */
    @JvmSynthetic
    public suspend fun acceptOffer(offerId: String, acceptance: CareersOfferAcceptance): CareersPortal =
        view("POST", listOf("offers", offerId, "answer"), buildJsonObject {
            put("action", "accept")
            put("name", acceptance.name)
            put("consent", acceptance.consent)
            put("documentHash", acceptance.documentHash)
        })

    @JvmSynthetic
    public suspend fun declineOffer(offerId: String, reason: String? = null): CareersPortal =
        view("POST", listOf("offers", offerId, "answer"), buildJsonObject {
            put("action", "decline")
            put("reason", reason)
        })

    /** The offer as a PDF, signed once it has been accepted. */
    @JvmSynthetic
    public suspend fun offerPdf(offerId: String): ByteArray = call("GET", listOf("offers", offerId, "pdf")).bytes

    /** Stops text messages on one channel, `sms` or `whatsapp`. */
    @JvmSynthetic
    public suspend fun stopTexts(channel: String): CareersPortal =
        view("POST", listOf("texting", "stop"), buildJsonObject { put("channel", channel) })

    @JvmSynthetic
    public suspend fun leaveTalentPool(): CareersPortal = view("POST", listOf("talent-pool", "leave"))

    @JvmSynthetic
    public suspend fun stayInTalentPool(): CareersPortal = view("POST", listOf("talent-pool", "stay"))

    private fun headers(): Map<String, String> = buildMap {
        put("Accept", "application/json")
        publicKey?.let { put("Authorization", "Bearer $it") }
        session.get()?.takeIf { it.isNotEmpty() }?.let { put("X-Careers-Session", it) }
    }

    private suspend fun send(method: String, segments: List<String>, body: RequestBody?): RawResponse {
        val url = connection.url(listOf("portal") + segments, listOf("locale" to locale))
        return connection.send(connection.request(method, url, headers(), body)) { message, code ->
            session.set(null)
            CareersSignedOutException(message, code)
        }
    }

    private suspend fun call(method: String, segments: List<String>, body: JsonElement? = null): RawResponse =
        send(method, segments, (body ?: JsonObject(emptyMap()).takeIf { method == "POST" })?.let(Connection::json))

    private suspend fun view(method: String, segments: List<String>, body: JsonElement? = null): CareersPortal =
        Connection.decode(call(method, segments, body), CareersPortal.serializer())
}
