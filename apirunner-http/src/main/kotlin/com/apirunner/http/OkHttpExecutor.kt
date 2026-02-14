package com.apirunner.http

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.apirunner.core.model.ApiResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLConnection

class OkHttpExecutor(
    private val client: OkHttpClient
) : HttpExecutor {

    override fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse {
        return try {
            val url = buildUrl(context.baseUrl, request)
            val reqBuilder = Request.Builder().url(url)
            val isMultipart = isMultipartRequest(request)
            for ((key, value) in request.headers) {
                if (isMultipart && key.equals("Content-Type", ignoreCase = true)) {
                    continue
                }
                reqBuilder.addHeader(key, value)
            }

            val body = buildRequestBody(request, isMultipart)
            val okRequest = when (request.method.uppercase()) {
                "GET" -> reqBuilder.get().build()
                "POST" -> reqBuilder.post(body).build()
                "PUT" -> reqBuilder.put(body).build()
                "DELETE" -> reqBuilder.delete().build()
                "PATCH" -> reqBuilder.patch(body).build()
                else -> reqBuilder.method(request.method.uppercase(), body).build()
            }

            val start = System.currentTimeMillis()
            client.newCall(okRequest).execute().use { response ->
                val bodyString = response.body?.string()
                JwtExtractor.extract(bodyString)?.let { context.variables["jwt"] = it }

                ApiResponse(
                    status = response.code,
                    headers = response.headers.toMultimap().mapValues { (_, values) -> values.joinToString(",") },
                    body = bodyString,
                    durationMs = System.currentTimeMillis() - start
                )
            }
        } catch (t: Throwable) {
            throw HttpError.NetworkFailure(t)
        }
    }

    private fun isMultipartRequest(request: ApiRequest): Boolean {
        return request.headers.entries.any { (key, value) ->
            key.equals("Content-Type", ignoreCase = true) &&
                value.contains("multipart/form-data", ignoreCase = true)
        }
    }

    private fun buildRequestBody(request: ApiRequest, isMultipart: Boolean): RequestBody {
        if (!isMultipart) {
            return request.body.orEmpty().toRequestBody("application/json".toMediaType())
        }

        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        val lines = request.body.orEmpty().lineSequence().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            val idx = line.indexOf('=')
            if (idx <= 0) {
                continue
            }
            val name = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            if (name.isBlank()) {
                continue
            }

            if (value.startsWith("@")) {
                val path = value.removePrefix("@").trim()
                val file = File(path)
                if (!file.exists() || !file.isFile) {
                    throw IllegalArgumentException("Multipart file not found: $path")
                }
                val contentType = URLConnection.guessContentTypeFromName(file.name)
                    ?.toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaType()
                builder.addFormDataPart(name, file.name, file.asRequestBody(contentType))
            } else if (looksLikeJson(value)) {
                builder.addFormDataPart(name, null, value.toRequestBody("application/json".toMediaType()))
            } else {
                builder.addFormDataPart(name, value)
            }
        }
        return builder.build()
    }

    private fun looksLikeJson(value: String): Boolean {
        val t = value.trim()
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))
    }

    private fun buildUrl(baseUrl: String, request: ApiRequest): String {
        val full = (baseUrl.trimEnd('/') + "/" + request.pathTemplate.trimStart('/')).toHttpUrlOrNull()
            ?: error("Invalid URL base=$baseUrl path=${request.pathTemplate}")

        val builder = full.newBuilder()
        request.queryParams.forEach { (k, v) ->
            builder.addQueryParameter(k, v)
        }
        return builder.build().toString()
    }
}
