package com.apirunner.core.model

data class EndpointDescriptor(
    val httpMethod: String,
    val pathTemplate: String,
    val pathVariableNames: List<String>,
    val queryParamNames: List<String>,
    val requestBodyType: String?
)
