package com.apirunner.core.model

data class ApiRequest(
    val method: String,
    val pathTemplate: String,
    val pathVariables: MutableMap<String, String> = mutableMapOf(),
    val queryParams: MutableMap<String, String> = mutableMapOf(),
    val headers: MutableMap<String, String> = mutableMapOf(),
    var body: String? = null
)
