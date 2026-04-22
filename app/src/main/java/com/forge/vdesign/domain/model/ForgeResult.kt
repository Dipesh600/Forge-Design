package com.forge.vdesign.domain.model

/**
 * Sealed result wrapper used throughout the domain and data layers.
 * Replaces bare exceptions crossing architectural boundaries.
 */
sealed class ForgeResult<out T> {
    data class Success<T>(val data: T) : ForgeResult<T>()
    data class Error(val exception: ForgeException) : ForgeResult<Nothing>()
    data object Loading : ForgeResult<Nothing>()
}

/**
 * Typed exception hierarchy — no raw Throwables crossing the network boundary.
 */
sealed class ForgeException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /** Network is unavailable or request timed out. */
    class NetworkException(
        message: String = "No internet connection.",
        cause: Throwable? = null,
    ) : ForgeException(message, cause)

    /** Firebase Auth session has expired or user is not signed in. */
    class AuthException(
        message: String = "Session expired. Please sign in again.",
        cause: Throwable? = null,
    ) : ForgeException(message, cause)

    /** MiniMax API returned a non-2xx response. */
    class ApiException(
        val code: Int,
        message: String,
        cause: Throwable? = null,
    ) : ForgeException(message, cause)

    /** MiniMax API key usage limit exceeded. */
    class RateLimitException(
        message: String = "AI service is busy. Please try again in a moment.",
    ) : ForgeException(message)

    /** Stitch MCP returned invalid or unparseable XML. */
    class XmlParseException(
        message: String = "Generated layout could not be parsed.",
        cause: Throwable? = null,
    ) : ForgeException(message, cause)

    /** Generic catch-all for unexpected errors. */
    class UnknownException(
        message: String = "Something went wrong.",
        cause: Throwable? = null,
    ) : ForgeException(message, cause)
}

/** Convenience extension: map a Success value. */
inline fun <T, R> ForgeResult<T>.mapSuccess(transform: (T) -> R): ForgeResult<R> = when (this) {
    is ForgeResult.Success -> ForgeResult.Success(transform(data))
    is ForgeResult.Error   -> this
    ForgeResult.Loading    -> ForgeResult.Loading
}
