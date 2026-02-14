package com.apirunner.http

import com.apirunner.core.engine.ExecutionContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpClientProvider {
    fun create(
        context: ExecutionContext,
        config: HttpConfig = HttpConfig(),
        cookieJar: SessionCookieJar = SessionCookieJar()
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .followRedirects(config.followRedirects)
            .cookieJar(cookieJar)
            .addInterceptor(AuthInterceptor(context))
            .build()
    }
}
