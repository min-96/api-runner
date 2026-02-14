package com.apirunner.ide.ui

import com.apirunner.core.model.ApiRequest
import java.nio.file.Paths

class RequestDialog {
    fun validate(request: ApiRequest): List<String> {
        val errors = mutableListOf<String>()
        request.pathVariables.forEach { (key, value) ->
            if (value.isBlank()) {
                errors += "Path variable '$key' is required"
            }
        }

        val isMultipart = request.headers.any { (k, v) ->
            k.equals("Content-Type", ignoreCase = true) &&
                v.contains("multipart/form-data", ignoreCase = true)
        }

        val body = request.body
        if (!body.isNullOrBlank()) {
            if (isMultipart) {
                body.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        if (!line.contains("=")) {
                            errors += "Invalid multipart line: '$line' (expected key=value)"
                            return@forEach
                        }
                        val value = line.substringAfter('=').trim()
                        if (value.startsWith("@")) {
                            val rawPath = value.removePrefix("@").trim()
                            val path = runCatching { Paths.get(rawPath) }.getOrNull()
                            if (path == null) {
                                errors += "Invalid file path: '$rawPath'"
                            } else if (path.isAbsolute) {
                                errors += "Use project-relative file path only: '$rawPath'"
                            } else {
                                val normalized = path.normalize().toString().replace('\\', '/')
                                if (normalized.startsWith("../") || normalized == "..") {
                                    errors += "File path cannot escape project root: '$rawPath'"
                                }
                            }
                        }
                    }
            } else {
                val trim = body.trim()
                val looksJson = (trim.startsWith("{") && trim.endsWith("}")) || (trim.startsWith("[") && trim.endsWith("]"))
                if (!looksJson) {
                    errors += "Invalid JSON format"
                }
            }
        }

        return errors
    }
}
