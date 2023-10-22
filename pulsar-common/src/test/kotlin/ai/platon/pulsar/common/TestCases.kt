package ai.platon.pulsar.common

import ai.platon.pulsar.common.urls.UrlTree
import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultimap
import com.google.common.collect.TreeMultiset
import com.google.common.net.InetAddresses
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.Precision
import org.junit.Ignore
import org.junit.Test
import java.awt.Color
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestClass(
    val file: String = "",
    val preprocessor: String = "",
    val wordsComparator: Comparator<String> = kotlin.Comparator { t, t2 -> t.compareTo(t2) }
) {
    private val lines = TreeMultiset.create<String>()
    init { load() }
    fun merge(other: SingleFiledLines) {}
    operator fun contains(text: String): Boolean = true
    fun lines(): Multiset<String> = lines
    val size: Int get() = lines.size
    val isEmpty: Boolean get() = lines.isEmpty()
    val isNotEmpty: Boolean get() = lines.isNotEmpty()
    fun load() {}
    fun saveTo(destFile: String) {}
    fun save() {}

    interface Preprocessor {
        fun process(line: String): String
    }

    class TextPreprocessor : Preprocessor {
        override fun process(line: String): String = ""
    }

    class RegexPreprocessor : Preprocessor {
        override fun process(line: String): String = ""
    }
}


class TestCases {

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
    fun testPostIncrementOperator() {
        var i = 0
        assertTrue { i++ == 0 }
    }

    @Test
    fun testPostIncrementOperatorAndRemainder() {
        assertEquals(0, 0 % 20)
        assertEquals(0, 20 % 20)
        assertEquals(0, 40 % 20)

        val numbers = mutableListOf<Int>()
        var k = 0
        while (k < 1000) {
            if (k++ % 20 == 0) {
                // k is the number of consecutive warnings, the sequence of k is: 1, 21, 41, 61, 81, 101, ...
                numbers.add(k)
            }
        }
        println(numbers)
        assertTrue { 21 in numbers }
        assertTrue { 61 in numbers }
        assertTrue { 201 in numbers }
        assertTrue { 561 in numbers }
    }

    @Test
    fun testOptional() {
        val s1 = Optional.ofNullable("hello")
        val s2 = Optional.ofNullable<String>(null)

        assertEquals("hello", s1.get())
        assertEquals("(null)", s2.orElse("(null)"))
    }

    @Test
    fun testReflection() {
        val clazz = SingleFiledLines::class
        assertEquals(1, clazz.constructors.size)
        // println("constructors: " + clazz.constructors.size)
        println("members: " + clazz.members.size)
        assertEquals(16, clazz.members.size)

        val ctor = clazz.constructors.first()
        assertEquals(3, ctor.parameters.size)
        // println("first constructor parameters: " + ctor.parameters.size)
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

    @Ignore("Not actually a test")
    @Test
    fun testGeneratePopularDurations() {
        IntRange(1, 60).forEach { i ->
            println("val sec$i = Duration.ofSeconds($i)")
        }

        println("")
        IntRange(1, 60).forEach { i ->
            println("val min$i = Duration.ofMinutes($i)")
        }

        println("")
        IntRange(1, 60).forEach { i ->
            println("val hour$i = Duration.ofHours($i)")
        }

        println("")
        IntRange(1, 20).forEach { i ->
            println("val day$i = Duration.ofDays($i)")
        }
        IntRange(21, 200).filter { it % 5 == 0 }.forEach { i ->
            println("val day$i = Duration.ofDays($i)")
        }

        println("")
        IntRange(1, 10).forEach { i ->
            println("val year$i = Duration.ofDays($i * 365)")
        }
    }

    @Test
    fun testNumber() {
        val a = 100000
        val b = 23
        val n = a + 23

//        println(n % a)
//        println((a * 3 + b) % a)

        for (i in 0..10) {
            assertEquals(b, (a * i + b) % a, "i=$i")
        }

        assertTrue { Int.MIN_VALUE < Int.MAX_VALUE }
    }

    @Test
    fun testEnv() {
        SystemUtils.USER_NAME
        println(System.getProperty("USER"))

        var env = System.getenv("XDG_SESSION_TYPE")
        println(env)
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
        assertTrue { Strings.isMoneyLike(text) }
        assertTrue { Strings.isNumericLike(text) }

        text = "10+"
        assertTrue { Strings.isNumericLike(text) }

        text = "￥669.00"

        // println(String.format("%10s%10s%10s%10s", "a", *arrayOf("b", "c", "d")))

        // println(arrayOf("").joinToString(";"))
    }

    @Ignore("Print special chars only if we want to check it")
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
    fun testStripSpecialChars() {
        val text = " hell\uE60Do\uDF21world "

        // text.forEachIndexed { i, it -> println("$i.\t$it -> ${StringUtil.isActuallyWhitespace(it.toInt())}") }

        assertEquals("helloworld", Strings.stripNonCJKChar(text))

        assertEquals("hell\uE60Do\uDF21world", Strings.stripNonPrintableChar(text))
        assertEquals("", Strings.stripNonPrintableChar("              "))
        assertEquals("a b c d e f g", Strings.stripNonPrintableChar(" a b c d e f g "))

        val unicodeChars = arrayOf('', 'Ɑ', 'ⰿ', '', '?', 'И', ' ')
        unicodeChars.forEach {
            print(Strings.stripNonPrintableChar("<$it>"))
            print('\t')
            print(Strings.stripNonCJKChar("<$it>", Strings.DEFAULT_KEEP_CHARS))
            println()
        }

        arrayOf("hello world", " hello      world ", " hello world ", "                 hello world").forEach {
            assertEquals("hello world", Strings.stripNonPrintableChar(it))
        }
    }

    @Test
    fun testRandomInt() {
//        val s = "%E4%B8%89%E9%87%8C%E7%95%882-03%E5%8F%B7%E5%9C%B0%E6%AE%B5%E8%A7%84%E5%88%92%E5%8F%8A%E7%BC%96%E5%88%B6"
//        println(URLDecoder.decode(s, StandardCharsets.UTF_8.toString()))

        val numbers = IntRange(1, 100).joinToString { Random.nextInt(0, 100000).toString(Character.MAX_RADIX) }
        println(numbers)
    }

    @Test
    fun testQualifiedClassNames() {
        val classNames = arrayOf(
                mutableSetOf(""),
                mutableSetOf("", "a", "b", "clearfix", "d", "right", "e", "f")
        ).map { getQualifiedClassNames(it) }.forEach {
            if (it.isNotEmpty()) {
                println(it.joinToString(".", ".") { it })
            }
        }
    }

    fun getQualifiedClassNames(classNames: MutableSet<String>): MutableSet<String> {
        classNames.remove("")
        if (classNames.isEmpty()) return classNames
        arrayOf("clearfix", "left", "right", "l", "r").forEach {
            classNames.remove(it)
            if (classNames.isEmpty()) {
                classNames.add(it)
                return@forEach
            }
        }
        return classNames
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
    fun testUUID() {
        for (i in 0..100) {
            val uuid = UUID.randomUUID()
            assertEquals(36, uuid.toString().length)
        }
    }

    @Test
    fun testInetAddresses() {
        assertTrue(InetAddresses.isInetAddress("12.3.3.4"))
        assertTrue(InetAddresses.isInetAddress("::1"))
        assertFalse(InetAddresses.isInetAddress("12.3.3.4:80"))
        assertFalse(InetAddresses.isInetAddress(".3.3.4:80"))
    }
}
