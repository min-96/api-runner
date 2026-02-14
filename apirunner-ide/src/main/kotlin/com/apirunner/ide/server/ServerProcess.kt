package com.apirunner.ide.server

interface ServerProcess {
    fun isRunning(): Boolean
    fun runAndWaitForPort(timeoutMs: Long = 30_000): Int?
}
