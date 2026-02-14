package com.apirunner.ide.runtime

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.model.ApiRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.apirunner.http.SessionCookieJar
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.security.MessageDigest

@Service(Service.Level.PROJECT)
class ApiRunnerProjectService(private val project: Project) {
    val context: ExecutionContext = ExecutionContext()
    val cookieJar: SessionCookieJar = SessionCookieJar()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private val props: PropertiesComponent
        get() = PropertiesComponent.getInstance(project)

    fun getSelectedAppClass(): String? = props.getValue(KEY_APP_CLASS)

    fun setSelectedAppClass(className: String) {
        props.setValue(KEY_APP_CLASS, className)
    }

    fun getLastPort(): Int? = props.getValue(KEY_LAST_PORT)?.toIntOrNull()

    fun setLastPort(port: Int) {
        props.setValue(KEY_LAST_PORT, port.toString())
    }

    fun getSavedRequest(endpointKey: String): ApiRequest? {
        val raw = props.getValue(storageKey(endpointKey)) ?: return null
        return runCatching { objectMapper.readValue(raw, ApiRequest::class.java) }.getOrNull()
    }

    fun saveRequest(endpointKey: String, request: ApiRequest) {
        val raw = runCatching { objectMapper.writeValueAsString(request) }.getOrNull() ?: return
        props.setValue(storageKey(endpointKey), raw)
    }

    private fun storageKey(endpointKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(endpointKey.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$KEY_REQUEST_DRAFT_PREFIX$digest"
    }

    companion object {
        private const val KEY_APP_CLASS = "apiRunner.selectedSpringBootAppClass"
        private const val KEY_LAST_PORT = "apiRunner.lastServerPort"
        private const val KEY_REQUEST_DRAFT_PREFIX = "apiRunner.requestDraft."
    }
}
