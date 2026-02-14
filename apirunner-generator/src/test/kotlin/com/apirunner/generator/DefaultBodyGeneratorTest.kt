package com.apirunner.generator

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultBodyGeneratorTest {
    private val generator = DefaultBodyGenerator()
    private val mapper = ObjectMapper()

    data class Address(val city: String)
    data class User(val name: String, val age: Int, val address: Address)
    enum class Role { ADMIN, USER }

    data class HasEnum(val role: Role)

    @Test
    fun `generates primitive and nested object`() {
        val json = generator.generate(User::class.java.name, GeneratorConfig(maxDepth = 3))
        val root = mapper.readTree(json)

        assertEquals("string", root["name"].asText())
        assertEquals(1, root["age"].asInt())
        assertEquals("string", root["address"]["city"].asText())
    }

    @Test
    fun `generates enum first value`() {
        val json = generator.generate(HasEnum::class.java.name)
        val root = mapper.readTree(json)

        assertEquals("ADMIN", root["role"].asText())
    }

    @Test
    fun `respects depth limit`() {
        val json = generator.generate(User::class.java.name, GeneratorConfig(maxDepth = 0))
        val root = mapper.readTree(json)

        assertTrue(root["address"].isNull)
    }
}
