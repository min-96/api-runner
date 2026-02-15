package com.apirunner.core.engine

import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.apirunner.core.model.ApiResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HttpCallStepTest {
    @Test
    fun `stores response in context lastResponse`() {
        val context = ExecutionContext().apply {
            variables["jwt"] = "token-1"
        }

        var capturedRequest: ApiRequest? = null
        val executor = object : HttpExecutor {
            override fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse {
                capturedRequest = request
                return ApiResponse(status = 200, body = "ok")
            }
        }

        val step = HttpCallStep(
            request = ApiRequest(
                method = "GET",
                pathTemplate = "/m/{id}",
                pathVariables = mutableMapOf("id" to "10"),
                headers = mutableMapOf("Authorization" to "Bearer {{jwt}}")
            ),
            httpExecutor = executor
        )

        step.execute(context)

        assertNotNull(capturedRequest)
        assertEquals("/m/10", capturedRequest?.pathTemplate)
        assertEquals("Bearer token-1", capturedRequest?.headers?.get("Authorization"))
        assertEquals("/m/10", context.lastRequest?.pathTemplate)
        assertEquals("Bearer token-1", context.lastRequest?.headers?.get("Authorization"))
        assertEquals(200, context.lastResponse?.status)
    }
}
