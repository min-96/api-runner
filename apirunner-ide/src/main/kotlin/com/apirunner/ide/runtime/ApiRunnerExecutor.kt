package com.apirunner.ide.runtime

import com.apirunner.core.engine.ExecutionContext
import com.apirunner.core.engine.ExecutionEngine
import com.apirunner.core.http.HttpExecutor
import com.apirunner.core.model.ApiRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.apirunner.generator.DefaultBodyGenerator
import com.apirunner.generator.DefaultParamGenerator
import com.apirunner.http.OkHttpClientProvider
import com.apirunner.http.OkHttpExecutor
import com.apirunner.ide.orchestration.ApiExecutionOrchestrator
import com.apirunner.ide.psi.EndpointExtractor
import com.apirunner.ide.server.ServerManager
import com.apirunner.ide.ui.ApiRequestDialogWrapper
import com.apirunner.ide.ui.ApiRunnerResultService
import com.apirunner.ide.ui.ResultPresenter
import com.apirunner.ide.ui.ResultSink
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import java.nio.file.Path
import java.nio.file.Paths

class ApiRunnerExecutor(private val project: Project) {
    private val endpointExtractor = EndpointExtractor()
    private val bodyGenerator = DefaultBodyGenerator()
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    fun run(method: PsiMethod) {
        val descriptor = endpointExtractor.extract(method)
        val session = project.getService(ApiRunnerProjectService::class.java)
        val endpointKey = "${descriptor.httpMethod} ${descriptor.pathTemplate}"

        val multipart = isMultipartRequest(method)
        val body = if (multipart) generateMultipartTemplate(method) else generateInitialBody(method, descriptor.requestBodyType)
        val headers = mutableMapOf<String, String>()
        if (multipart) {
            headers["Content-Type"] = "multipart/form-data"
        } else if (body != null) {
            headers["Content-Type"] = "application/json"
        }

        val request = ApiRequest(
            method = descriptor.httpMethod,
            pathTemplate = descriptor.pathTemplate,
            pathVariables = DefaultParamGenerator.generatePathVars(descriptor.pathVariableNames).toMutableMap(),
            queryParams = DefaultParamGenerator.generateQueryParams(descriptor.queryParamNames).toMutableMap(),
            headers = headers,
            body = body
        )
        val initialRequest = session.getSavedRequest(endpointKey)?.let { saved ->
            mergeSavedRequest(base = request, saved = saved)
        } ?: request

        val dialog = ApiRequestDialogWrapper(project, initialRequest)
        if (!dialog.showAndGet()) {
            return
        }
        val editedRawRequest = dialog.buildRequest() ?: return
        session.saveRequest(endpointKey, editedRawRequest)
        val editedRequest = normalizeMultipartFilePaths(editedRawRequest)

        object : Task.Backgroundable(project, "Running API", false) {
            override fun run(indicator: ProgressIndicator) {
                val serverManager = ServerManager(
                    process = IntellijServerProcess(session),
                    fallbackPortProvider = { session.getLastPort() ?: DEFAULT_PORT }
                )
                val httpExecutor = createHttpExecutor(session.context, session)
                val presenter = ResultPresenter(serviceBackedSink())
                val orchestrator = ApiExecutionOrchestrator(
                    serverManager = serverManager,
                    httpExecutor = httpExecutor,
                    resultPresenter = presenter,
                    engine = ExecutionEngine()
                )
                orchestrator.execute(editedRequest, session.context)
            }
        }.queue()
    }

    private fun mergeSavedRequest(base: ApiRequest, saved: ApiRequest): ApiRequest {
        val mergedPathVars = base.pathVariables.toMutableMap().apply { putAll(saved.pathVariables) }
        val mergedQuery = base.queryParams.toMutableMap().apply { putAll(saved.queryParams) }
        val mergedHeaders = base.headers.toMutableMap().apply { putAll(saved.headers) }

        return base.copy(
            pathTemplate = saved.pathTemplate.ifBlank { base.pathTemplate },
            pathVariables = mergedPathVars,
            queryParams = mergedQuery,
            headers = mergedHeaders,
            body = saved.body ?: base.body
        )
    }

    private fun createHttpExecutor(context: ExecutionContext, session: ApiRunnerProjectService): HttpExecutor {
        val client = OkHttpClientProvider.create(context = context, cookieJar = session.cookieJar)
        return OkHttpExecutor(client)
    }

    private fun isMultipartRequest(method: PsiMethod): Boolean {
        val methodConsumesMultipart = method.modifierList.annotations.any { annotation ->
            annotation.text.contains("multipart/form-data")
        }
        if (methodConsumesMultipart) {
            return true
        }

        return method.parameterList.parameters.any { parameter ->
            parameter.type.canonicalText.contains("MultipartFile") ||
                parameter.modifierList?.annotations.orEmpty().any { ann ->
                    ann.qualifiedName?.endsWith("RequestPart") == true ||
                        ann.text.contains("RequestPart")
                }
        }
    }

    private fun generateMultipartTemplate(method: PsiMethod): String {
        val lines = mutableListOf<String>()
        val parameters = method.parameterList.parameters
        for (parameter in parameters) {
            val annotations = parameter.modifierList?.annotations.orEmpty()
            val type = parameter.type.canonicalText
            val partName = extractNamedValue(annotations) ?: parameter.name

            if (type.contains("MultipartFile")) {
                lines += "$partName=@/path/to/$partName.bin"
                continue
            }

            val isRequestPart = annotations.any { ann ->
                ann.qualifiedName?.endsWith("RequestPart") == true || ann.text.contains("RequestPart")
            }
            if (!isRequestPart) {
                continue
            }

            val value = when {
                type == "java.lang.String" || type == "kotlin.String" -> "\"text\""
                else -> {
                    val generated = runCatching { bodyGenerator.generate(type) }.getOrNull()
                    if (!generated.isNullOrBlank() && generated.trim() != "null") {
                        runCatching {
                            objectMapper.writeValueAsString(objectMapper.readTree(generated))
                        }.getOrDefault(generated)
                    } else {
                        "{}"
                    }
                }
            }
            lines += "$partName=$value"
        }

        return lines.joinToString("\n")
    }

    private fun extractNamedValue(annotations: Array<out com.intellij.psi.PsiAnnotation>): String? {
        val target = annotations.firstOrNull { ann ->
            ann.qualifiedName?.endsWith("RequestPart") == true ||
                ann.qualifiedName?.endsWith("RequestParam") == true ||
                ann.text.contains("RequestPart") ||
                ann.text.contains("RequestParam")
        } ?: return null

        val text = target.text
        val direct = Regex("\\(\\s*\"([^\"]+)\"\\s*\\)").find(text)?.groupValues?.get(1)
        if (!direct.isNullOrBlank()) {
            return direct
        }
        val named = Regex("(value|name|path)\\s*=\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(2)
        return named
    }

    private fun generateInitialBody(method: PsiMethod, extractedType: String?): String? {
        val bodyType = extractedType ?: resolveRequestBodyType(method) ?: return null
        val reflected = runCatching { bodyGenerator.generate(bodyType) }.getOrNull()
        if (!reflected.isNullOrBlank() && reflected.trim() != "null") {
            return reflected
        }

        val fallbackNode = generateBodyNodeFromPsi(bodyType, depth = 0, seen = mutableSetOf())
            ?: linkedMapOf<String, Any?>()
        return runCatching { objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fallbackNode) }
            .getOrDefault("{}")
    }

    private fun resolveRequestBodyType(method: PsiMethod): String? {
        return ReadAction.compute<String?, RuntimeException> {
            method.parameterList.parameters.firstOrNull { parameter ->
                parameter.modifierList?.annotations.orEmpty().any { annotation ->
                    annotation.qualifiedName?.endsWith("RequestBody") == true ||
                        annotation.text.contains("RequestBody")
                }
            }?.type?.canonicalText
        }
    }

    private fun generateBodyNodeFromPsi(typeName: String, depth: Int, seen: MutableSet<String>): Any? {
        if (depth > 3) {
            return null
        }

        val normalized = typeName.substringAfterLast("?").trim()
        primitiveDefault(normalized)?.let { return it }

        if (isCollectionType(normalized)) {
            val generic = collectionGenericType(normalized) ?: "java.lang.String"
            return listOfNotNull(generateBodyNodeFromPsi(generic, depth + 1, seen))
        }

        val rawType = normalized.substringBefore('<').trim()
        if (rawType == "java.lang.Object" || rawType == "kotlin.Any") {
            return linkedMapOf<String, Any?>()
        }
        if (!seen.add(rawType)) {
            return null
        }

        try {
            enumFirstValue(rawType)?.let { return it }
            val fields = resolveFields(rawType)
            if (fields.isEmpty()) {
                return linkedMapOf<String, Any?>()
            }

            val out = linkedMapOf<String, Any?>()
            for ((name, fieldType) in fields) {
                out[name] = generateBodyNodeFromPsi(fieldType, depth + 1, seen)
            }
            return out
        } finally {
            seen.remove(rawType)
        }
    }

    private fun resolveFields(typeName: String): List<Pair<String, String>> {
        return ReadAction.compute<List<Pair<String, String>>, RuntimeException> {
            val psiClass = JavaPsiFacade.getInstance(project)
                .findClass(typeName, GlobalSearchScope.projectScope(project))
                ?: return@compute emptyList()

            psiClass.fields
                .filterNot { field ->
                    field.name.startsWith("$") ||
                        field.hasModifierProperty(PsiModifier.STATIC)
                }
                .map { it.name to it.type.canonicalText }
        }
    }

    private fun enumFirstValue(typeName: String): String? {
        return ReadAction.compute<String?, RuntimeException> {
            val psiClass = JavaPsiFacade.getInstance(project)
                .findClass(typeName, GlobalSearchScope.projectScope(project))
                ?: return@compute null
            if (!psiClass.isEnum) {
                return@compute null
            }
            psiClass.fields.filterIsInstance<PsiEnumConstant>().firstOrNull()?.name
        }
    }

    private fun isCollectionType(typeName: String): Boolean {
        return typeName.startsWith("java.util.List") ||
            typeName.startsWith("kotlin.collections.List") ||
            typeName.startsWith("java.util.Set") ||
            typeName.startsWith("kotlin.collections.Set")
    }

    private fun collectionGenericType(typeName: String): String? {
        val start = typeName.indexOf('<')
        val end = typeName.lastIndexOf('>')
        return if (start >= 0 && end > start) typeName.substring(start + 1, end).trim() else null
    }

    private fun primitiveDefault(typeName: String): Any? {
        return when (typeName) {
            "java.lang.String", "kotlin.String" -> "string"
            "int", "java.lang.Integer", "kotlin.Int" -> 1
            "long", "java.lang.Long", "kotlin.Long" -> 1L
            "boolean", "java.lang.Boolean", "kotlin.Boolean" -> true
            "double", "java.lang.Double", "kotlin.Double" -> 1.0
            "float", "java.lang.Float", "kotlin.Float" -> 1.0
            else -> null
        }
    }

    private fun serviceBackedSink(): ResultSink {
        return project.getService(ApiRunnerResultService::class.java)
    }

    private fun normalizeMultipartFilePaths(request: ApiRequest): ApiRequest {
        val isMultipart = request.headers.any { (k, v) ->
            k.equals("Content-Type", ignoreCase = true) &&
                v.contains("multipart/form-data", ignoreCase = true)
        }
        if (!isMultipart) {
            return request
        }

        val basePath = project.basePath ?: return request
        val projectRoot = Paths.get(basePath)
        val rewritten = request.body.orEmpty().lineSequence()
            .joinToString("\n") { line ->
                val trimmed = line.trim()
                val idx = trimmed.indexOf('=')
                if (idx <= 0) {
                    return@joinToString line
                }
                val key = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim()
                if (!value.startsWith("@")) {
                    return@joinToString line
                }
                val rawPath = value.removePrefix("@").trim()
                val resolved = resolvePathFromProjectRoot(rawPath, projectRoot) ?: return@joinToString line
                "$key=@$resolved"
            }
        return request.copy(body = rewritten)
    }

    private fun resolvePathFromProjectRoot(rawPath: String, projectRoot: Path): String? {
        val candidate = runCatching { Paths.get(rawPath) }.getOrNull() ?: return null
        if (candidate.isAbsolute) {
            return null
        }
        val normalized = projectRoot.resolve(candidate).normalize()
        if (!normalized.startsWith(projectRoot)) {
            return null
        }
        return normalized.toString()
    }

    companion object {
        private const val DEFAULT_PORT = 8080
    }
}
