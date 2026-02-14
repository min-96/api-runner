package com.apirunner.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutionEngineTest {
    @Test
    fun `runs steps in sequence and shares context`() {
        val context = ExecutionContext()
        val order = mutableListOf<String>()

        val first = object : Step {
            override fun execute(context: ExecutionContext) {
                order += "first"
                context.variables["k"] = "v"
            }
        }
        val second = object : Step {
            override fun execute(context: ExecutionContext) {
                order += context.variables.getValue("k")
            }
        }

        ExecutionEngine().run(ExecutionPlan(listOf(first, second)), context)

        assertEquals(listOf("first", "v"), order)
    }
}
