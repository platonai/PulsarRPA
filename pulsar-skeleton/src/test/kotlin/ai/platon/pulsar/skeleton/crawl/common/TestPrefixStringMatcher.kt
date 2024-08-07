
package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.common.PrefixStringMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for PrefixStringMatcher.
 */
class TestPrefixStringMatcher {
    private fun makeRandString(minLen: Int, maxLen: Int): String {
        val len = minLen + (Math.random() * (maxLen - minLen)).toInt()
        val chars = CharArray(len)
        for (pos in 0 until len) {
            chars[pos] = alphabet[(Math.random() * alphabet.size).toInt()]
        }
        return String(chars)
    }

    @Test
    fun testPrefixMatcher() {
        var numMatches = 0
        var numInputsTested = 0
        for (round in 0 until NUM_TEST_ROUNDS) { // build list of prefixes
            val numPrefixes = (Math.random() * MAX_TEST_PREFIXES).toInt()
            val prefixes = arrayOfNulls<String>(numPrefixes)
            for (i in 0 until numPrefixes) {
                prefixes[i] = makeRandString(0, MAX_PREFIX_LEN)
            }
            val prematcher = PrefixStringMatcher(prefixes)
            // test random strings for prefix matches
            for (i in 0 until NUM_TEST_INPUTS_PER_ROUND) {
                val input = makeRandString(0, MAX_INPUT_LEN)
                var matches = false
                var longestMatch = -1
                var shortestMatch = -1
                for (j in prefixes.indices) {
                    val prefix = prefixes[j]
                    if (!prefix.isNullOrEmpty() && input.startsWith(prefix)) {
                        matches = true
                        val matchSize = prefix.length
                        if (matchSize > longestMatch) longestMatch = matchSize
                        if (matchSize < shortestMatch || shortestMatch == -1) shortestMatch = matchSize
                    }
                }
                if (matches) numMatches++
                numInputsTested++
                val message = "'$input' should ${if (matches) "" else "not "}match!"
                assertEquals(matches, prematcher.matches(input), message)
                if (matches) {
                    assertEquals(shortestMatch, prematcher.shortestMatch(input).length)
                    assertEquals(input.substring(0, shortestMatch), prematcher.shortestMatch(input))
                    assertEquals(longestMatch, prematcher.longestMatch(input).length)
                    assertEquals(input.substring(0, longestMatch), prematcher.longestMatch(input))
                }
            }
        }
        println("got $numMatches matches out of $numInputsTested tests")
    }

    companion object {
        private const val NUM_TEST_ROUNDS = 20
        private const val MAX_TEST_PREFIXES = 100
        private const val MAX_PREFIX_LEN = 10
        private const val NUM_TEST_INPUTS_PER_ROUND = 100
        private const val MAX_INPUT_LEN = 20
        private val alphabet = charArrayOf('a', 'b', 'c', 'd')
    }
}