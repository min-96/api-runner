package com.apirunner.core.http

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.model.ApiRequest
import com.apirunner.core.model.ApiResponse

interface HttpExecutor {
    fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse
}
