package com.apirunner.generator

data class GeneratorConfig(
    val maxDepth: Int = 3,
    val collectionSize: Int = 0,
    val useRandomValues: Boolean = false
)
