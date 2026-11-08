# Careers Orbit SDK for Kotlin

The Kotlin SDK, `com.bifrostorbit:careers-sdk`, shows live jobs from the [delivery API](https://docs.bifrostorbit.com/careers/developers/delivery-api/), takes applications with a CV and runs the candidate portal, in Android apps and on JVM servers such as Ktor or Spring Boot. It does what the client in the [JavaScript SDK](https://docs.bifrostorbit.com/careers/developers/careers-sdk/) does, with the same errors. It is open source under the MIT licence.

- Suspend functions for Kotlin coroutines, and `CompletableFuture`s for Java.
- Built on OkHttp and kotlinx.serialization. Bring your own `OkHttpClient` to share its connection pool and interceptors.
- Every answer has a `@Serializable` model.

## Install

It needs Java 11 or later on servers, and Android 5.0 (API 21) or later in apps. The Java futures need Android 7.0 (API 24). Kotlin projects need Kotlin 2.2 or later.

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.bifrostorbit:careers-sdk:0.1.0")
}
```

With Maven:

```xml
<dependency>
  <groupId>com.bifrostorbit</groupId>
  <artifactId>careers-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

An Android app also needs `<uses-permission android:name="android.permission.INTERNET" />` in its manifest.

## Set up

Create a public key under **API keys** in Careers Orbit. It starts with `careers_pk_` and can only read live jobs and send applications, so it may ship in an app.

```kotlin
import com.bifrostorbit.careers.CareersOrbitClient

val careers = CareersOrbitClient("careers_pk_...") {
    locale = "de"
}
```

In Java:

```java
CareersOrbitClient careers = CareersOrbitClient.builder("careers_pk_...")
    .locale("de")
    .build();
```

| Option | What it does |
| --- | --- |
| `publicKey` | A public key (`careers_pk_…`). Required. |
| `baseUrl` | The API address. `https://api.bifrostorbit.com/careers/v1` by default. |
| `environment(…)` | `PRODUCTION` or `STAGING`, a shortcut for `baseUrl`. Staging is `https://api-stage.bifrostorbit.com/careers/v1`. |
| `locale` | Used when a call names no language. Without it, the career site's main language answers. |
| `timeoutMillis` | Per attempt. 5 seconds by default. |
| `retries` | Extra attempts at a read after a network error or a server error. 2 by default. Applications are sent once. |
| `okHttpClient` | Your own OkHttp client, for interceptors, logging or a proxy. |

Create one client and share it. It is safe to use from many coroutines and threads at once.

## Methods

```kotlin
val list = careers.getJobs(JobQuery(remote = "hybrid", q = "guide"))
val job = careers.findJob("mountain-guide-lofoten") ?: return notFound()
val filters = careers.getFilters()
val site = careers.getSite()
```

| Method | Answers with |
| --- | --- |
| `getJobs(JobQuery)` | Live jobs, filtered by `department`, `location`, `remote`, `type` and `q`, with `page` and `perPage` |
| `getJob(slug, locale)` | One job with its description, form, privacy notice and JobPosting data. Throws `CareersNotFoundException` when it is closed or gone. |
| `findJob(slug, locale)` | The same, or `null` |
| `getFilters(locale)` | Departments, locations, job types and workplaces that have live jobs, with counts |
| `getSite(locale)` | The career site's name, introduction, logo, colours, languages and current privacy notice |
| `getSitemap()` | Every live job with its address in each language |
| `apply(slug, application, files)` | Sends an application. A second one to the same job from the same email updates the first. |
| `joinTalentPool(signup)` | Keeps someone's details for future jobs |
| `trackJob(slug, TrackKind.VIEW)` | Counts a view or an apply start for the reports. Never throws. |

## Applications

```kotlin
val result = careers.apply(
    job.slug,
    CareersApplication(
        firstName = "Kari",
        lastName = "Nordmann",
        email = "kari@northwilds.com",
        privacyVersion = job.privacy.version,
        privacyAccepted = true,
        phone = "+47 912 34 567",
        answers = mapOf(
            "q-years" to CareersAnswers.number(6),
            "q-areas" to CareersAnswers.choices("lofoten", "lyngen"),
            "q-first-aid" to CareersAnswers.yesNo(true),
        ),
        source = "northwilds-app",
    ),
    CareersApplicationFiles(cv = CareersFile("kari-nordmann.pdf", cvBytes)),
)
```

Send the standard fields `job.form` asks for (`job.form.asks("phone")`) and an answer for each question, keyed by its id. Choice answers are option ids. `privacyAccepted` must be `true`; the client refuses the application before sending it otherwise. Files are PDF or Word documents up to 10 MB.

## Candidate portal

`CareersPortalClient` lets candidates sign in with an emailed code and follow their applications. Keep the session token somewhere safe, such as EncryptedSharedPreferences, by implementing `CareersSessionStore`; `CareersMemorySession` keeps it in memory.

```kotlin
val portal = CareersPortalClient {
    publicKey = "careers_pk_..."
    locale = "de"
    session = EncryptedSession(context)
}

portal.requestCode("solveig@northwilds.com")
portal.signIn("solveig@northwilds.com", "123456")
val me = portal.me() // null when signed out
me?.offers?.firstOrNull { it.status == "open" }?.let { offer ->
    portal.acceptOffer(offer.id, CareersOfferAcceptance("Solveig Haugen", consent = true, documentHash = offer.documentHash.orEmpty()))
}
```

| Method | What it does |
| --- | --- |
| `requestCode(email)`, `signIn(email, code)`, `signOut()` | Sign-in with a six-digit code. The session lasts two hours. |
| `me()` | Applications, offers, talent pool and requests, or `null` when signed out |
| `withdraw(applicationId, reason)`, `addDocument(applicationId, file)` | Change an open application |
| `openOffers()`, `acceptOffer(offerId, acceptance)`, `declineOffer(offerId, reason)`, `offerPdf(offerId)` | Read, sign or decline an offer, and download it |
| `exportData()`, `askToCorrect(detail)`, `askToErase()` | The candidate's data and data requests |
| `stayInTalentPool()`, `leaveTalentPool()` | Talent pool consent |
| `stopTexts(channel)` | Stops text messages on `sms` or `whatsapp` |

Calls that change something answer with the whole `CareersPortal` again. A `401` throws `CareersSignedOutException` and clears the session store. Leave out `publicKey` when `baseUrl` is your own server, which adds the key.

## Errors

Failed calls throw a `CareersOrbitException`, or one of its kinds. Each has the HTTP `status` and the API's `code`, and a message candidates can read in the language of the request.

| Exception | When |
| --- | --- |
| `CareersNotFoundException` | No such job, or it isn't live |
| `CareersUnauthorizedException` | A missing, wrong, revoked or expired key |
| `CareersSignedOutException` | The candidate's portal session is missing or has expired |
| `CareersRateLimitedException` | Too many requests. `retryAfter` is the number of seconds to wait. |
| `CareersServerException` | Careers Orbit failed, could not be reached or did not answer in time, after every retry |
| `CareersDecodingException` | The answer could not be decoded |
| `CareersOrbitException` | Anything else, such as `422` with the code `invalid` |

Reads are retried twice after network and server errors, with back-off. Applications, sign-ups and portal changes are sent once. Cancelling the coroutine cancels the request.

## Develop

The project builds with the Gradle wrapper, so only a JDK is needed (17 or later to run Gradle; the library itself is compiled for Java 11).

```bash
./gradlew test       # JUnit 5 and MockWebServer
./gradlew build      # compiles, tests and assembles build/libs
```

The tests cover request addresses, query parameters and headers, decoding from fixtures in `src/test/resources/fixtures`, multipart applications, every error, retries and timeouts, the portal session and the Java futures.

Releases go to Maven Central the same way as the CMS Orbit Kotlin SDK: see its README for the one-off setup.
