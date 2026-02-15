package com.apirunner.ide.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ApiRunnerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val resultService = project.getService(ApiRunnerResultService::class.java)
        val settingsService = project.getService(ApiRunnerSettingsService::class.java)

        val resultContent = contentFactory.createContent(resultService.content, "Result", false)
        val settingsContent = contentFactory.createContent(settingsService.content, "Settings", false)

        toolWindow.contentManager.addContent(resultContent)
        toolWindow.contentManager.addContent(settingsContent)
    }
}
