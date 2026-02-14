package com.apirunner.generator

import com.apirunner.generator.model.TypeField

interface TypeInspector {
    fun isPrimitive(typeName: String): Boolean
    fun isCollection(typeName: String): Boolean
    fun getCollectionGeneric(typeName: String): String?
    fun getFields(typeName: String): List<TypeField>
    fun isEnum(typeName: String): Boolean
    fun getEnumValues(typeName: String): List<String>
}
