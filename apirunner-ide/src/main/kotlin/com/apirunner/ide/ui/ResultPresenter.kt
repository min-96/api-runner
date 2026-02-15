package com.apirunner.ide.ui

import com.apirunner.core.model.ApiResponse
import com.apirunner.core.model.ApiRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ResultPresenter(
    private val toolWindow: ResultSink
) {
    fun showResponse(response: ApiResponse, request: ApiRequest, baseUrl: String) {
        toolWindow.update(
            ResultViewModel(
                status = response.status,
                durationMs = response.durationMs,
                headers = response.headers,
                body = response.body,
                requestMethod = request.method.uppercase(),
                requestUrl = buildRequestUrl(baseUrl, request),
                requestHeaders = request.headers.toMap(),
                requestBody = request.body
            )
        )
    }

    fun showError(message: String, request: ApiRequest? = null, baseUrl: String = "") {
        toolWindow.update(
            ResultViewModel(
                status = 0,
                durationMs = 0,
                headers = emptyMap(),
                body = null,
                requestMethod = request?.method?.uppercase(),
                requestUrl = request?.let { buildRequestUrl(baseUrl, it) },
                requestHeaders = request?.headers?.toMap().orEmpty(),
                requestBody = request?.body,
                error = message
            )
        )
    }

    private fun buildRequestUrl(baseUrl: String, request: ApiRequest): String {
        val baseWithPath = if (baseUrl.isBlank()) {
            request.pathTemplate
        } else {
            baseUrl.trimEnd('/') + "/" + request.pathTemplate.trimStart('/')
        }
        if (request.queryParams.isEmpty()) {
            return baseWithPath
        }

        val query = request.queryParams.entries.joinToString("&") { (key, value) ->
            "${encodeQueryParam(key)}=${encodeQueryParam(value)}"
        }
        return "$baseWithPath?$query"
    }

    private fun encodeQueryParam(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}
