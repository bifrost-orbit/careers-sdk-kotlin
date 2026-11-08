package com.bifrostorbit.careers

/**
 * Which jobs to list, for [CareersOrbitClient.getJobs]. Ids come from [CareersOrbitClient.getFilters].
 *
 * @property department A department id.
 * @property location A location id.
 * @property remote `onsite`, `hybrid` or `remote`.
 * @property type `fullTime`, `partTime`, `contract`, `temporary`, `internship`, `apprenticeship` or `workingStudent`.
 * @property q Words in the title, summary, department or location.
 * @property perPage Up to 100. 50 when not set.
 * @property locale Overrides the client's language.
 */
public data class JobQuery @JvmOverloads constructor(
    val department: String? = null,
    val location: String? = null,
    val remote: String? = null,
    val type: String? = null,
    val q: String? = null,
    val page: Int? = null,
    val perPage: Int? = null,
    val locale: String? = null,
) {
    public fun withDepartment(department: String?): JobQuery = copy(department = department)
    public fun withLocation(location: String?): JobQuery = copy(location = location)
    public fun withRemote(remote: String?): JobQuery = copy(remote = remote)
    public fun withType(type: String?): JobQuery = copy(type = type)
    public fun withQuery(q: String?): JobQuery = copy(q = q)
    public fun withPage(page: Int?): JobQuery = copy(page = page)
    public fun withPerPage(perPage: Int?): JobQuery = copy(perPage = perPage)
    public fun withLocale(locale: String?): JobQuery = copy(locale = locale)

    public companion object {
        @JvmField
        public val DEFAULT: JobQuery = JobQuery()
    }
}

/** What [CareersOrbitClient.trackJob] counts. */
public enum class TrackKind(public val value: String) {
    VIEW("view"),
    START("start"),
}

/** Where the clients read from when no `baseUrl` is given. */
public enum class CareersEnvironment(public val baseUrl: String) {
    PRODUCTION("https://api.bifrostorbit.com/careers/v1"),
    STAGING("https://api-stage.bifrostorbit.com/careers/v1"),
}

/** Keeps a candidate's portal session token, such as in EncryptedSharedPreferences or a server-side session. */
public interface CareersSessionStore {
    public fun get(): String?
    public fun set(token: String?)
}

/** Keeps the session token in memory, for as long as the store lives. */
public class CareersMemorySession @JvmOverloads constructor(token: String? = null) : CareersSessionStore {
    @Volatile
    private var token: String? = token

    override fun get(): String? = token

    override fun set(token: String?) {
        this.token = token
    }
}
