package com.apirunner.ide.util

class ModuleResolver {
    fun resolveModuleName(path: String): String {
        return path.split('/').firstOrNull { it.isNotBlank() } ?: "default"
    }
}
