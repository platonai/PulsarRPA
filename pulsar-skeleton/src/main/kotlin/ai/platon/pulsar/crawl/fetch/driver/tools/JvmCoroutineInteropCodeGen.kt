package ai.platon.pulsar.crawl.fetch.driver.tools

import ai.platon.pulsar.common.ResourceLoader

class JvmCoroutineInteropCodeGen {
    fun extractParameters(parameters: String): List<String> {
        return """(.+)\s*:\s*(.+)\s*,?""".toRegex().findAll(parameters)
            .mapNotNull { it.groups[1]?.value }
            .toList()
    }

    fun transform(methodName: String, parameters: String, returnType: String): String? {
        val parameterList = extractParameters(parameters).joinToString(", ")

        return """
override fun ${methodName}Async($parameters) = interopScope.future { $methodName($parameterList) }
            """.trimMargin()
    }

    fun transform(originalMethod: String): String? {
        val regex = """.+\s+(.+)\((.*)\):?\s*(.*)""".toRegex()

//        println(originalMethod.matches(regex))
        val result = regex.find(originalMethod)
        if (result != null) {
            val (signature, methodName, parameters, returnType) = result.groups.map { it?.value }
            return transform(methodName!!, parameters ?: "", returnType ?: "Unit")
        }

        println("Can not transform: >>>$originalMethod<<<")

        return null
    }
}

fun main() {
    val gen = JvmCoroutineInteropCodeGen()
    ResourceLoader.readAllLines("tools/WebDriver.kt.txt")
        .map { it.trim() }
        .filter { it.contains("suspend fun ") }
        .map { gen.transform(it) }
        .forEach { println(it) }
}
