package ai.platon.pulsar.common

import ai.platon.pulsar.common.sql.ResultSetFormatter
import com.google.common.collect.TreeMultimap
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.Precision
import org.junit.Ignore
import org.junit.Test
import java.awt.Color
import java.awt.SystemColor.text
import java.math.BigInteger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.sql.Types
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestAnything {

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
    fun testReflection() {
        val clazz = SingleFiledLines::class
        println("constructors: " + clazz.constructors.size)
        println("members: " + clazz.members.size)

        val ctor = clazz.constructors.first()
        println("first constructor parameters: " + ctor.parameters.size)
    }

    @Test
    fun testBucket() {
        val ints = (0..200 step 5).toMutableList()
        ints.add(0, -1000)
        ints.add(1000)
        val buckets = ints.toList().map { it.toDouble() }.toDoubleArray()
        buckets.indices.map { String.format("%6d", it) }.joinToString("").also { println(it) }
        buckets.map { String.format("%6.0f", it) }.joinToString("").also { println(it) }
        val bucketIndexes = listOf(-10.0, -1.0, 0.0, 0.1, 1.0, 4.0, 5.0, 6.0, 9.0, 10.0, 11.0, 99.0, 100.0, 101.0, 199.0, 200.0, 201.0)
                .map { it to buckets.binarySearch(it) }
                .map { it.first to if (it.second < 0) -it.second - 2 else it.second }.toMap()

        println()
        bucketIndexes.keys.map { String.format("%6.1f", it) }.joinToString("").also { println(it) }
        bucketIndexes.values.map { String.format("%6d", it) }.joinToString("").also { println(it) }

        assertTrue { bucketIndexes[-1.0] == 0 }
        assertTrue { bucketIndexes[0.0] == 1 }
        assertTrue { bucketIndexes[1.0] == 1 }
        assertTrue { bucketIndexes[4.0] == 1 }
    }

    @Test
    fun testNumber() {
        val a = 100000
        val b = 23
        val n = a + 23

        println(n % a)
        println((a * 3 + b) % a)

        for (i in 0..10) {
            assertEquals(b, (a * i + b) % a, "i=$i")
        }
    }

    /**
     * The numerical comparison operators <, <=, >, and >= always return false if either or both operands are NaN.
     * The equality operator == returns false if either operand is NaN.
     * The inequality operator != returns true if either operand is NaN.
     * */
    @Test
    fun testDouble() {
        assertFalse { Double.NaN > Double.MAX_VALUE }
        assertFalse { Double.NaN < Double.MAX_VALUE }
        assertFalse { Double.NaN > 0 }
        assertFalse { Double.NaN < 0 }

        assertEquals(1, 1.9999999999999.toInt())
        assertEquals(1, 1.0000000000001.toInt())

        assertEquals(1.0, 1.0000000000000000000000000000000001)

        assertTrue { Precision.equals(1.0, 1.0000000000000001) }
        assertTrue { Precision.equals(1.0, 1.0000000000000000000000000000000001) }
        assertTrue { Precision.equals(1.00000000000000000000000000001, 1.0000000000000000000000000000000001) }
    }

    @Test
    fun testAlignment() {
        val numbers = listOf(391, 392, 393, 394, 395, 396, 397, 398, 399)
        val s = numbers.map { (it + 2.5).toInt() / 5 * 5 }.joinToString()
        println(s)

        val s2 = numbers.map { Math.round(it.toDouble() / 5) * 5 }.joinToString()
        println(s2)

        val s3 = doubleArrayOf(0.5, 0.61, 1.0, 1.3, 1.5, 2.0, 2.8, 3.0).map { Math.floor(it) }.joinToString()
        println(s3)
    }

    @Test
    fun testStringPattern() {
        assertEquals(8891, '⊻'.toInt())
        assertEquals("a", "\t a\t\n".trim())

        println(''.toInt())

        println(' '.isWhitespace())

        var text = "¥58.00"
        assertTrue { StringUtil.isMoneyLike(text) }
        assertTrue { StringUtil.isNumericLike(text) }

        text = "10+"
        assertTrue { StringUtil.isNumericLike(text) }

        text = "￥669.00"

        // println(String.format("%10s%10s%10s%10s", "a", *arrayOf("b", "c", "d")))

        // println(arrayOf("").joinToString(";"))
    }

    @Ignore("Print special chars if required")
    @Test
    fun printSpecialChars() {
        val s = '!'.toInt()
        val e = '◻'.toInt()
        for (i in s..e) {
            print(i.toChar())
            print('\t')
            if (i % 100 == 0) {
                print('\n')
            }
        }
    }

    @Test
    fun testSpecialChars() {
        val text = " hell\uE60Do\uDF21world "
        assertEquals("hell\uE60Do\uDF21world", StringUtil.stripNonPrintableChar(text))
        assertEquals("helloworld", StringUtil.stripNonCJKChar(text))

        val unicodeChars = arrayOf('', 'Ɑ', 'ⰿ', '', '?', 'И', ' ')
        unicodeChars.forEach {
            print(StringUtil.stripNonPrintableChar("<$it>"))
            print('\t')
            print(StringUtil.stripNonCJKChar("<$it>", StringUtil.DEFAULT_KEEP_CHARS))
            println()
        }

        arrayOf("hello world", " hello      world ", " hello world ", "                 hello world").forEach {
            assertEquals("hello world", StringUtil.stripNonPrintableChar(it))
        }
    }

    @Test
    fun testUrlDecoder() {
        val s = "%E4%B8%89%E9%87%8C%E7%95%882-03%E5%8F%B7%E5%9C%B0%E6%AE%B5%E8%A7%84%E5%88%92%E5%8F%8A%E7%BC%96%E5%88%B6"
        println(URLDecoder.decode(s, StandardCharsets.UTF_8.toString()))
    }

    @Test
    fun testStringFormat() {
        println(String.format("%06x", 0x333))
        println(String.format("%s %s", "a", null?:""))
    }

    @Test
    fun testColor() {
        assertEquals("#00ffff", String.format("#%02x%02x%02x", 0, 255, 255))
        assertEquals(65535, BigInteger("00ffff", 16).toLong())
        val rgb = 0x996600
        val rgb2 = Color(rgb).rgb
        val rgb3 = Color(rgb2, true).rgb
        println("$rgb\t$rgb2\t$rgb3")
        assertEquals(rgb3, rgb2)

        val mask = ALPHA_MASK
        val rgba = mask or rgb
        assertEquals(mask xor rgba, rgb)
    }

    @Test
    fun testMod() {
        for (i in 0..100) {
            for (j in 0..100) {
                assertEquals(j, (i * 255 + j) % 255)
            }
        }
    }

    @Test
    fun testTreeMultimap() {
        var start = Instant.now()
        val multimap = TreeMultimap.create<Int, Int>()
        for (i in 0..2000000) {
            multimap.put(i, i)
        }
        println("init: " + Duration.between(start, Instant.now()))

        start = Instant.now()
        var map = multimap.asMap()
        println("asMap: " + Duration.between(start, Instant.now()))
        println("${multimap.size()} -> ${map.size}")

        start = Instant.now()
        map = multimap.asMap()
        println("asMap: " + Duration.between(start, Instant.now()))
        println("${multimap.size()} -> ${map.size}")

        multimap.put(map.size + 1, map.size + 1)
        start = Instant.now()
        map = multimap.asMap()
        println("asMap: " + Duration.between(start, Instant.now()))
        println("${multimap.size()} -> ${map.size}")

        multimap.clear()
        start = Instant.now()
        map = multimap.asMap()
        println("asMap: " + Duration.between(start, Instant.now()))
        println("${multimap.size()} -> ${map.size}")
    }

    @Test
    fun testFrequency() {
        val freq = Frequency<String>()
        freq.addAll(listOf("a", "a", "b", "a", "b", "a", "c", "d"))
        println("iteration: ")
        freq.forEachIndexed { i, s -> print("$s\t") }
        println("\nentry set: ")
        freq.entrySet().also { println(it) }
        println("\nelement set: ")
        freq.elementSet().also { println(it) }

        println("\nsize: ${freq.size}")
        println("\ntotal frequency: ${freq.totalFrequency}")
        println("\nmode: ${freq.mode}")
        println("\nmodes: ${freq.modes}")

        println("\ncumulative frequency: ")
        freq.elementSet().associateBy({ it }) { freq.cumulativeFrequencyOf(it) }.also { println(it) }

        println("\npercentage: ")
        freq.elementSet().associateBy({ it }) { freq.percentageOf(it) }.also { println(it) }
        println("\ncumulative percentage: ")
        freq.elementSet().associateBy({ it }) { freq.cumulativePercentageOf(it) }.also { println(it) }
    }

    @Test
    fun testDistribution() {
        val d1 = UniformIntegerDistribution(0, 100)
        println(d1.sample(30).joinToString())

        val d2 = NormalDistribution(10.0, 1.0)
        println(d2.sample(30).joinToString())
    }

    @Test
    fun testRange() {
        var a = 0
        for (i in 10 downTo 1) {
            a = i
        }
        require(a == 1)
        IntRange(1, 10).toList().also { println(it) }
    }

    @Test
    fun testStatisticsIQR() {
        // val gaps = doubleArrayOf(100.0, 101.0, 102.0, 105.0, 107.0, 120.0, 122.0, 124.0, 180.0, 184.0, 500.0)
        val gaps = doubleArrayOf(100.0, 101.0)
        val ds = DescriptiveStatistics(gaps)
        val q1 = ds.getPercentile(25.0)
        val q2 = ds.getPercentile(50.0)
        val q3 = ds.getPercentile(75.0)
        val q4 = ds.getPercentile(100.0)
        val iqr = q3 - q1
        // upper 1.5*IQR whisker
        val whisker = (q3 + 1.5 * iqr).toInt()

        println(ds)
        println("$q1\t$q2\t$q3\t$q4\twhisker:$whisker")
    }

    @Test
    fun testUrlTree() {
        val urls = arrayOf(
                "http://a.com/cn/news/201810/a/b/file1.htm",
                "http://a.com/cn/news/201810/a/b/file2.htm",
                "http://a.com/cn/news/201810/a/b/file3.htm",
                "http://a.com/cn/news/201810/a/b/file4.htm",
                "http://a.com/cn/news/201810/a/b/file5.htm",
                "http://a.com/cn/news/201810/e/c/file6.htm",
                "http://a.com/cn/news/201812/d/file7.htm",
                "http://a.com/cn/news/201812/d/file8.htm",
                "http://a.com/cn/news/file9.htm"
        )
        val tree = UrlTree()
        urls.forEach { tree.add(it) }
        tree.print()
    }

    @Test
    fun testHaffmanTree() {
        val frequency: Frequency<String> = urls.flatMapTo(Frequency()) { it.split("/") }
        println(frequency)

        val tree = HuffmanTree(frequency)
        ai.platon.pulsar.common.BTreePrinter.print(tree.root.convert())
        // tree.getEncodingMap().also { println(it) }
    }

    @Test
    fun testFrequencyTree() {
        val frequency: Frequency<String> = urls.flatMapTo(Frequency()) { it.split("/") }
        println(frequency)

        val tree = FrequencyTree(frequency)
        // tree.print()
        val ptree = tree.root.convert()
        ai.platon.pulsar.common.BTreePrinter.print(ptree)
    }

    @Test
    fun testUUID() {
        for (i in 0..100) {
            val uuid = UUID.randomUUID()
            assertEquals(36, uuid.toString().length)
        }
    }
}
