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
package ai.platon.pulsar.skeleton.crawl.common

import ai.platon.pulsar.common.SuffixStringMatcher
import org.junit.Assert
import kotlin.test.*

/**
 * Unit tests for SuffixStringMatcher.
 */
class TestSuffixStringMatcher {
    private fun makeRandString(minLen: Int, maxLen: Int): String {
        val len = minLen + (Math.random() * (maxLen - minLen)).toInt()
        val chars = CharArray(len)
        for (pos in 0 until len) {
            chars[pos] = alphabet[(Math.random() * alphabet.size).toInt()]
        }
        return String(chars)
    }

    @Test
    fun testSuffixMatcher() {
        var numMatches = 0
        var numInputsTested = 0
        for (round in 0 until NUM_TEST_ROUNDS) { // build list of suffixes
            val numSuffixes = (Math.random() * MAX_TEST_SUFFIXES).toInt()
            val suffixes = arrayOfNulls<String>(numSuffixes)
            for (i in 0 until numSuffixes) {
                suffixes[i] = makeRandString(0, MAX_SUFFIX_LEN)
            }
            val sufmatcher = SuffixStringMatcher(suffixes)
            // test random strings for suffix matches
            for (i in 0 until NUM_TEST_INPUTS_PER_ROUND) {
                val input = makeRandString(0, MAX_INPUT_LEN)
                var matches = false
                var longestMatch = -1
                var shortestMatch = -1
                for (j in suffixes.indices) {
                    if (suffixes[j]!!.length > 0 && input.endsWith(suffixes[j]!!)) {
                        matches = true
                        val matchSize = suffixes[j]!!.length
                        if (matchSize > longestMatch) longestMatch = matchSize
                        if (matchSize < shortestMatch || shortestMatch == -1) shortestMatch = matchSize
                    }
                }
                if (matches) numMatches++
                numInputsTested++
                Assert.assertTrue("'" + input + "' should " + (if (matches) "" else "not ")
                        + "match!", matches == sufmatcher.matches(input))
                if (matches) {
                    Assert.assertTrue(shortestMatch == sufmatcher.shortestMatch(input).length)
                    Assert.assertTrue(input.substring(input.length - shortestMatch) ==
                            sufmatcher.shortestMatch(input))
                    Assert.assertTrue(longestMatch == sufmatcher.longestMatch(input).length)
                    Assert.assertTrue(input.substring(input.length - longestMatch) ==
                            sufmatcher.longestMatch(input))
                }
            }
        }
        println("got " + numMatches + " matches out of "
                + numInputsTested + " tests")
    }

    companion object {
        private const val NUM_TEST_ROUNDS = 20
        private const val MAX_TEST_SUFFIXES = 100
        private const val MAX_SUFFIX_LEN = 10
        private const val NUM_TEST_INPUTS_PER_ROUND = 100
        private const val MAX_INPUT_LEN = 20
        private val alphabet = charArrayOf('a', 'b', 'c', 'd')
    }
}