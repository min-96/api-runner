package com.apirunner.http

import com.apirunner.core.engine.ExecutionContext
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val context: ExecutionContext
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val hasAuthorization = original.headers.names().any { it.equals("Authorization", ignoreCase = true) }

        if (hasAuthorization) {
            return chain.proceed(original)
        }

        val jwt = context.variables["jwt"] ?: return chain.proceed(original)
        val updated = original.newBuilder()
            .addHeader("Authorization", "Bearer $jwt")
            .build()
        return chain.proceed(updated)
    }
}
