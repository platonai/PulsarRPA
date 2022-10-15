package ai.platon.pulsar.persist

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberExtensionProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.assertTrue

class TestWebPageCodeGeneration {

    private fun convertReturnType(type: KType): String {
        var s = type.toString().filter { it != '!' }
            .replace("kotlin.", "")
            .replace("java.nio.", "")
            .replace("collections.(Mutable)", "")
            .replace("java.time.", "")

        if (!s.contains("<")) {
            s = s.substringAfterLast(".")
        }

        return s
    }

    @Test
    fun generateJavaInterface() {
        val properties = WebPage::class.declaredMemberFunctions
            .filter { it.isOpen }
            .filter { it.name.startsWith("get") }
            .map {
                convertReturnType(it.returnType) + " " +
                it.name + "();"
            }

        val clazz = """
            |interface Asset {
            |    ${properties.joinToString("\n    ")}
            |}
        """.trimMargin()

        println(clazz)
    }

    @Test
    fun generateJavaImmutableClass() {
        val properties = WebPage::class.declaredMemberFunctions
            .filter { it.isOpen }
            .filter { it.name.startsWith("get") }
            .map {
                convertReturnType(it.returnType) + " " +
                        it.name + "() { /** IMPLEMENTATION */ };"
            }

        val clazz = """
            |class WebPage {
            |    ${properties.joinToString("\n    ")}
            |}
        """.trimMargin()

        println(clazz)
    }

    @Test
    fun generateKotlinImmutableClass() {
        val properties = WebPage::class.declaredMemberFunctions
            .filter { it.isOpen }
            .filter { it.name.startsWith("get") }
            .map {
                "val " + StringUtils.uncapitalize(it.name.substringAfter("get")) +
                        ": " + it.returnType.toString().filter { it != '!' }
                            .replace("kotlin.", "")
                            .replace("java.time.", "")
            }

        val clazz = """
            |class Asset {
            |    ${properties.joinToString("\n    ")}
            |}
        """.trimMargin()

        println(clazz)
    }

    @Test
    fun generateKotlinMutableClass() {
        val properties = WebPage::class.declaredMemberFunctions
            .filter { it.isOpen }
            .filter { it.name.startsWith("get") }
            .map {
                "var " + StringUtils.uncapitalize(it.name.substringAfter("get")) +
                        ": " + convertReturnType(it.returnType)
            }

        val clazz = """
            |class MutableWebPage {
            |    ${properties.joinToString("\n    ")}
            |}
        """.trimMargin()

        println(clazz)
    }
}
