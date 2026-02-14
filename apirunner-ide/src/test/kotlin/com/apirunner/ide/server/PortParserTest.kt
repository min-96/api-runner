package com.apirunner.ide.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortParserTest {
    @Test
    fun `parses tomcat and netty logs`() {
        assertEquals(8080, PortParser.parse("Tomcat started on port(s): 8080 (http) with context path ''"))
        assertEquals(9090, PortParser.parse("Netty started on port(s): 9090"))
    }

    @Test
    fun `returns null when log does not contain port`() {
        assertNull(PortParser.parse("Application started"))
    }
}
