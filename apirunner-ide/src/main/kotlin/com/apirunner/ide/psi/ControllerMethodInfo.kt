package com.apirunner.ide.psi

data class ControllerMethodInfo(
    val classAnnotations: List<String>,
    val methodAnnotations: List<String>,
    val parameters: List<MethodParameter>
)

data class MethodParameter(
    val name: String,
    val annotations: List<String>,
    val typeName: String
)
