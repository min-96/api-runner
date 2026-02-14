package com.apirunner.core.engine

class ExecutionEngine {
    fun run(plan: ExecutionPlan, context: ExecutionContext) {
        for (step in plan.steps) {
            step.execute(context)
        }
    }
}
