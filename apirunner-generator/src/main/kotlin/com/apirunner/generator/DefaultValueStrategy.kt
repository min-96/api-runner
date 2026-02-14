package com.apirunner.generator

interface DefaultValueStrategy {
    fun defaultValue(typeName: String): Any?
}
