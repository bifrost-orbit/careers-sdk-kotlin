package com.bifrostorbit.careers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

/** The calls of [CareersPortalClient] for Java, each answering with a `CompletableFuture`. Get it from [CareersPortalClient.async]. */
public class CareersPortalAsyncClient internal constructor(private val client: CareersPortalClient) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun <T> run(block: suspend CareersPortalClient.() -> T): CompletableFuture<T> = scope.future { client.block() }

    public fun requestCode(email: String): CompletableFuture<Unit> = run { requestCode(email) }

    public fun signIn(email: String, code: String): CompletableFuture<String?> = run { signIn(email, code) }

    public fun signOut(): CompletableFuture<Unit> = run { signOut() }

    public fun me(): CompletableFuture<CareersPortal?> = run { me() }

    @JvmOverloads
    public fun withdraw(applicationId: String, reason: String? = null): CompletableFuture<CareersPortal> = run { withdraw(applicationId, reason) }

    public fun addDocument(applicationId: String, file: CareersFile): CompletableFuture<CareersPortal> = run { addDocument(applicationId, file) }

    public fun exportData(): CompletableFuture<ByteArray> = run { exportData() }

    public fun askToErase(): CompletableFuture<CareersPortal> = run { askToErase() }

    public fun askToCorrect(detail: String): CompletableFuture<CareersPortal> = run { askToCorrect(detail) }

    public fun openOffers(): CompletableFuture<CareersPortal> = run { openOffers() }

    public fun acceptOffer(offerId: String, acceptance: CareersOfferAcceptance): CompletableFuture<CareersPortal> = run { acceptOffer(offerId, acceptance) }

    @JvmOverloads
    public fun declineOffer(offerId: String, reason: String? = null): CompletableFuture<CareersPortal> = run { declineOffer(offerId, reason) }

    public fun offerPdf(offerId: String): CompletableFuture<ByteArray> = run { offerPdf(offerId) }

    public fun stopTexts(channel: String): CompletableFuture<CareersPortal> = run { stopTexts(channel) }

    public fun leaveTalentPool(): CompletableFuture<CareersPortal> = run { leaveTalentPool() }

    public fun stayInTalentPool(): CompletableFuture<CareersPortal> = run { stayInTalentPool() }
}
