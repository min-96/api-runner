package com.apirunner.ide.ui

import com.apirunner.core.model.ApiRequest
import com.intellij.json.JsonFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBTextField
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ApiRequestDialogWrapper(
    project: Project,
    private val initial: ApiRequest
) : DialogWrapper(project) {

    private val pathField = JBTextField(initial.pathTemplate)
    private val queryField = JBTextField(initial.queryParams.entries.joinToString("&") { "${it.key}=${it.value}" })
    private val headersField = JBTextField(initial.headers.entries.joinToString(";") { "${it.key}:${it.value}" })
    private val bodyEditor = EditorTextField(
        EditorFactory.getInstance().createDocument(initial.body.orEmpty()),
        project,
        JsonFileType.INSTANCE
    ).apply {
        setOneLineMode(false)
        val scheme = EditorColorsManager.getInstance().globalScheme
        background = scheme.defaultBackground
        preferredSize = Dimension(640, 260)
    }

    init {
        title = "Run API: ${initial.method} ${initial.pathTemplate}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        var row = 0

        addRow(panel, row++, "Path", pathField, 0.0)
        addRow(panel, row++, "Query (k=v&k2=v2)", queryField, 0.0)
        addRow(panel, row++, "Headers (k:v;k2:v2)", headersField, 0.0)
        addRow(panel, row, "Body (JSON)", bodyEditor, 1.0)
        return panel
    }

    private fun addRow(panel: JPanel, row: Int, label: String, field: JComponent, weightY: Double) {
        panel.add(
            JLabel(label),
            GridBagConstraints().apply {
                gridx = 0
                gridy = row
                anchor = GridBagConstraints.NORTHWEST
                insets = Insets(4, 4, 4, 8)
            }
        )
        panel.add(
            field,
            GridBagConstraints().apply {
                gridx = 1
                gridy = row
                weightx = 1.0
                this.weighty = weightY
                fill = GridBagConstraints.BOTH
                insets = Insets(4, 0, 4, 4)
            }
        )
    }

    override fun doValidate(): ValidationInfo? {
        val request = buildRequest() ?: return ValidationInfo("Request could not be built")
        val validator = RequestDialog()
        val errors = validator.validate(request)
        if (errors.isNotEmpty()) {
            return ValidationInfo(errors.first())
        }
        return null
    }

    fun buildRequest(): ApiRequest? {
        val parsedQuery = parseQuery(queryField.text)
        val parsedHeaders = parseHeaders(headersField.text)
        return initial.copy(
            pathTemplate = pathField.text.trim().ifEmpty { initial.pathTemplate },
            queryParams = parsedQuery.toMutableMap(),
            headers = parsedHeaders.toMutableMap(),
            body = bodyEditor.text.takeIf { it.isNotBlank() }
        )
    }

    private fun parseQuery(input: String): Map<String, String> {
        if (input.isBlank()) {
            return emptyMap()
        }
        return input.split('&').mapNotNull {
            val idx = it.indexOf('=')
            if (idx <= 0) {
                null
            } else {
                it.substring(0, idx).trim() to it.substring(idx + 1).trim()
            }
        }.toMap()
    }

    private fun parseHeaders(input: String): Map<String, String> {
        if (input.isBlank()) {
            return emptyMap()
        }
        return input.split(';').mapNotNull {
            val idx = it.indexOf(':')
            if (idx <= 0) {
                null
            } else {
                it.substring(0, idx).trim() to it.substring(idx + 1).trim()
            }
        }.toMap()
    }
}
