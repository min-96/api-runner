package com.apirunner.ide.server

object PortParser {
    private val patterns = listOf(
        Regex("Tomcat started on port\\(s\\):\\s*(\\d+)"),
        Regex("Netty started on port\\(s\\):\\s*(\\d+)"),
        Regex("Started .* on port\\(s\\):\\s*(\\d+)")
    )

    fun parse(logLine: String): Int? {
        for (pattern in patterns) {
            val match = pattern.find(logLine)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        return null
    }
}
