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
            if (context.baseUrl.isBlank()) {
                context.baseUrl = serverManager.ensureServerRunning()
            }

            val plan = ExecutionPlan(
                listOf(HttpCallStep(request, httpExecutor))
            )
            engine.run(plan, context)

            val response = requireNotNull(context.lastResponse) { "No response produced by execution plan" }
            resultPresenter.showResponse(response)
        } catch (t: Throwable) {
            val causeMessage = t.cause?.message
            val message = listOfNotNull(t.message, causeMessage)
                .distinct()
                .joinToString(" | ")
                .ifBlank { "Execution failed" }
            resultPresenter.showError(message)
        }
        return context
    }
}
