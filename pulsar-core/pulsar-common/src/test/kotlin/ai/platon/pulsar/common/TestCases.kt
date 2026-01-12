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
}
