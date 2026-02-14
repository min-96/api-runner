package com.apirunner.ide.psi

import kotlin.test.Test
import kotlin.test.assertEquals

class EndpointExtractorTest {
    private val extractor = EndpointExtractor()

    @Test
    fun `extracts endpoint descriptor from annotation models`() {
        val method = ControllerMethodInfo(
            classAnnotations = listOf("@RequestMapping(\"/api\")"),
            methodAnnotations = listOf("@PostMapping(\"/users/{id}\")"),
            parameters = listOf(
                MethodParameter("id", listOf("@PathVariable"), "java.lang.Long"),
                MethodParameter("version", listOf("@RequestParam"), "int"),
                MethodParameter("body", listOf("@RequestBody"), "com.example.UpdateUserRequest")
            )
        )

        val descriptor = extractor.extract(method)

        assertEquals("POST", descriptor.httpMethod)
        assertEquals("/api/users/{id}", descriptor.pathTemplate)
        assertEquals(listOf("id"), descriptor.pathVariableNames)
        assertEquals(listOf("version"), descriptor.queryParamNames)
        assertEquals("com.example.UpdateUserRequest", descriptor.requestBodyType)
    }

    @Test
    fun `extracts class and method paths when mapping uses array syntax`() {
        val method = ControllerMethodInfo(
            classAnnotations = listOf("@RequestMapping(path={\"/api/v1\"})"),
            methodAnnotations = listOf("@GetMapping(value=[\"/users\"])"),
            parameters = emptyList()
        )

        val descriptor = extractor.extract(method)

        assertEquals("GET", descriptor.httpMethod)
        assertEquals("/api/v1/users", descriptor.pathTemplate)
    }

    @Test
    fun `normalizes duplicated slashes when joining paths`() {
        val method = ControllerMethodInfo(
            classAnnotations = listOf("@RequestMapping(\"/api/\")"),
            methodAnnotations = listOf("@GetMapping(\"/users\")"),
            parameters = emptyList()
        )

        val descriptor = extractor.extract(method)

        assertEquals("/api/users", descriptor.pathTemplate)
    }

    @Test
    fun `defaults to POST when request mapping has request body but no explicit method`() {
        val method = ControllerMethodInfo(
            classAnnotations = listOf("@RequestMapping(\"/api\")"),
            methodAnnotations = listOf("@RequestMapping(\"/ai\")"),
            parameters = listOf(
                MethodParameter("input", listOf("@RequestBody"), "com.example.RequestInput")
            )
        )

        val descriptor = extractor.extract(method)

        assertEquals("POST", descriptor.httpMethod)
        assertEquals("/api/ai", descriptor.pathTemplate)
    }
}
