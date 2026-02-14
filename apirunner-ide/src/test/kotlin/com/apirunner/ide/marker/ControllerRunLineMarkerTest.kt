package com.apirunner.ide.marker

import com.apirunner.ide.psi.ControllerMethodInfo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerRunLineMarkerTest {
    private val marker = ControllerRunLineMarker()

    @Test
    fun `shows run icon for mapping annotation`() {
        val method = ControllerMethodInfo(
            classAnnotations = emptyList(),
            methodAnnotations = listOf("@GetMapping(\"/health\")"),
            parameters = emptyList()
        )

        assertTrue(marker.shouldShowRunIcon(method))
    }

    @Test
    fun `hides run icon for non endpoint method`() {
        val method = ControllerMethodInfo(
            classAnnotations = emptyList(),
            methodAnnotations = listOf("@Transactional"),
            parameters = emptyList()
        )

        assertFalse(marker.shouldShowRunIcon(method))
    }
}
