package com.apirunner.ide.ui

import com.apirunner.core.model.ApiResponse

class ResultPresenter(
    private val toolWindow: ResultSink
) {
    fun showResponse(response: ApiResponse) {
        toolWindow.update(
            ResultViewModel(
                status = response.status,
                durationMs = response.durationMs,
                headers = response.headers,
                body = response.body
            )
        )
    }

    fun showError(message: String) {
        toolWindow.update(
            ResultViewModel(
                status = 0,
                durationMs = 0,
                headers = emptyMap(),
                body = null,
                error = message
            )
        )
    }
}
