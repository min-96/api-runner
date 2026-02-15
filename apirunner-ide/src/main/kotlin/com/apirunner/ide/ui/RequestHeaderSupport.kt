package com.apirunner.ide.ui

object RequestHeaderSupport {
    fun isMultipart(headers: Map<String, String>): Boolean {
        return headers.any { (k, v) ->
            k.equals("Content-Type", ignoreCase = true) &&
                v.contains("multipart/form-data", ignoreCase = true)
        }
    }
}
