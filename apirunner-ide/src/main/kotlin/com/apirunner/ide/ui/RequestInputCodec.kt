package com.apirunner.ide.ui

object RequestInputCodec {
    fun formatQuery(query: Map<String, String>): String {
        return query.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    fun formatHeaders(headers: Map<String, String>): String {
        return headers.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    fun parseQuery(input: String): Map<String, String> {
        return parseDelimitedKeyValue(input = input, entrySeparator = '&', keyValueSeparator = '=')
    }

    fun parseHeaders(input: String): Map<String, String> {
        return parseDelimitedKeyValue(input = input, entrySeparator = ';', keyValueSeparator = ':')
    }

    private fun parseDelimitedKeyValue(
        input: String,
        entrySeparator: Char,
        keyValueSeparator: Char
    ): Map<String, String> {
        if (input.isBlank()) {
            return emptyMap()
        }
        return input.split(entrySeparator)
            .mapNotNull { token ->
                val idx = token.indexOf(keyValueSeparator)
                if (idx <= 0) {
                    null
                } else {
                    token.substring(0, idx).trim() to token.substring(idx + 1).trim()
                }
            }
            .toMap()
    }
}
