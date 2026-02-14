# 05_GENERATOR_DESIGN.md

# Spring Controller API Runner — Generator Design

---

## 1. 목적

이 문서는 `apirunner-generator` 모듈의 설계를 정의한다.

Generator의 역할은 다음과 같다:

- Controller의 `requestBodyType` 정보를 기반으로
- 자동으로 JSON Body를 생성하고
- PathVariable / QueryParam 기본값을 생성하며
- 사용자가 빠르게 수정 가능한 초기 입력을 제공하는 것

이 모듈은 **자동 입력 생성 전담 엔진**이다.

---

## 2. 설계 원칙

1. IntelliJ API에 의존하지 않는다.
2. PSI를 직접 사용하지 않는다.
3. Reflection 기반 MVP 구현을 사용한다.
4. 재귀 객체 생성이 가능해야 한다.
5. 무한 루프 방지를 위해 Depth 제한을 둔다.
6. 확장(Scenario, 사용자 정의 값 전략)을 고려한 구조로 설계한다.

---

## 3. 모듈 책임 범위

### 포함 범위

- RequestBody JSON 자동 생성
- Primitive 기본값 전략
- Collection 생성 전략
- Nested 객체 재귀 생성
- Depth 제한 처리
- Enum 처리 (MVP+)

### 제외 범위

- PSI 타입 분석
- HTTP 호출
- 서버 실행
- UI 표시
- Template 치환

---

## 4. 패키지 구조

```
apirunner-generator/
└── src/main/kotlin/com/apirunner/generator/
├── BodyGenerator.kt
├── DefaultBodyGenerator.kt
├── GeneratorConfig.kt
├── TypeInspector.kt
├── ReflectionTypeInspector.kt
├── DefaultValueStrategy.kt
├── SimpleDefaultValueStrategy.kt
└── model/
└── TypeField.kt
```
---

## 5. 전체 동작 흐름
```
IDE (PSI 분석)
↓
requestBodyType: "com.example.CreateUserRequest"
↓
BodyGenerator.generate(typeName)
↓
ReflectionTypeInspector
↓
DefaultValueStrategy
↓
JSON 문자열 생성
↓
Dialog에 표시
```
---

## 6. 핵심 인터페이스

---

### 6.1 BodyGenerator

```kotlin
interface BodyGenerator {

    fun generate(
        typeName: String,
        config: GeneratorConfig = GeneratorConfig()
    ): String
}
```
```kotlin


6.2 GeneratorConfig
data class GeneratorConfig(
    val maxDepth: Int = 3,
    val collectionSize: Int = 0,
    val useRandomValues: Boolean = false
)
```
필드설명

| 필드              | 설명             |
| --------------- | -------------- |
| maxDepth        | 재귀 객체 생성 최대 깊이 |
| collectionSize  | 리스트 기본 생성 개수   |
| useRandomValues | 랜덤 기본값 사용 여부   |

## 7. TypeInspector 설계
TypeInspector는 타입의 구조를 분석하는 책임을 가진다.

### 7.1 TypeField
```kotlin
data class TypeField(
    val name: String,
    val type: String
)
```

### 7.2 인터페이스
```kotlin


interface TypeInspector {

    fun isPrimitive(typeName: String): Boolean

    fun isCollection(typeName: String): Boolean

    fun getCollectionGeneric(typeName: String): String?

    fun getFields(typeName: String): List<TypeField>

    fun isEnum(typeName: String): Boolean

    fun getEnumValues(typeName: String): List<String>
}
```
## 8. ReflectionTypeInspector (MVP 구현)
```kotlin


class ReflectionTypeInspector : TypeInspector {

    override fun isPrimitive(typeName: String): Boolean {
        return typeName in primitiveTypes
    }

    override fun isCollection(typeName: String): Boolean {
        return typeName.contains("List") || typeName.contains("Set")
    }

    override fun getCollectionGeneric(typeName: String): String? {
        // MVP: 단순 처리
        return "java.lang.String"
    }

    override fun getFields(typeName: String): List<TypeField> {
        val clazz = Class.forName(typeName)
        return clazz.declaredFields.map {
            TypeField(it.name, it.type.name)
        }
    }

    override fun isEnum(typeName: String): Boolean {
        return Class.forName(typeName).isEnum
    }

    override fun getEnumValues(typeName: String): List<String> {
        val clazz = Class.forName(typeName)
        return clazz.enumConstants.map { it.toString() }
    }

    companion object {
        private val primitiveTypes = listOf(
            "java.lang.String",
            "kotlin.String",
            "int",
            "java.lang.Integer",
            "long",
            "java.lang.Long",
            "boolean",
            "java.lang.Boolean",
            "double",
            "java.lang.Double"
        )
    }
}
```

## 9. DefaultValueStrategy 설계
### 9.1 인터페이스
```kotlin
interface DefaultValueStrategy {
    fun defaultValue(typeName: String): Any?
}
```

### 9.2 기본 구현
```kotlin

class SimpleDefaultValueStrategy : DefaultValueStrategy {

    override fun defaultValue(typeName: String): Any? {
        return when (typeName) {
            "java.lang.String",
            "kotlin.String" -> "string"

            "int",
            "java.lang.Integer" -> 1

            "long",
            "java.lang.Long" -> 1L

            "boolean",
            "java.lang.Boolean" -> true

            "double",
            "java.lang.Double" -> 1.0

            else -> null
        }
    }
}
```

## 10. DefaultBodyGenerator 설계
```kotlin


class DefaultBodyGenerator(
    private val typeInspector: TypeInspector,
    private val defaultValueStrategy: DefaultValueStrategy
) : BodyGenerator {

    override fun generate(typeName: String, config: GeneratorConfig): String {

        val node = generateNode(typeName, 0, config)

        return ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(node)
    }

    private fun generateNode(
        typeName: String,
        depth: Int,
        config: GeneratorConfig
    ): Any? {

        if (depth > config.maxDepth) return null

        if (typeInspector.isPrimitive(typeName)) {
            return defaultValueStrategy.defaultValue(typeName)
        }

        if (typeInspector.isEnum(typeName)) {
            return typeInspector.getEnumValues(typeName).firstOrNull()
        }

        if (typeInspector.isCollection(typeName)) {
            val generic = typeInspector.getCollectionGeneric(typeName) ?: return emptyList<Any>()
            return List(config.collectionSize) {
                generateNode(generic, depth + 1, config)
            }
        }

        val fields = typeInspector.getFields(typeName)

        val map = mutableMapOf<String, Any?>()

        for (field in fields) {
            map[field.name] = generateNode(field.type, depth + 1, config)
        }

        return map
    }
}
```
## 11. PathVariable / QueryParam 기본값 전략
BodyGenerator 외에 ParamGenerator를 둘 수 있다.
```kotlin


object DefaultParamGenerator {

    fun generatePathVars(names: List<String>): Map<String, String> {
        return names.associateWith { "1" }
    }

    fun generateQueryParams(names: List<String>): Map<String, String> {
        return names.associateWith { "1" }
    }
}
```
## 12. 리스크 및 제한 사항
### R1. Reflection 한계
* Kotlin data class metadata 일부 누락 가능
* Generic 타입 정확한 파싱 어려움
* Inner class 처리 제한

### R2. 순환 참조
* Depth 제한으로 방지
* maxDepth 초과 시 null 반환

### R3. 컬렉션 Generic 미지원(MVP)
* 기본 String으로 fallback

--- 
## 13. 향후 개선 계획
### Phase 2
* Kotlin metadata 기반 필드 추출

* Generic 타입 정확한 처리

* Optional/Nullable 처리 개선

* 사용자 정의 샘플 값 전략 추가

### Phase 3
* 프로젝트별 preset 설정

* Annotation 기반 값 생성 (@SampleValue)

* JSONPath 기반 자동 저장 지원

## 14. 테스트 전략
### 단위 테스트 항목
* Primitive 타입 JSON 생성
* Nested 객체 생성
* Enum 생성
* Depth 제한 동작
* 컬렉션 생성

## 15. 다음 문서
* 06_HTTP_DESIGN.md
  * OkHttpExecutor
  * CookieJar
  * AuthInterceptor
  * JWT 자동 저장 전략

