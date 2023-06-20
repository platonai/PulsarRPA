package ai.platon.pulsar.common

import ai.platon.pulsar.common.options.OptionUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestOptions {

    @Test
    fun testFindOption() {
        val label = "best-sellers"
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
            val result = "-label\\s+([\\-_a-zA-Z0-9]+)\\s?".toRegex().find(args)
            assertNotNull(result)

            println("$args")
            result.groups.forEach { println(it) }
            println()

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
