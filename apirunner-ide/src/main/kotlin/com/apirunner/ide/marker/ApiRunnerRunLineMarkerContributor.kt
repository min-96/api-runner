package com.apirunner.ide.marker

import com.apirunner.ide.runtime.ApiRunnerExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class ApiRunnerRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.firstChild != null) {
            return null
        }

        val psiMethod = resolveEndpointMethod(element) ?: return null
        if (!hasMappingAnnotation(psiMethod)) {
            return null
        }

        val action = object : AnAction("Run API", "Run API request", AllIcons.Actions.Execute) {
            override fun actionPerformed(e: AnActionEvent) {
                ApiRunnerExecutor(element.project).run(psiMethod)
            }
        }

        return Info(
            AllIcons.Actions.Execute,
            { "Run API" },
            action
        )
    }

    private fun resolveEndpointMethod(element: PsiElement): PsiMethod? {
        if (element is PsiIdentifier && element.parent is PsiMethod) {
            return element.parent as PsiMethod
        }

        var cursor: PsiElement? = element
        while (cursor != null) {
            val uMethod = cursor.toUElement(UMethod::class.java)
            if (uMethod != null) {
                val source = uMethod.sourcePsi
                if (source is PsiNameIdentifierOwner && source.nameIdentifier == element) {
                    return uMethod.javaPsi
                }
            }
            cursor = cursor.parent
        }
        return null
    }

    private fun hasMappingAnnotation(method: PsiMethod): Boolean {
        return method.modifierList.annotations.any { annotation ->
            val qName = annotation.qualifiedName.orEmpty()
            qName.endsWith("GetMapping") ||
                qName.endsWith("PostMapping") ||
                qName.endsWith("PutMapping") ||
                qName.endsWith("DeleteMapping") ||
                qName.endsWith("PatchMapping") ||
                qName.endsWith("RequestMapping")
        }
    }
}
