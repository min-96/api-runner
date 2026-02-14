package com.apirunner.ide.psi

import com.apirunner.core.model.EndpointDescriptor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.uast.UMethod

class EndpointExtractor {
    fun extract(method: PsiMethod): EndpointDescriptor {
        val classAnnotations = method.containingClass?.annotationsAsStrings().orEmpty()
        val methodAnnotations = method.annotationsAsStrings()
        val parameters = method.parameterList.parameters.map { it.toMethodParameter() }
        return extract(
            ControllerMethodInfo(
                classAnnotations = classAnnotations,
                methodAnnotations = methodAnnotations,
                parameters = parameters
            )
        )
    }

    fun extract(method: UMethod): EndpointDescriptor {
        return extract(method.javaPsi)
    }

    fun extract(method: ControllerMethodInfo): EndpointDescriptor {
        val classPath = extractPath(method.classAnnotations)
        val methodPath = extractPath(method.methodAnnotations)

        val fullPath = joinPaths(classPath, methodPath)

        val pathVars = method.parameters
            .filter { it.annotations.any { ann -> ann.startsWith("@PathVariable") } }
            .map { param ->
                extractAnnotationValue(param.annotations.first { it.startsWith("@PathVariable") }) ?: param.name
            }

        val queryParams = method.parameters
            .filter { it.annotations.any { ann -> ann.startsWith("@RequestParam") } }
            .map { param ->
                extractAnnotationValue(param.annotations.first { it.startsWith("@RequestParam") }) ?: param.name
            }

        val requestBodyType = method.parameters
            .firstOrNull { it.annotations.any { ann -> ann.startsWith("@RequestBody") } }
            ?.typeName

        val httpMethod = extractHttpMethod(method.methodAnnotations, hasRequestBody = requestBodyType != null)

        return EndpointDescriptor(
            httpMethod = httpMethod,
            pathTemplate = fullPath,
            pathVariableNames = pathVars,
            queryParamNames = queryParams,
            requestBodyType = requestBodyType
        )
    }

    private fun extractHttpMethod(annotations: List<String>, hasRequestBody: Boolean): String {
        val mapping = annotations.firstOrNull {
            it.startsWith("@GetMapping") ||
                it.startsWith("@PostMapping") ||
                it.startsWith("@PutMapping") ||
                it.startsWith("@DeleteMapping") ||
                it.startsWith("@PatchMapping") ||
                it.startsWith("@RequestMapping")
        } ?: return "GET"

        return when {
            mapping.startsWith("@GetMapping") -> "GET"
            mapping.startsWith("@PostMapping") -> "POST"
            mapping.startsWith("@PutMapping") -> "PUT"
            mapping.startsWith("@DeleteMapping") -> "DELETE"
            mapping.startsWith("@PatchMapping") -> "PATCH"
            else -> extractRequestMappingMethod(mapping) ?: if (hasRequestBody) "POST" else "GET"
        }
    }

    private fun extractRequestMappingMethod(annotation: String): String? {
        val regex = Regex("method\\s*=\\s*RequestMethod\\.(\\w+)")
        return regex.find(annotation)?.groupValues?.get(1)
    }

    private fun extractPath(annotations: List<String>): String {
        val mapping = annotations.firstOrNull {
            it.startsWith("@GetMapping") ||
                it.startsWith("@PostMapping") ||
                it.startsWith("@PutMapping") ||
                it.startsWith("@DeleteMapping") ||
                it.startsWith("@PatchMapping") ||
                it.startsWith("@RequestMapping")
        } ?: return ""

        extractAnnotationValue(mapping)?.let { return it }
        extractArrayAnnotationValue(mapping)?.let { return it }

        val regex = Regex("(value|path)\\s*=\\s*\"([^\"]+)\"")
        return regex.find(mapping)?.groupValues?.get(2).orEmpty()
    }

    private fun extractAnnotationValue(annotation: String): String? {
        val direct = Regex("\\(\\s*\"([^\"]+)\"\\s*\\)").find(annotation)?.groupValues?.get(1)
        if (direct != null) {
            return direct
        }

        val named = Regex("(value|name|path)\\s*=\\s*\"([^\"]+)\"").find(annotation)?.groupValues?.get(2)
        return named
    }

    private fun extractArrayAnnotationValue(annotation: String): String? {
        val namedArray = Regex("(value|path)\\s*=\\s*[\\[{]\\s*\"([^\"]+)\"").find(annotation)?.groupValues?.get(2)
        if (namedArray != null) {
            return namedArray
        }

        val directArray = Regex("\\(\\s*[\\[{]\\s*\"([^\"]+)\"").find(annotation)?.groupValues?.get(1)
        return directArray
    }

    private fun joinPaths(classPath: String, methodPath: String): String {
        val cp = classPath.trim()
        val mp = methodPath.trim()

        val left = if (cp.startsWith("/")) cp else if (cp.isNotEmpty()) "/$cp" else ""
        val right = if (mp.startsWith("/")) mp else if (mp.isNotEmpty()) "/$mp" else ""

        return (left + right)
            .replace(Regex("/{2,}"), "/")
            .ifEmpty { "/" }
    }

    private fun PsiClass.annotationsAsStrings(): List<String> {
        return modifierList?.annotations.orEmpty().map { it.toNormalizedText() }
    }

    private fun PsiMethod.annotationsAsStrings(): List<String> {
        return modifierList.annotations.map { it.toNormalizedText() }
    }

    private fun PsiParameter.toMethodParameter(): MethodParameter {
        val annotations = modifierList?.annotations.orEmpty().map { it.toNormalizedText() }
        val typeName = type.canonicalText
        return MethodParameter(name, annotations, typeName)
    }

    private fun PsiAnnotation.toNormalizedText(): String {
        val shortName = qualifiedName?.substringAfterLast('.') ?: nameReferenceElement?.text ?: "Unknown"
        val attrs = parameterList.attributes
        if (attrs.isEmpty()) {
            return "@$shortName"
        }
        val joined = attrs.joinToString(",") { attr ->
            val key = attr.name
            val value = attr.value?.text ?: ""
            if (key == null) value else "$key=$value"
        }
        return "@$shortName($joined)"
    }
}
