package com.apirunner.ide.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ApiRunnerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.getService(ApiRunnerResultService::class.java)
        val content = ContentFactory.getInstance().createContent(service.content, "Result", false)
        toolWindow.contentManager.addContent(content)
    }
}
