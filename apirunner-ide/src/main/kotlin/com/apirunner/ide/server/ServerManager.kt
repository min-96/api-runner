package com.apirunner.ide.server

class ServerManager(
    private val process: ServerProcess,
    private val fallbackPortProvider: () -> Int?
) {
    fun ensureServerRunning(): String {
        val port = process.runAndWaitForPort(timeoutMs = 2_000) ?: fallbackPortProvider()
            ?: throw IllegalStateException("Could not detect server port")

        return "http://localhost:$port"
    }
}
