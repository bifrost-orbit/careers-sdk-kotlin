package com.bifrostorbit.careers

/** A failed call. [status] is the HTTP status and [code] the API's error code, such as `invalid`. */
public open class CareersOrbitException(
    public val status: Int,
    public val code: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** A missing, wrong, revoked or expired key. */
public class CareersUnauthorizedException(message: String, code: String = "unauthorized") :
    CareersOrbitException(401, code, message)

/** The candidate's portal session is missing or has expired. The session store has been cleared. */
public class CareersSignedOutException(message: String, code: String = "unauthorized") :
    CareersOrbitException(401, code, message)

/** No such job, or it is closed, scheduled or unpublished. */
public class CareersNotFoundException(message: String, code: String = "not_found") :
    CareersOrbitException(404, code, message)

/** Too many requests. [retryAfter] is the number of seconds to wait. */
public class CareersRateLimitedException(message: String, public val retryAfter: Int) :
    CareersOrbitException(429, "rate_limited", message)

/** Careers Orbit failed, could not be reached or did not answer in time, after every retry. */
public class CareersServerException(message: String, status: Int = 503, cause: Throwable? = null) :
    CareersOrbitException(status, "unavailable", message, cause)

/** The answer could not be read into the expected type. */
public class CareersDecodingException(message: String, cause: Throwable? = null) :
    CareersOrbitException(0, "decoding_error", message, cause)
