package com.apirunner.generator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class DefaultBodyGenerator(
    private val typeInspector: TypeInspector = ReflectionTypeInspector(),
    private val defaultValueStrategy: DefaultValueStrategy = SimpleDefaultValueStrategy()
) : BodyGenerator {

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    override fun generate(typeName: String, config: GeneratorConfig): String {
        val node = generateNode(typeName, 0, config)
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
    }

    private fun generateNode(typeName: String, depth: Int, config: GeneratorConfig): Any? {
        if (depth > config.maxDepth) {
            return null
        }

        if (typeInspector.isPrimitive(typeName)) {
            return defaultValueStrategy.defaultValue(typeName)
        }

        if (typeInspector.isEnum(typeName)) {
            return typeInspector.getEnumValues(typeName).firstOrNull()
        }

        if (typeInspector.isCollection(typeName)) {
            val generic = typeInspector.getCollectionGeneric(typeName) ?: "java.lang.String"
            return List(config.collectionSize) {
                generateNode(generic, depth + 1, config)
            }
        }

        val fields = typeInspector.getFields(typeName)
        if (fields.isEmpty()) {
            return null
        }

        val out = linkedMapOf<String, Any?>()
        for (field in fields) {
            out[field.name] = generateNode(field.type, depth + 1, config)
        }
        return out
    }
}
