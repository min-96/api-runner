package com.apirunner.ide.ui

data class ResultViewModel(
    val status: Int,
    val durationMs: Long,
    val headers: Map<String, String>,
    val body: String?,
    val error: String? = null
)
