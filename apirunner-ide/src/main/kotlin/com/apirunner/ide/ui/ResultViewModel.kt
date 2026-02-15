package com.apirunner.ide.ui

data class ResultViewModel(
    val status: Int,
    val durationMs: Long,
    val headers: Map<String, String>,
    val body: String?,
    val requestMethod: String? = null,
    val requestUrl: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val error: String? = null
)
