package ai.platon.pulsar.common

import com.google.common.net.InetAddresses
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.Precision
import java.awt.Color
import java.math.BigInteger
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.test.*

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
        printlnPro(numbers)
        assertTrue { 21 in numbers }
        assertTrue { 61 in numbers }
        assertTrue { 201 in numbers }
        assertTrue { 561 in numbers }
    }

    @Test
    fun testIfEmpty() {
        val s = ""
        assertTrue { s.isEmpty() }
        assertTrue { s.isBlank() }
        assertTrue { s.ifEmpty { "empty" } == "empty" }
        assertTrue { s.ifBlank { "blank" } == "blank" }
    }

    @Test
    fun testReturnInIfEmpty() {
        val s = ""
        var result = s.ifEmpty { return }
        assertTrue { result == "This will never happen" }

        printlnPro("Will return to the caller")
        result = s.ifEmpty { return@testReturnInIfEmpty }
        assertTrue { result == "This will never happen" }
    }

    @Test
    fun testOptional() {
        val s1 = Optional.ofNullable("hello")
        val s2 = Optional.ofNullable<String>(null)

        assertEquals("hello", s1.get())
        assertEquals("(null)", s2.orElse("(null)"))
    }

    @Test
    fun testNumber() {
        val a = 100000
        val b = 23
        val n = a + 23

        for (i in 0..10) {
            assertEquals(b, (a * i + b) % a, "i=$i")
        }

        assertTrue { Int.MIN_VALUE < Int.MAX_VALUE }
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
    fun testNumberRoundTo() {
        val numbers = listOf(391, 392, 393, 394, 395, 396, 397, 398, 399)
        val s = numbers.map { (it + 2.5).toInt() / 5 * 5 }
        assertContentEquals(listOf(390, 390, 395, 395, 395, 395, 395, 400, 400), s)
        // logPrintln(s)

        val s2 = numbers.map { (it.toDouble() / 5).roundToInt() * 5 }
        assertContentEquals(listOf(390, 390, 395, 395, 395, 395, 395, 400, 400), s2)
        // logPrintln(s2)

        val s3 = doubleArrayOf(0.5, 0.61, 1.0, 1.3, 1.5, 2.0, 2.8, 3.0).map { floor(it) }
        // logPrintln(s3)
        assertContentEquals(listOf(0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 2.0, 3.0), s3)
    }

    @Test
    fun testStringPattern() {
        assertEquals(8891, '⊻'.code)
        assertEquals("a", "\t a\t\n".trim())

        assertEquals(58893, ''.code)

        assertTrue(' '.isWhitespace())

        var text = "¥58.00"
        assertTrue { Strings.isMoneyLike(text) }
        assertTrue { Strings.isNumericLike(text) }

        text = "10+"
        assertTrue { Strings.isNumericLike(text) }

        text = "￥669.00"

        // logPrintln(String.format("%10s%10s%10s%10s", "a", *arrayOf("b", "c", "d")))

        // logPrintln(arrayOf("").joinToString(";"))
    }

    @Test
    fun testStripSpecialChars() {
        val text = " hell\uE60Do\uDF21world "

        // text.forEachIndexed { i, it -> logPrintln("$i.\t$it -> ${StringUtil.isActuallyWhitespace(it.toInt())}") }

        assertEquals("helloworld", Strings.removeNonCJKChar(text))

        assertEquals("hell\uE60Do\uDF21world", Strings.removeNonPrintableChar(text))
        assertEquals("", Strings.removeNonPrintableChar("              "))
        assertEquals("a b c d e f g", Strings.removeNonPrintableChar(" a b c d e f g "))

        arrayOf("hello world", " hello      world ", " hello world ", "                 hello world").forEach {
            assertEquals("hello world", Strings.removeNonPrintableChar(it))
        }
    }

    @Test
    fun testColor() {
        assertEquals("#00ffff", String.format("#%02x%02x%02x", 0, 255, 255))
        assertEquals(65535, BigInteger("00ffff", 16).toLong())
        val rgb = 0x996600
        val rgb2 = Color(rgb).rgb
        val rgb3 = Color(rgb2, true).rgb
        printlnPro("$rgb\t$rgb2\t$rgb3")
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
    fun testRange() {
        var a = 0
        for (i in 10 downTo 1) {
            a = i
        }
        require(a == 1)
        IntRange(1, 10).toList().also { printlnPro(it) }
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

//        logPrintln(ds)
//        logPrintln("$q1\t$q2\t$q3\t$q4\twhisker:$whisker")

        assertEquals(2, ds.n)
        assertEquals(100.0, ds.min)
        assertEquals(101.0, ds.max)
        assertEquals(100.5, ds.mean)

        assertEquals(100.0, q1)
        assertEquals(100.5, q2)
        assertEquals(101.0, q3)
        assertEquals(101.0, q4)

        assertEquals(102, whisker)
//        assertEquals(Double.NaN, ds.skewness)
//        assertEquals(Double.NaN, ds.kurtosis)
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
