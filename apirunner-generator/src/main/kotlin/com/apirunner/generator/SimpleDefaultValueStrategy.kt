package com.apirunner.generator

class SimpleDefaultValueStrategy : DefaultValueStrategy {
    override fun defaultValue(typeName: String): Any? {
        return when (typeName) {
            "java.lang.String", "kotlin.String" -> "string"
            "int", "java.lang.Integer", "kotlin.Int" -> 1
            "long", "java.lang.Long", "kotlin.Long" -> 1L
            "boolean", "java.lang.Boolean", "kotlin.Boolean" -> true
            "double", "java.lang.Double", "kotlin.Double" -> 1.0
            else -> null
        }
    }
}
