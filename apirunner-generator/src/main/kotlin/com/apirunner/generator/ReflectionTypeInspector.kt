package com.apirunner.generator

import com.apirunner.generator.model.TypeField

class ReflectionTypeInspector : TypeInspector {
    override fun isPrimitive(typeName: String): Boolean = typeName in primitiveTypes

    override fun isCollection(typeName: String): Boolean {
        return typeName.startsWith("java.util.List") ||
            typeName.startsWith("kotlin.collections.List") ||
            typeName.startsWith("java.util.Set") ||
            typeName.startsWith("kotlin.collections.Set")
    }

    override fun getCollectionGeneric(typeName: String): String? {
        val start = typeName.indexOf('<')
        val end = typeName.lastIndexOf('>')
        if (start >= 0 && end > start) {
            return typeName.substring(start + 1, end).trim()
        }
        return "java.lang.String"
    }

    override fun getFields(typeName: String): List<TypeField> {
        return runCatching {
            val clazz = classForName(typeName)
            clazz.declaredFields
                .filterNot { it.name.startsWith("$") || java.lang.reflect.Modifier.isStatic(it.modifiers) }
                .map { TypeField(it.name, it.type.name) }
        }.getOrDefault(emptyList())
    }

    override fun isEnum(typeName: String): Boolean {
        return runCatching { classForName(typeName).isEnum }.getOrDefault(false)
    }

    override fun getEnumValues(typeName: String): List<String> {
        return runCatching {
            classForName(typeName).enumConstants?.map { it.toString() } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun classForName(typeName: String): Class<*> {
        return when (typeName) {
            "int" -> Int::class.javaPrimitiveType!!
            "long" -> Long::class.javaPrimitiveType!!
            "double" -> Double::class.javaPrimitiveType!!
            "boolean" -> Boolean::class.javaPrimitiveType!!
            else -> Class.forName(typeName)
        }
    }

    companion object {
        private val primitiveTypes = setOf(
            "java.lang.String",
            "kotlin.String",
            "int",
            "java.lang.Integer",
            "kotlin.Int",
            "long",
            "java.lang.Long",
            "kotlin.Long",
            "boolean",
            "java.lang.Boolean",
            "kotlin.Boolean",
            "double",
            "java.lang.Double",
            "kotlin.Double"
        )
    }
}
