package com.apirunner.generator

object DefaultParamGenerator {
    fun generatePathVars(names: List<String>): Map<String, String> = names.associateWith { "1" }

    fun generateQueryParams(names: List<String>): Map<String, String> = names.associateWith { "1" }
}
