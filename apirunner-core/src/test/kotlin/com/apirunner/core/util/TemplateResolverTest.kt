package com.apirunner.core.util

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.model.ApiRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateResolverTest {
    @Test
    fun `resolves path variables and context variables and merges headers`() {
        val context = ExecutionContext().apply {
            variables["jwt"] = "abc"
            globalHeaders["X-Trace"] = "trace-1"
            globalHeaders["Authorization"] = "Bearer old"
        }

        val request = ApiRequest(
            method = "GET",
            pathTemplate = "/users/{id}",
            pathVariables = mutableMapOf("id" to "42"),
            queryParams = mutableMapOf("token" to "{{jwt}}"),
            headers = mutableMapOf("Authorization" to "Bearer {{jwt}}"),
            body = "{\"jwt\":\"{{jwt}}\"}"
        )

        val resolved = TemplateResolver.resolve(request, context)

        assertEquals("/users/42", resolved.pathTemplate)
        assertEquals("abc", resolved.queryParams["token"])
        assertEquals("Bearer abc", resolved.headers["Authorization"])
        assertEquals("trace-1", resolved.headers["X-Trace"])
        assertEquals("{\"jwt\":\"abc\"}", resolved.body)
    }
}
