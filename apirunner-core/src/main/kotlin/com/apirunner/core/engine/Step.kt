package com.apirunner.core.engine

interface Step {
    fun execute(context: ExecutionContext)
}
