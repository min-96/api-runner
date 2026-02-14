package com.apirunner.core.engine

import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.apirunner.core.util.TemplateResolver

class HttpCallStep(
    private val request: ApiRequest,
    private val httpExecutor: HttpExecutor
) : Step {
    override fun execute(context: ExecutionContext) {
        val resolvedRequest = TemplateResolver.resolve(request, context)
        val response = httpExecutor.execute(resolvedRequest, context)
        context.lastResponse = response
    }
}
