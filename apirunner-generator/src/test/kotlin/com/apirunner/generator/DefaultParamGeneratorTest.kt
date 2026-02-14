package com.apirunner.generator

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultParamGeneratorTest {
    @Test
    fun `generates defaults for path and query`() {
        assertEquals(mapOf("id" to "1"), DefaultParamGenerator.generatePathVars(listOf("id")))
        assertEquals(mapOf("page" to "1", "size" to "1"), DefaultParamGenerator.generateQueryParams(listOf("page", "size")))
    }
}
