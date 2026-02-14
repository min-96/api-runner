package com.apirunner.core.model

data class ApiResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val durationMs: Long = 0
)
