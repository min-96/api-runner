package com.apirunner.http

object JwtExtractor {
    private val keys = listOf("accessToken", "token", "jwt")

    fun extract(body: String?): String? {
        if (body.isNullOrBlank()) {
            return null
        }

        for (key in keys) {
            val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
            val match = regex.find(body)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
}
