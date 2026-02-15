package com.apirunner.ide.ui

import com.apirunner.ide.runtime.ApiRunnerProjectService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

@Service(Service.Level.PROJECT)
class ApiRunnerSettingsService(private val project: Project) {
    private val session = project.getService(ApiRunnerProjectService::class.java)
    private val brightTextColor: Color = UIUtil.getLabelForeground().brighter()

    private val portField = JBTextField().apply {
        columns = 8
        text = (session.getLastPort() ?: DEFAULT_PORT).toString()
    }

    private val messageLabel = JBLabel(" ").apply {
        foreground = brightTextColor
    }

    val content: JComponent = JPanel(BorderLayout()).apply {
        add(
            JPanel(GridBagLayout()).apply {
                val c = GridBagConstraints().apply {
                    insets = Insets(8, 8, 8, 8)
                    anchor = GridBagConstraints.WEST
                    fill = GridBagConstraints.HORIZONTAL
                    weightx = 0.0
                    gridx = 0
                    gridy = 0
                }
                add(JBLabel("Server Port").apply { foreground = brightTextColor }, c)

                c.gridx = 1
                c.weightx = 0.0
                add(portField, c)

                c.gridx = 2
                add(JButton("Apply").apply { addActionListener { persistPort() } }, c)

                c.gridx = 0
                c.gridy = 1
                c.gridwidth = 3
                c.weightx = 1.0
                add(messageLabel, c)
            },
            BorderLayout.NORTH
        )
    }

    init {
        portField.addActionListener { persistPort() }
    }

    private fun persistPort() {
        val port = portField.text.trim().toIntOrNull()
        if (port == null || port !in 1..65535) {
            messageLabel.text = "Invalid port. Enter a number between 1 and 65535."
            return
        }

        session.setLastPort(port)
        portField.text = port.toString()
        messageLabel.text = "Saved. Result runs will use port $port."
    }

    companion object {
        private const val DEFAULT_PORT = 8080
    }
}
