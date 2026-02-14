package com.apirunner.ide.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerManagerTest {
    @Test
    fun `uses detected port when available`() {
        val process = object : ServerProcess {
            override fun isRunning(): Boolean = true
            override fun runAndWaitForPort(timeoutMs: Long): Int? = 8081
        }

        val manager = ServerManager(process) { 9000 }
        assertEquals("http://localhost:8081", manager.ensureServerRunning())
    }

    @Test
    fun `uses fallback port when detection fails`() {
        val process = object : ServerProcess {
            override fun isRunning(): Boolean = false
            override fun runAndWaitForPort(timeoutMs: Long): Int? = null
        }

        val manager = ServerManager(process) { 9090 }
        assertEquals("http://localhost:9090", manager.ensureServerRunning())
    }

    @Test
    fun `throws when both detection and fallback fail`() {
        val process = object : ServerProcess {
            override fun isRunning(): Boolean = false
            override fun runAndWaitForPort(timeoutMs: Long): Int? = null
        }

        val manager = ServerManager(process) { null }
        assertFailsWith<IllegalStateException> { manager.ensureServerRunning() }
    }
}
