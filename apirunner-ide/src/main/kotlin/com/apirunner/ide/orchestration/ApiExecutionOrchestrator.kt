package com.apirunner.ide.orchestration

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.engine.ExecutionEngine
import com.apirunner.core.engine.ExecutionPlan
import com.apirunner.core.engine.HttpCallStep
import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.apirunner.ide.server.ServerManager
import com.apirunner.ide.ui.ResultPresenter

class ApiExecutionOrchestrator(
    private val serverManager: ServerManager,
    private val httpExecutor: HttpExecutor,
    private val resultPresenter: ResultPresenter,
    private val engine: ExecutionEngine = ExecutionEngine()
) {
    fun execute(request: ApiRequest, existingContext: ExecutionContext? = null): ExecutionContext {
        val context = existingContext ?: ExecutionContext()
        try {
            // Refresh on every run so updated Settings port is applied immediately.
            context.baseUrl = serverManager.ensureServerRunning()

            val plan = ExecutionPlan(
                listOf(HttpCallStep(request, httpExecutor))
            )
            engine.run(plan, context)

            val response = requireNotNull(context.lastResponse) { "No response produced by execution plan" }
            val resolvedRequest = context.lastRequest ?: request
            resultPresenter.showResponse(response, resolvedRequest, context.baseUrl)
        } catch (t: Throwable) {
            val causeMessage = t.cause?.message
            val message = listOfNotNull(t.message, causeMessage)
                .distinct()
                .joinToString(" | ")
                .ifBlank { "Execution failed" }
            val resolvedRequest = context.lastRequest ?: request
            resultPresenter.showError(message, resolvedRequest, context.baseUrl)
        }
        return context
    }
}
