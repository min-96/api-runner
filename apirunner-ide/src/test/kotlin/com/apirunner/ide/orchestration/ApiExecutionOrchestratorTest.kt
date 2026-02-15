package com.apirunner.ide.orchestration

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.apirunner.core.model.ApiResponse
import com.apirunner.ide.server.ServerManager
import com.apirunner.ide.server.ServerProcess
import com.apirunner.ide.ui.InMemoryResultSink
import com.apirunner.ide.ui.ResultPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.util.concurrent.atomic.AtomicInteger

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

        val toolWindow = InMemoryResultSink()
        val presenter = ResultPresenter(toolWindow)
        val orchestrator = ApiExecutionOrchestrator(serverManager, executor, presenter)

        val context = orchestrator.execute(ApiRequest(method = "GET", pathTemplate = "/health"))

        assertEquals("http://localhost:8080", context.baseUrl)
        assertEquals(200, context.lastResponse?.status)
        assertNotNull(toolWindow.latestResult())
        assertEquals(200, toolWindow.latestResult()?.status)
        assertEquals("GET", toolWindow.latestResult()?.requestMethod)
        assertEquals("http://localhost:8080/health", toolWindow.latestResult()?.requestUrl)
    }

    @Test
    fun `refreshes base url when port changes in existing context`() {
        val port = AtomicInteger(8080)
        val process = object : ServerProcess {
            override fun isRunning(): Boolean = true
            override fun runAndWaitForPort(timeoutMs: Long): Int? = null
        }
        val serverManager = ServerManager(process) { port.get() }

        val executor = object : HttpExecutor {
            override fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse {
                return ApiResponse(status = 200, body = "{\"ok\":true}", durationMs = 5)
            }
        }

        val sink = InMemoryResultSink()
        val presenter = ResultPresenter(sink)
        val orchestrator = ApiExecutionOrchestrator(serverManager, executor, presenter)
        val context = ExecutionContext()

        orchestrator.execute(ApiRequest(method = "GET", pathTemplate = "/health"), context)
        assertEquals("http://localhost:8080", context.baseUrl)
        assertEquals("http://localhost:8080/health", sink.latestResult()?.requestUrl)

        port.set(9090)
        orchestrator.execute(ApiRequest(method = "GET", pathTemplate = "/health"), context)
        assertEquals("http://localhost:9090", context.baseUrl)
        assertEquals("http://localhost:9090/health", sink.latestResult()?.requestUrl)
    }
}
