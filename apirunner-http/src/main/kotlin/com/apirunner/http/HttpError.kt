package com.apirunner.http

sealed class HttpError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class NetworkFailure(cause: Throwable) : HttpError("Network call failed", cause)
}
