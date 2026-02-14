package com.apirunner.ide.orchestration

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.apirunner.core.model.ApiResponse
import com.apirunner.ide.server.ServerManager
import com.apirunner.ide.server.ServerProcess
import com.apirunner.ide.ui.ResultPresenter
import com.apirunner.ide.ui.ResultToolWindowFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiExecutionOrchestratorTest {
    @Test
    fun `runs plan and updates toolwindow`() {
        val process = object : ServerProcess {
            override fun isRunning(): Boolean = true
            override fun runAndWaitForPort(timeoutMs: Long): Int? = 8080
        }
        val serverManager = ServerManager(process) { null }

        val executor = object : HttpExecutor {
            override fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse {
                return ApiResponse(status = 200, body = "{\"ok\":true}", durationMs = 12)
            }
        }

        val toolWindow = ResultToolWindowFactory()
        val presenter = ResultPresenter(toolWindow)
        val orchestrator = ApiExecutionOrchestrator(serverManager, executor, presenter)

        val context = orchestrator.execute(ApiRequest(method = "GET", pathTemplate = "/health"))

        assertEquals("http://localhost:8080", context.baseUrl)
        assertEquals(200, context.lastResponse?.status)
        assertNotNull(toolWindow.latestResult())
        assertEquals(200, toolWindow.latestResult()?.status)
    }
}
