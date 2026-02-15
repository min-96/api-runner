package com.apirunner.ide.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.json.JsonFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.ScrollPaneConstants
import javax.swing.JComponent
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class ApiRunnerResultService(private val project: Project) : ResultSink {
    private val formatter = ResultTextFormatter()
    private val brightTextColor: Color = UIUtil.getLabelForeground().brighter()

    private val statusField = JBTextField().apply {
        isEditable = false
        foreground = brightTextColor
    }

    private val statusPanel = JPanel(BorderLayout()).apply {
        add(JBLabel("Status / Latency").apply { foreground = brightTextColor }, BorderLayout.NORTH)
        add(statusField, BorderLayout.CENTER)
    }

    private val sections: List<ResultSectionSpec> = listOf(
        ResultSectionSpec(
            key = "request",
            title = "Request",
            maxLength = MAX_REQUEST_LENGTH,
            extractor = { result -> formatter.formatRequestPayload(result) },
            formatter = { text -> text }
        ),
        ResultSectionSpec(
            key = "responseBody",
            title = "Response Body",
            maxLength = MAX_BODY_LENGTH,
            extractor = { result -> result.body.orEmpty() },
            formatter = { text -> formatter.prettyJsonIfPossible(text) }
        )
    )

    private val sectionViews: List<SectionView> = sections.map { spec ->
        val editor = createViewerEditor()
        SectionView(spec = spec, editor = editor, panel = createScrollableEditorPanel(spec.title, editor))
    }

    val content: JComponent = JPanel(BorderLayout()).apply {
        statusPanel.preferredSize = Dimension(0, 52)
        add(statusPanel, BorderLayout.NORTH)
        add(buildVerticalSectionsComponent(sectionViews.map { it.panel }), BorderLayout.CENTER)
    }

    override fun update(result: ResultViewModel) {
        ApplicationManager.getApplication().invokeLater {
            val statusText = if (result.error == null) {
                "Status=${result.status}, Time=${result.durationMs}ms"
            } else {
                "Error: ${result.error}"
            }
            statusField.text = statusText

            for (view in sectionViews) {
                val raw = view.spec.extractor(result)
                val formatted = view.spec.formatter(raw)
                view.editor.text = formatter.truncate(formatted, view.spec.maxLength)
            }
        }
    }

    private fun createViewerEditor(): EditorTextField {
        return EditorTextField(
            EditorFactory.getInstance().createDocument(""),
            project,
            JsonFileType.INSTANCE
        ).apply {
            isViewer = true
            setOneLineMode(false)
            foreground = brightTextColor
        }
    }

    private fun createScrollableEditorPanel(title: String, editor: EditorTextField): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JBLabel(title).apply { foreground = brightTextColor }, BorderLayout.NORTH)
            add(
                JBScrollPane(
                    editor,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                ),
                BorderLayout.CENTER
            )
        }
    }

    private fun buildVerticalSectionsComponent(panels: List<JComponent>): JComponent {
        if (panels.isEmpty()) {
            return JPanel(BorderLayout())
        }
        if (panels.size == 1) {
            return panels.first()
        }

        var acc: JComponent = panels.first()
        for (index in 1 until panels.size) {
            val remaining = panels.size - index
            val proportion = when {
                remaining <= 0 -> 0.5f
                else -> (1f / (remaining + 1)).coerceIn(0.15f, 0.85f)
            }
            acc = JBSplitter(true, proportion).apply {
                firstComponent = acc
                secondComponent = panels[index]
                dividerWidth = 4
            }
        }
        return acc
    }

    private data class SectionView(
        val spec: ResultSectionSpec,
        val editor: EditorTextField,
        val panel: JPanel
    )

    companion object {
        private const val MAX_REQUEST_LENGTH = 512 * 1024
        private const val MAX_BODY_LENGTH = 2 * 1024 * 1024
    }
}
