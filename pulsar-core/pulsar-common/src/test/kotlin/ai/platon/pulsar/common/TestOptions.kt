package ai.platon.pulsar.common

import ai.platon.pulsar.common.options.OptionUtils
import ai.platon.pulsar.common.options.OptionUtils.OPTION_REGEX
import kotlin.test.*

class TestOptions {

    @Test
    fun testSplitCommandLineByRegexWithNamedOptions() {
        val url = "https://www.amazon.com/s?k=insomnia&i=aps&page=100"
        val text = "Search insomnia on Amazon"
        val args = "-i 10d -ii 1h -label search"
        val linkString = "$url -text $text -args $args" + " "

        val names = listOf("text", "args", "href", "referrer")
            .map { " -$it " }
            .filter { it in linkString }
        val regex = names.joinToString("|").toRegex()
        val values = linkString.split(regex)

        println("Regex: $regex")
        values.forEach { println(it) }
        // assertTrue { "" }
    }

    @Test
    fun testSplitCommandLineByRegexWithUnnamedOptions() {
        val url = "https://www.amazon.com/s?k=insomnia&i=aps&page=100"
        val text = "Search insomnia on Amazon"
        val args = "-i 10d -ii 1h -label search"
        val linkString = "$url -text \"$text\" -args $args" + " "

        val regex = "-\\w+".toRegex()
        val values = linkString.split(regex)

        println("Regex: $regex")
        values.forEach { println(it) }
    }

    @Test
    fun testSplitCommandLine() {
        val url = "https://www.amazon.com/s?k=insomnia&i=aps&page=100"
        val text = "Search insomnia on Amazon"
        val args = "-i 10d -ii 1h -label search"
        val linkString = "$url -text \"$text\" -args \"$args\""

        val argv = OptionUtils.translateCommandline(linkString)

        argv.forEach { println(it) }
        assertEquals("-text", argv[1])
        assertEquals(text, argv[2])
        assertEquals("-args", argv[3])
        assertEquals(args, argv[4])
    }

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
