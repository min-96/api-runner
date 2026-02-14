package com.apirunner.ide.action

import com.apirunner.core.model.ApiRequest
import com.apirunner.ide.orchestration.ApiExecutionOrchestrator
import com.apirunner.ide.psi.ControllerMethodInfo
import com.apirunner.ide.psi.EndpointExtractor
import com.apirunner.generator.BodyGenerator
import com.apirunner.generator.DefaultParamGenerator

class RunControllerAction(
    private val endpointExtractor: EndpointExtractor,
    private val bodyGenerator: BodyGenerator,
    private val orchestrator: ApiExecutionOrchestrator
) {
    fun run(method: ControllerMethodInfo) {
        val descriptor = endpointExtractor.extract(method)

        val body = descriptor.requestBodyType?.let { bodyGenerator.generate(it) }
        val headers = mutableMapOf<String, String>()
        if (body != null) {
            headers["Content-Type"] = "application/json"
        }

        val request = ApiRequest(
            method = descriptor.httpMethod,
            pathTemplate = descriptor.pathTemplate,
            pathVariables = DefaultParamGenerator.generatePathVars(descriptor.pathVariableNames).toMutableMap(),
            queryParams = DefaultParamGenerator.generateQueryParams(descriptor.queryParamNames).toMutableMap(),
            headers = headers,
            body = body
        )

        orchestrator.execute(request)
    }
}
