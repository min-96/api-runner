package com.apirunner.core.error

class CoreException(val error: CoreError) : RuntimeException(error.toString())
