package com.apirunner.ide.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.json.JsonFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class ApiRunnerResultService(private val project: Project) {
    private val objectMapper = ObjectMapper()

    private val statusField = JBTextField().apply { isEditable = false }
    private val bodyEditor = EditorTextField(
        EditorFactory.getInstance().createDocument(""),
        project,
        JsonFileType.INSTANCE
    ).apply {
        isViewer = true
        setOneLineMode(false)
        val scheme = EditorColorsManager.getInstance().globalScheme
        background = scheme.defaultBackground
    }

    private val statusPanel = JPanel(BorderLayout()).apply {
        add(JBLabel("Status / Latency"), BorderLayout.NORTH)
        add(statusField, BorderLayout.CENTER)
    }

    val content: JComponent = JPanel(BorderLayout()).apply {
        val splitter = JBSplitter(true, 0.2f).apply {
            firstComponent = statusPanel
            secondComponent = bodyEditor
            dividerWidth = 4
        }
        add(splitter, BorderLayout.CENTER)
    }

    fun show(result: ResultViewModel) {
        ApplicationManager.getApplication().invokeLater {
            val statusText = if (result.error == null) {
                "Status=${result.status}, Time=${result.durationMs}ms"
            } else {
                "Error: ${result.error}"
            }
            statusField.text = statusText

            val formattedBody = formatBody(result.body.orEmpty())
            bodyEditor.text = if (formattedBody.length > MAX_BODY_LENGTH) {
                formattedBody.take(MAX_BODY_LENGTH) + "\n\n...truncated..."
            } else {
                formattedBody
            }
        }
    }

    private fun formatBody(body: String): String {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return body
        }

        return runCatching {
            val node = objectMapper.readTree(trimmed)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
        }.getOrDefault(body)
    }

    companion object {
        private const val MAX_BODY_LENGTH = 2 * 1024 * 1024
    }
}
