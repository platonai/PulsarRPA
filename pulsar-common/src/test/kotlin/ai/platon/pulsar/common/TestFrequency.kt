package ai.platon.pulsar.common

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by vincent on 17-1-14.
 */
class TestFrequency {

    val urls = arrayOf(
        "http://a.example.com/cn/news/201810/a/b/file1.htm",
        "http://a.example.com/cn/news/201810/a/b/file2.htm",
        "http://a.example.com/cn/news/201810/a/b/file3.htm",
        "http://a.example.com/en/news/201810/a/b/file4.htm",
        "http://a.example.com/en/news/201810/a/b/file5.htm",
        "http://a.example.com/en/news/201810/e/c/file6.htm",
        "http://a.example.com/en/video/201812/d/file7.htm",
        "http://a.example.com/en/video/201812/d/file8.htm",
        "http://a.example.com/en/video/file9.htm"
    )

    @Test
    fun testFrequency() {
        val freq = Frequency<String>()
        freq.addAll(listOf("a", "a", "b", "a", "b", "a", "c", "d"))
        println("iteration: ")
        freq.forEachIndexed { i, s -> print("$s\t") }
        assertEquals("a,a,a,a,b,b,c,d", freq.joinToString())
        println("\nentry set: ")
        freq.entrySet().also { println(it) }
        assertEquals("[a x 4, b x 2, c, d]", freq.entrySet().toString())
        println("\nelement set: ")
        freq.elementSet().also { println(it) }
        assertEquals("[a, b, c, d]", freq.elementSet().toString())

        println("\nsize: ${freq.size}")
        assertEquals(4, freq.size)
        println("\ntotal frequency: ${freq.totalFrequency}")
        assertEquals(8, freq.totalFrequency)
        println("\nmode: ${freq.mode}")
        assertEquals("a", freq.mode)
        println("\nmodes: ${freq.modes}")
        assertEquals("[a, b, c, d]", freq.modes.toString())

        println("\ncumulative frequency: ")
        freq.elementSet().associateBy({ it }) { freq.cumulativeFrequencyOf(it) }.also { println(it) }

        println("\npercentage: ")
        freq.elementSet().associateBy({ it }) { freq.percentageOf(it) }.also { println(it) }
        println("\ncumulative percentage: ")
        freq.elementSet().associateBy({ it }) { freq.cumulativePercentageOf(it) }.also { println(it) }
    }

    @Test
    fun testFrequencyTree() {
        val frequency: Frequency<String> = urls.flatMapTo(Frequency()) { it.split("/") }
        println(frequency)

        val tree = FrequencyTree(frequency)
        // tree.print()
        val ptree = tree.root.convert()
        BTreePrinter.print(ptree)
    }

    @Test
    fun testToString() {
        val frequency = Frequency<String>()
        IntRange(1, 10).forEach {  i ->
            repeat(i) { frequency.add("$i") }
        }
//        println(frequency.toString())
//        println(frequency.toPString())
        // Probability string
        assertEquals("1:0.02\t2:0.04\t3:0.05\t4:0.07\t5:0.09\t6:0.11\t7:0.13\t8:0.15\t9:0.16\t10:0.18",
            frequency.toPString())
        // Frequency string
        assertEquals("1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9, 10: 10", frequency.toString())
    }
}
