package com.apirunner.core.util

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.model.ApiRequest

object TemplateResolver {
    fun resolve(request: ApiRequest, context: ExecutionContext): ApiRequest {
        val resolvedPath = resolvePathVariables(request.pathTemplate, request.pathVariables)
        val resolvedQuery = request.queryParams.mapValues { (_, v) -> resolveVariables(v, context) }.toMutableMap()
        val resolvedHeaders = request.headers.mapValues { (_, v) -> resolveVariables(v, context) }.toMutableMap()
        val resolvedBody = request.body?.let { resolveVariables(it, context) }

        val mergedHeaders = context.globalHeaders.toMutableMap().apply {
            putAll(resolvedHeaders)
        }

        return request.copy(
            pathTemplate = resolvedPath,
            queryParams = resolvedQuery,
            headers = mergedHeaders,
            body = resolvedBody
        )
    }

    private fun resolvePathVariables(pathTemplate: String, pathVariables: Map<String, String>): String {
        var resolved = pathTemplate
        for ((key, value) in pathVariables) {
            resolved = resolved.replace("{$key}", value)
        }
        return resolved
    }

    private fun resolveVariables(text: String, context: ExecutionContext): String {
        var resolved = text
        for ((key, value) in context.variables) {
            resolved = resolved.replace("{{$key}}", value)
        }
        return resolved
    }
}
