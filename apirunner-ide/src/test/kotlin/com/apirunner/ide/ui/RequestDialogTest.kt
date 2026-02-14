package com.apirunner.ide.ui

import com.apirunner.core.model.ApiRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestDialogTest {
    private val dialog = RequestDialog()

    @Test
    fun `returns error for blank path variable`() {
        val request = ApiRequest(
            method = "GET",
            pathTemplate = "/users/{id}",
            pathVariables = mutableMapOf("id" to "")
        )

        val errors = dialog.validate(request)
        assertEquals(1, errors.size)
    }

    @Test
    fun `returns error for invalid json body`() {
        val request = ApiRequest(
            method = "POST",
            pathTemplate = "/users",
            body = "not-json"
        )

        val errors = dialog.validate(request)
        assertEquals(1, errors.size)
    }

    @Test
    fun `accepts valid request`() {
        val request = ApiRequest(
            method = "POST",
            pathTemplate = "/users/{id}",
            pathVariables = mutableMapOf("id" to "1"),
            body = "{\"name\":\"x\"}"
        )

        assertTrue(dialog.validate(request).isEmpty())
    }

    @Test
    fun `accepts multipart request body format`() {
        val request = ApiRequest(
            method = "POST",
            pathTemplate = "/upload",
            headers = mutableMapOf("Content-Type" to "multipart/form-data"),
            body = """
                file=@testdata/test.png
                metadata={"userId":1}
            """.trimIndent()
        )

        assertTrue(dialog.validate(request).isEmpty())
    }

    @Test
    fun `rejects absolute path in multipart file`() {
        val request = ApiRequest(
            method = "POST",
            pathTemplate = "/upload",
            headers = mutableMapOf("Content-Type" to "multipart/form-data"),
            body = "file=@/Users/name/Desktop/test.png"
        )

        val errors = dialog.validate(request)
        assertTrue(errors.any { it.contains("project-relative file path") })
    }

    @Test
    fun `rejects parent traversal in multipart file path`() {
        val request = ApiRequest(
            method = "POST",
            pathTemplate = "/upload",
            headers = mutableMapOf("Content-Type" to "multipart/form-data"),
            body = "file=@../../outside.png"
        )

        val errors = dialog.validate(request)
        assertTrue(errors.any { it.contains("cannot escape project root") })
    }
}
