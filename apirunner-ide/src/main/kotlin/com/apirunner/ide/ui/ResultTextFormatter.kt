package com.apirunner.ide.ui

import com.fasterxml.jackson.databind.ObjectMapper

class ResultTextFormatter(
    private val objectMapper: ObjectMapper = ObjectMapper()
) {
    fun formatRequestPayload(result: ResultViewModel): String {
        val payload = linkedMapOf<String, Any?>(
            "method" to (result.requestMethod ?: "(unknown)"),
            "url" to (result.requestUrl ?: "(unknown)"),
            "headers" to result.requestHeaders
        )
        payload["body"] = parseJsonOrRaw(result.requestBody)
        return runCatching {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload)
        }.getOrElse {
            "{}"
        }
    }

    fun prettyJsonIfPossible(body: String): String {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return body
        }

        return runCatching {
            val node = objectMapper.readTree(trimmed)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
        }.getOrDefault(body)
    }

    fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.take(maxLength) + "\n\n...truncated..."
        } else {
            text
        }
    }

    private fun parseJsonOrRaw(text: String?): Any? {
        val raw = text?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { objectMapper.readTree(raw) }.getOrDefault(raw)
    }
}
