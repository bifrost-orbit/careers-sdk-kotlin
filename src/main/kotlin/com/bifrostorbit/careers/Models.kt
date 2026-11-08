package com.bifrostorbit.careers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A place a job is based. [remote] is `onsite`, `hybrid` or `remote`. */
@Serializable
public data class CareersLocation(
    val id: String,
    val name: String = "",
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val region: String = "",
    val country: String = "",
    val remote: String = "onsite",
)

/** A job's pay, only when the employer chose to show it. [text] is ready to show, such as "42.000 NOK – 48.000 NOK pro Monat". */
@Serializable
public data class CareersSalary(
    val min: Double? = null,
    val max: Double? = null,
    val currency: String = "",
    val period: String = "year",
    val text: String = "",
)

@Serializable
public data class CareersDepartment(val id: String, val name: String = "")

/**
 * A live job in a list.
 *
 * @property url On your website when job addresses are set, otherwise on the hosted career pages.
 * @property employmentType `fullTime`, `partTime`, `contract`, `temporary`, `internship`, `apprenticeship` or `workingStudent`.
 * @property remote `onsite`, `hybrid` or `remote`.
 */
@Serializable
public data class CareersJobSummary(
    val id: String,
    val slug: String,
    val url: String = "",
    val title: String = "",
    val summary: String = "",
    val department: CareersDepartment? = null,
    val locations: List<CareersLocation> = emptyList(),
    val employmentType: String = "fullTime",
    val employmentTypeLabel: String = "",
    val remote: String = "onsite",
    val remoteLabel: String = "",
    val salary: CareersSalary? = null,
    val publishedAt: String = "",
    val closesAt: String? = null,
    val internal: Boolean = false,
)

/** One page of live jobs, in [locale]. */
public data class CareersJobList(
    val items: List<CareersJobSummary>,
    val locale: String,
    val total: Int,
    val page: Int,
    val perPage: Int,
)

@Serializable
public data class CareersQuestionOption(val id: String, val label: String = "")

/** A question the job's form asks. [kind] is `text`, `longText`, `choice`, `multiChoice`, `yesNo`, `file`, `date` or `number`. */
@Serializable
public data class CareersQuestion(
    val id: String,
    val kind: String = "text",
    val label: String = "",
    val help: String = "",
    val required: Boolean = false,
    val options: List<CareersQuestionOption> = emptyList(),
)

/**
 * What a job's apply form asks for.
 *
 * @property fields `required`, `optional` or `off` for each standard field: `phone`, `cv`, `coverLetter`, `linkedin`, `location`, `salaryExpectation` and `startDate`.
 */
@Serializable
public data class CareersForm(
    val fields: Map<String, String> = emptyMap(),
    val questions: List<CareersQuestion> = emptyList(),
) {
    /** Whether a standard field is shown, as required or optional. */
    public fun asks(field: String): Boolean = (fields[field] ?: "off") != "off"
}

/** The privacy notice candidates agree to. Send its [version] with an application. */
@Serializable
public data class CareersPrivacy(val version: Int, val text: String = "")

@Serializable
public data class CareersSeo(val title: String = "", val description: String = "")

/**
 * One live job with its description, form, privacy notice and JobPosting data.
 *
 * @property locales The languages the job has its own title in, for `hreflang` links.
 * @property fallback The fields shown in the main language because they aren't translated.
 * @property description Cleaned HTML.
 * @property jobPosting The schema.org JobPosting, for a `<script type="application/ld+json">`.
 */
@Serializable
public data class CareersJob(
    val id: String,
    val slug: String,
    val url: String = "",
    val title: String = "",
    val summary: String = "",
    val department: CareersDepartment? = null,
    val locations: List<CareersLocation> = emptyList(),
    val employmentType: String = "fullTime",
    val employmentTypeLabel: String = "",
    val remote: String = "onsite",
    val remoteLabel: String = "",
    val salary: CareersSalary? = null,
    val publishedAt: String = "",
    val closesAt: String? = null,
    val internal: Boolean = false,
    val locale: String = "",
    val locales: List<String> = emptyList(),
    val fallback: List<String> = emptyList(),
    val description: String = "",
    val seo: CareersSeo = CareersSeo(),
    val cmsEntryId: String? = null,
    val form: CareersForm = CareersForm(),
    val privacy: CareersPrivacy,
    val jobPosting: JsonObject = JsonObject(emptyMap()),
)

@Serializable
public data class CareersColours(val brand: String? = null, val accent: String? = null)

@Serializable
public data class CareersInsightsSite(val site: String)

/** The text message channels the career site offers candidates. */
@Serializable
public data class CareersTextingChannels(val sms: Boolean = false, val whatsapp: Boolean = false)

/** The career site's name, introduction, logo, colours, languages and current privacy notice. */
@Serializable
public data class CareersSite(
    val name: String,
    val slug: String = "",
    val intro: String = "",
    val logoUrl: String? = null,
    val colours: CareersColours = CareersColours(),
    val locale: String = "",
    val languages: List<String> = emptyList(),
    val defaultLanguage: String = "",
    val contactEmail: String? = null,
    val websiteUrl: String? = null,
    val hostedUrl: String = "",
    val privacy: CareersPrivacy,
    val insights: CareersInsightsSite? = null,
    val texting: CareersTextingChannels? = null,
)

@Serializable
public data class CareersFilterOption(
    val value: String,
    val label: String = "",
    val count: Int = 0,
    val city: String? = null,
    val country: String? = null,
)

/** Departments, locations, job types and workplaces that have live jobs, with counts. Locations carry `city` and `country`. */
@Serializable
public data class CareersFilters(
    val departments: List<CareersFilterOption> = emptyList(),
    val locations: List<CareersFilterOption> = emptyList(),
    val employmentTypes: List<CareersFilterOption> = emptyList(),
    val remote: List<CareersFilterOption> = emptyList(),
)

/** A live job's address, with its address in each language. */
@Serializable
public data class CareersSitemapEntry(
    val url: String,
    val slug: String,
    val updatedAt: String = "",
    val alternates: Map<String, String> = emptyMap(),
)

/** Where an application came from, for the reports' channels. */
@Serializable
public data class CareersUtm @JvmOverloads constructor(
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
)

/**
 * An application, for [CareersOrbitClient.apply]. Send the standard fields the job's form asks for.
 *
 * @property answers Keyed by question id. Build them with [CareersAnswers].
 * @property privacyVersion The `privacy.version` of the job the candidate read.
 * @property privacyAccepted Must be `true`: the candidate agreed to the privacy notice.
 * @property source Where the form is, such as `northwilds.com`. Used when there is no `utm.source`.
 * @property referral The `ref` of a colleague's referral link.
 * @property texting The text message channels the candidate agreed to: `sms` and `whatsapp`.
 */
@Serializable
public data class CareersApplication @JvmOverloads constructor(
    val firstName: String,
    val lastName: String,
    val email: String,
    val privacyVersion: Int,
    val privacyAccepted: Boolean,
    val phone: String? = null,
    val coverLetter: String? = null,
    val linkedin: String? = null,
    val location: String? = null,
    val salaryExpectation: String? = null,
    val startDate: String? = null,
    val answers: Map<String, JsonElement> = emptyMap(),
    val talentPool: Boolean? = null,
    val locale: String? = null,
    val source: String? = null,
    val utm: CareersUtm? = null,
    val referrer: String? = null,
    val referral: String? = null,
    val texting: List<String>? = null,
) {
    public fun withTexting(texting: List<String>?): CareersApplication = copy(texting = texting)
    public fun withAnswer(questionId: String, answer: JsonElement): CareersApplication = copy(answers = answers + (questionId to answer))
    public fun withPhone(phone: String?): CareersApplication = copy(phone = phone)
    public fun withCoverLetter(coverLetter: String?): CareersApplication = copy(coverLetter = coverLetter)
    public fun withTalentPool(talentPool: Boolean?): CareersApplication = copy(talentPool = talentPool)
    public fun withLocale(locale: String?): CareersApplication = copy(locale = locale)
    public fun withSource(source: String?): CareersApplication = copy(source = source)
    public fun withUtm(utm: CareersUtm?): CareersApplication = copy(utm = utm)
    public fun withReferral(referral: String?): CareersApplication = copy(referral = referral)
}

/** Answers to a job's questions. Choices are option ids. */
public object CareersAnswers {
    @JvmStatic
    public fun text(value: String): JsonElement = JsonPrimitive(value)

    @JvmStatic
    public fun choices(vararg optionIds: String): JsonElement = JsonArray(optionIds.map(::JsonPrimitive))

    @JvmStatic
    public fun choices(optionIds: List<String>): JsonElement = JsonArray(optionIds.map(::JsonPrimitive))

    @JvmStatic
    public fun number(value: Number): JsonElement = JsonPrimitive(value)

    /** A yes or no question's answer, sent as `"yes"` or `"no"`. */
    @JvmStatic
    public fun yesNo(value: Boolean): JsonElement = JsonPrimitive(if (value) "yes" else "no")

    @JvmStatic
    public fun none(): JsonElement = JsonNull
}

/** A file to send: a PDF or Word document up to 10 MB. */
public class CareersFile @JvmOverloads constructor(
    public val name: String,
    public val bytes: ByteArray,
    public val mimeType: String = "application/pdf",
)

/** The files that go with an application: the CV, and a file for each file question keyed by question id. */
public data class CareersApplicationFiles @JvmOverloads constructor(
    val cv: CareersFile? = null,
    val questions: Map<String, CareersFile> = emptyMap(),
)

/** The answer to an application. [updated] is `true` when it updated an earlier application from the same email. */
@Serializable
public data class CareersApplicationResult(val applicationId: String, val updated: Boolean = false)

/**
 * A talent pool sign-up, for [CareersOrbitClient.joinTalentPool].
 *
 * @property jobAlerts Email them when a new job opens.
 * @property departmentIds Only tell them about jobs in these departments.
 */
@Serializable
public data class CareersTalentPoolSignup @JvmOverloads constructor(
    val firstName: String,
    val email: String,
    val privacyVersion: Int,
    val privacyAccepted: Boolean,
    val lastName: String? = null,
    val locale: String? = null,
    val jobAlerts: Boolean? = null,
    val departmentIds: List<String>? = null,
)

/** A file on an application. [kind] is `cv` or `document`. */
@Serializable
public data class CareersPortalFile(
    val id: String,
    val name: String = "",
    val size: Long = 0,
    val kind: String = "document",
    val createdAt: String = "",
)

/**
 * An application in the candidate portal.
 *
 * @property stage The stage name candidates see.
 * @property status `active`, `rejected`, `withdrawn` or `hired`.
 */
@Serializable
public data class CareersPortalApplication(
    val id: String,
    val jobTitle: String = "",
    val jobUrl: String? = null,
    val stage: String = "",
    val status: String = "active",
    val appliedAt: String = "",
    val updatedAt: String = "",
    val canWithdraw: Boolean = false,
    val canUpload: Boolean = false,
    val files: List<CareersPortalFile> = emptyList(),
)

/**
 * An offer the candidate can read, download, accept or decline.
 *
 * @property body HTML.
 * @property status `open`, `accepted`, `declined` or `expired`.
 * @property documentHash Send it back in [CareersOfferAcceptance] to sign the version the candidate read.
 */
@Serializable
public data class CareersPortalOffer(
    val id: String,
    val applicationId: String = "",
    val jobTitle: String = "",
    val title: String = "",
    val body: String = "",
    val status: String = "open",
    val sentAt: String? = null,
    val expiresAt: String? = null,
    val documentHash: String? = null,
    val signedAs: String? = null,
    val answeredAt: String? = null,
)

/** A simple electronic signature for [CareersPortalClient.acceptOffer]: the name the candidate typed, their consent and the offer's `documentHash`. */
public data class CareersOfferAcceptance(val name: String, val consent: Boolean, val documentHash: String)

@Serializable
public data class CareersPortalCandidate(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val locale: String = "",
)

@Serializable
public data class CareersPortalSite(val name: String = "", val privacyVersion: Int = 0, val contactEmail: String? = null)

@Serializable
public data class CareersPortalTalentPool(val member: Boolean = false, val until: String? = null)

/** A data request. [kind] is `access`, `erasure` or `correction`; [status] is `open`, `done` or `declined`. */
@Serializable
public data class CareersPortalRequest(
    val id: String,
    val kind: String = "",
    val status: String = "open",
    val receivedAt: String = "",
    val dueAt: String = "",
)

/** Everything a signed-in candidate sees in the portal. [texting] lists the text message channels they agreed to, and [erasureAt] is when their data will be erased, once they have been told. */
@Serializable
public data class CareersPortal(
    val candidate: CareersPortalCandidate = CareersPortalCandidate(),
    val site: CareersPortalSite = CareersPortalSite(),
    val applications: List<CareersPortalApplication> = emptyList(),
    val offers: List<CareersPortalOffer> = emptyList(),
    val texting: List<String> = emptyList(),
    val talentPool: CareersPortalTalentPool = CareersPortalTalentPool(),
    val requests: List<CareersPortalRequest> = emptyList(),
    val erasureAt: String? = null,
)
