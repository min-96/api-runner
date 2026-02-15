package com.apirunner.core.engine

import com.apirunner.core.model.ApiRequest
import com.apirunner.core.model.ApiResponse

class ExecutionContext {
    val variables: MutableMap<String, String> = mutableMapOf()
    val globalHeaders: MutableMap<String, String> = mutableMapOf()
    var baseUrl: String = ""
    var lastRequest: ApiRequest? = null
    var lastResponse: ApiResponse? = null
}
