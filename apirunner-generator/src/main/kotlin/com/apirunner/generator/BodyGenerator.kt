package com.apirunner.generator

interface BodyGenerator {
    fun generate(typeName: String, config: GeneratorConfig = GeneratorConfig()): String
}
