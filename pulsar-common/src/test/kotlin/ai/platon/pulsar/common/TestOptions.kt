package ai.platon.pulsar.common

import ai.platon.pulsar.common.options.OptionUtils
import ai.platon.pulsar.common.options.OptionUtils.OPTION_REGEX
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestOptions {

    @Test
    fun testFindOption() {
        val labels = listOf("best-sellers", "com.br", "asin")
        labels.forEach { label ->
            val argsList = listOf(
                "-label $label",
                "-label $label -parse",
                "-persist -label $label",
                "-persist -label $label -label $label",
                "-persist -label $label -parse",
                "-persist -label $label -label $label     -parse",
                "-expires 1s -label $label",
                "-expires 1s -label $label -parse",
                "-expires 1s -label $label       -label $label  -parse"
            )

            argsList.forEach { args ->
                val result = "-label$OPTION_REGEX".toRegex().find(args)
                assertNotNull(result)

//                println("$args")
//                result.groups.forEach { println(it) }
//                println()

                assertEquals(2, result.groups.size)
                assertEquals(label, result.groupValues[1])
            }

            argsList.forEach { args ->
                assertEquals(label, OptionUtils.findOption(args, "-label"), args)
            }

            argsList.forEach { args ->
                assertEquals(label, OptionUtils.findOption(args, listOf("-l", "-label", "--label")), args)
            }
        }
    }
}
