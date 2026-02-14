package com.apirunner.http

data class HttpConfig(
    val connectTimeoutMs: Long = 3000,
    val readTimeoutMs: Long = 5000,
    val writeTimeoutMs: Long = 5000,
    val followRedirects: Boolean = true,
    val enableLogging: Boolean = false
)
