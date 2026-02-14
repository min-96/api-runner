package com.apirunner.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtExtractorTest {
    @Test
    fun `extracts known token keys`() {
        assertEquals("a", JwtExtractor.extract("{\"accessToken\":\"a\"}"))
        assertEquals("b", JwtExtractor.extract("{\"token\":\"b\"}"))
        assertEquals("c", JwtExtractor.extract("{\"jwt\":\"c\"}"))
    }

    @Test
    fun `returns null when token missing`() {
        assertNull(JwtExtractor.extract("{\"id\":1}"))
    }
}
