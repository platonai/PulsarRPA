package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.skeleton.ai.ToolCall
import ai.platon.pulsar.skeleton.common.llm.LLMUtils
import kotlin.reflect.KClass

class SystemToolExecutor: AbstractToolExecutor() {
    private val logger = getLogger(this)

    override val domain = "system"

    override val targetClass: KClass<*> = SystemToolExecutor::class

    fun help(domain: String, method: String): String {
        return when (domain) {
            "agent" -> {
                when (method) {
                    "extract" -> extractHelp()
                    else -> ""
                }
            }
            "driver" -> LLMUtils.readWebDriverFromResource()
            else -> return "usage: `system.help(domain: String, method: String): String`"
        }
    }

    fun extractHelp(): String {
        val help = $$"""
使用 `agent.extract` 满足高级数据提取要求：

- 对提取结果格式有严格要求
- 提取结果存在内嵌对象
- 其他数据提取工具无法满足要求

参数说明：

1. `instruction`: 准确描述 1. 数据提取目标 2. 数据提取要求
2. `schema`: JSON 格式描述的数据提取结果 schema 要求，遵循如下模式：

```kotlin
class ExtractionField(
    name: String,
    type: String = "string",                 // JSON schema primitive or 'object' / 'array'
    description: String,
    required: Boolean = true,
    properties: List<ExtractionField> = emptyList(), // children if object
    items: ExtractionField? = null                    // item schema if array
)
class ExtractionSchema(fields: List<ExtractionField>)
```

对应的 JSON 描述：

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ExtractionSchema",
  "type": "object",
  "required": ["fields"],
  "properties": {
    "fields": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/ExtractionField"
      }
    }
  },
  "definitions": {
    "ExtractionField": {
      "type": "object",
      "required": ["name", "description"],
      "properties": {
        "name": { "type": "string" },
        "type": {
          "type": "string",
          "default": "string",
          "enum": ["string", "number", "boolean", "object", "array"]
        },
        "description": { "type": "string" },
        "required": {
          "type": "boolean",
          "default": true
        },
        "properties": {
          "type": "array",
          "items": { "$ref": "#/definitions/ExtractionField" },
          "default": []
        },
        "items": {
          "$ref": "#/definitions/ExtractionField",
          "default": null
        }
      }
    }
  }
}
```

示例：

```json
{
  "fields": [
    {
      "name": "product",
      "type": "object",
      "description": "Product info",
      "properties": [
        {
          "name": "name",
          "type": "string",
          "description": "Product name",
          "required": true
        },
        {
          "name": "variants",
          "type": "array",
          "required": false,
          "items": {
            "name": "variant",
            "type": "object",
            "required": false,
            "properties": [
              { "name": "sku", "type": "string", "required": false },
              { "name": "price", "type": "number", "required": false }
            ]
          }
        }
      ]
    }
  ]
}
```

        """.trimIndent()

        return help
    }

    /**
     * Execute system.* expressions with named args.
     */
    @Suppress("UNUSED_PARAMETER")
    @Throws(IllegalArgumentException::class)
    override suspend fun execute(
        objectName: String, functionName: String, args: Map<String, Any?>, target: Any
    ): Any? {
        require(objectName == "system") { "Object must be an System" }
        require(functionName.isNotBlank()) { "Function name must not be blank" }

        return when (functionName) {
            "help" -> {
                validateArgs(args, allowed = emptySet(), required = emptySet(), functionName)
                mapOf(
                    "domain" to domain,
                    "methods" to listOf("help")
                )
            }
            else -> throw IllegalArgumentException("Unsupported system method: $functionName(${args.keys})")
        }
    }
}
