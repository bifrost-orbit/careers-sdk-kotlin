package com.bifrostorbit.careers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

/**
 * The calls of [CareersOrbitClient] for Java, each answering with a `CompletableFuture`. Call `join()` on it to wait
 * for the answer. Cancelling a future cancels its request. Get it from [CareersOrbitClient.async].
 */
public class CareersOrbitAsyncClient internal constructor(private val client: CareersOrbitClient) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun <T> run(block: suspend CareersOrbitClient.() -> T): CompletableFuture<T> = scope.future { client.block() }

    @JvmOverloads
    public fun getJobs(query: JobQuery = JobQuery.DEFAULT): CompletableFuture<CareersJobList> = run { getJobs(query) }

    @JvmOverloads
    public fun getJob(slug: String, locale: String? = null): CompletableFuture<CareersJob> = run { getJob(slug, locale) }

    @JvmOverloads
    public fun findJob(slug: String, locale: String? = null): CompletableFuture<CareersJob?> = run { findJob(slug, locale) }

    @JvmOverloads
    public fun getSite(locale: String? = null): CompletableFuture<CareersSite> = run { getSite(locale) }

    @JvmOverloads
    public fun getFilters(locale: String? = null): CompletableFuture<CareersFilters> = run { getFilters(locale) }

    public fun getSitemap(): CompletableFuture<List<CareersSitemapEntry>> = run { getSitemap() }

    @JvmOverloads
    public fun apply(
        slug: String,
        application: CareersApplication,
        files: CareersApplicationFiles = CareersApplicationFiles(),
    ): CompletableFuture<CareersApplicationResult> = run { apply(slug, application, files) }

    public fun joinTalentPool(signup: CareersTalentPoolSignup): CompletableFuture<Unit> = run { joinTalentPool(signup) }

    @JvmOverloads
    public fun trackJob(slug: String, kind: TrackKind, source: String? = null, utmSource: String? = null): CompletableFuture<Unit> =
        run { trackJob(slug, kind, source, utmSource) }
}
