package com.apirunner.http

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.model.ApiRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkHttpExecutorTest {
    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `injects jwt and extracts jwt from response`() {
        server.enqueue(MockResponse().setBody("{\"accessToken\":\"new-token\"}"))

        val context = ExecutionContext().apply {
            baseUrl = server.url("/").toString().trimEnd('/')
            variables["jwt"] = "old-token"
        }

        val client = OkHttpClientProvider.create(context)
        val executor = OkHttpExecutor(client)

        val response = executor.execute(
            ApiRequest(
                method = "GET",
                pathTemplate = "/auth",
                headers = mutableMapOf("X-Test" to "1")
            ),
            context
        )

        val recorded = server.takeRequest()
        assertEquals("Bearer old-token", recorded.getHeader("Authorization"))
        assertEquals(200, response.status)
        assertEquals("new-token", context.variables["jwt"])
        assertTrue(response.durationMs >= 0)
    }

    @Test
    fun `reuses cookies across calls with shared client`() {
        server.enqueue(MockResponse().setHeader("Set-Cookie", "sid=abc; Path=/"))
        server.enqueue(MockResponse().setBody("ok"))

        val context = ExecutionContext().apply {
            baseUrl = server.url("/").toString().trimEnd('/')
        }

        val cookieJar = SessionCookieJar()
        val client = OkHttpClientProvider.create(context, cookieJar = cookieJar)
        val executor = OkHttpExecutor(client)

        executor.execute(ApiRequest(method = "GET", pathTemplate = "/login"), context)
        executor.execute(ApiRequest(method = "GET", pathTemplate = "/me"), context)

        server.takeRequest() // first
        val second = server.takeRequest()
        assertTrue((second.getHeader("Cookie") ?: "").contains("sid=abc"))
    }

    @Test
    fun `does not override explicit authorization header`() {
        server.enqueue(MockResponse().setBody("ok"))

        val context = ExecutionContext().apply {
            baseUrl = server.url("/").toString().trimEnd('/')
            variables["jwt"] = "auto-token"
        }

        val client = OkHttpClientProvider.create(context)
        val executor = OkHttpExecutor(client)

        executor.execute(
            ApiRequest(
                method = "GET",
                pathTemplate = "/x",
                headers = mutableMapOf("Authorization" to "Bearer manual")
            ),
            context
        )

        val recorded = server.takeRequest()
        assertEquals("Bearer manual", recorded.getHeader("Authorization"))
    }

    @Test
    fun `sends multipart form with file and json metadata`() {
        server.enqueue(MockResponse().setBody("ok"))

        val tempFile = Files.createTempFile("apirunner-", ".png").toFile().apply {
            writeText("dummy-image")
            deleteOnExit()
        }

        val context = ExecutionContext().apply {
            baseUrl = server.url("/").toString().trimEnd('/')
        }
        val client = OkHttpClientProvider.create(context)
        val executor = OkHttpExecutor(client)

        executor.execute(
            ApiRequest(
                method = "POST",
                pathTemplate = "/upload",
                headers = mutableMapOf("Content-Type" to "multipart/form-data"),
                body = """
                    file=@${tempFile.absolutePath}
                    metadata={"userId":1,"name":"test"}
                """.trimIndent()
            ),
            context
        )

        val recorded = server.takeRequest()
        val contentType = recorded.getHeader("Content-Type").orEmpty()
        val bodyText = recorded.body.readUtf8()

        assertTrue(contentType.startsWith("multipart/form-data;"))
        assertTrue(bodyText.contains("name=\"file\""))
        assertTrue(bodyText.contains("filename=\"${tempFile.name}\""))
        assertTrue(bodyText.contains("name=\"metadata\""))
        assertTrue(bodyText.contains("{\"userId\":1,\"name\":\"test\"}"))
    }
}
