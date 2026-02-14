package com.apirunner.core.error

sealed class CoreError {
    data class TemplateResolutionFailed(val reason: String) : CoreError()
    data class StepFailed(val stepName: String, val reason: String) : CoreError()
}
