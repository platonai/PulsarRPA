/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.PrefixStringMatcher
import org.junit.Assert
import kotlin.test.*

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
                    if (prefixes[j]!!.length > 0 && input.startsWith(prefixes[j]!!)) {
                        matches = true
                        val matchSize = prefixes[j]!!.length
                        if (matchSize > longestMatch) longestMatch = matchSize
                        if (matchSize < shortestMatch || shortestMatch == -1) shortestMatch = matchSize
                    }
                }
                if (matches) numMatches++
                numInputsTested++
                Assert.assertTrue("'" + input + "' should " + (if (matches) "" else "not ")
                        + "match!", matches == prematcher.matches(input))
                if (matches) {
                    Assert.assertTrue(shortestMatch == prematcher.shortestMatch(input).length)
                    Assert.assertTrue(input.substring(0, shortestMatch) ==
                            prematcher.shortestMatch(input))
                    Assert.assertTrue(longestMatch == prematcher.longestMatch(input).length)
                    Assert.assertTrue(input.substring(0, longestMatch) ==
                            prematcher.longestMatch(input))
                }
            }
        }
        println("got " + numMatches + " matches out of "
                + numInputsTested + " tests")
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