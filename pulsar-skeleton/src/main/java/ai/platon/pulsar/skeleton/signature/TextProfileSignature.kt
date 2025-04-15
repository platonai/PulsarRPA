/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.skeleton.signature

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import org.apache.hadoop.io.MD5Hash
import java.util.*

/**
 * An implementation of a page signature. It calculates an MD5 hash of a plain
 * signature "profile" of a page. In case there is no signature, it calculates a hash
 * using the [MD5Signature].
 *
 * The algorithm to calculate a page "profile" takes the plain signature version of a
 * page and performs the following steps:
 *
 *  * remove all characters except letters and digits, and bring all characters to lower case,
 *  * split the signature into tokens (all consecutive non-whitespace characters),
 *  * discard tokens equal or shorter than MIN_TOKEN_LEN (default 2 characters),
 *  * sort the list of tokens by decreasing frequency,
 *  * round down the counts of tokens to the nearest multiple of QUANT (
 *  `QUANT = QUANT_RATE * maxFreq`, where `QUANT_RATE` is
 * 0.01f by default, and `maxFreq` is the maximum token frequency).
 * If `maxFreq` is higher than 1, then QUANT is always higher than 2
 * (which means that tokens with frequency 1 are always discarded).
 *  * tokens, which frequency after quantization falls below QUANT, are
 * discarded.
 *  * create a list of tokens and their quantized frequency, separated by
 * spaces, in the order of decreasing frequency.
 *
 * This list is then submitted to an MD5 hash calculation.
 *
 */
class TextProfileSignature(conf: ImmutableConfig) : Signature() {

    private var MIN_TOKEN_LEN = 2
    private var QUANT_RATE = 0.01f

    private val fallback = MD5Signature()

    init {
        MIN_TOKEN_LEN = conf.getInt("db.signature.text_profile.min_token_len", 2)
        QUANT_RATE = conf.getFloat("db.signature.text_profile.quant_rate", 0.01f)
    }

    override fun calculate(page: WebPage): ByteArray {
        val tokens = HashMap<String, Token>()
        // String text = page.getPageText();
        var text = page.contentText
        if (text.isNullOrEmpty() || text.length < GOOD_CONTENT_TEXT_LENGTH) {
            text = page.pageText
        }

        if (text.isNullOrEmpty()) {
            return fallback.calculate(page)
        }

        val curToken = StringBuilder()
        var maxFreq = 0
        for (element in text) {
            val c = element
            if (Character.isLetterOrDigit(c)) {
                curToken.append(Character.toLowerCase(c))
            } else {
                if (curToken.isNotEmpty()) {
                    if (curToken.length > MIN_TOKEN_LEN) {
                        // add it
                        val s = curToken.toString()
                        var tok: Token? = tokens[s]
                        if (tok == null) {
                            tok = Token(0, s)
                            tokens[s] = tok
                        }
                        tok.cnt++
                        if (tok.cnt > maxFreq)
                            maxFreq = tok.cnt
                    }
                    curToken.setLength(0)
                }
            }
        }

        // check the last token
        if (curToken.length > MIN_TOKEN_LEN) {
            // add it
            val s = curToken.toString()
            var tok: Token? = tokens[s]
            if (tok == null) {
                tok = Token(0, s)
                tokens[s] = tok
            }
            tok.cnt++
            if (tok.cnt > maxFreq)
                maxFreq = tok.cnt
        }
        var it = tokens.values.iterator()
        val profile = ArrayList<Token>()
        // calculate the QUANT value
        var QUANT = Math.round(maxFreq * QUANT_RATE)
        if (QUANT < 2) {
            if (maxFreq > 1)
                QUANT = 2
            else
                QUANT = 1
        }
        while (it.hasNext()) {
            val t = it.next()
            // round down to the nearest QUANT
            t.cnt = t.cnt / QUANT * QUANT
            // discard the frequencies below the QUANT
            if (t.cnt < QUANT) {
                continue
            }
            profile.add(t)
        }

        profile.sortWith(TokenComparator())
        val newText = StringBuilder()
        it = profile.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (newText.length > 0)
                newText.append("\n")
            newText.append(t.toString())
        }
        return MD5Hash.digest(newText.toString()).digest
    }

    private class Token(var cnt: Int, var val_: String) {

        override fun toString(): String {
            return "$val_ $cnt"
        }
    }

    private class TokenComparator : Comparator<Token> {
        override fun compare(t1: Token, t2: Token): Int {
            return t2.cnt - t1.cnt
        }
    }

    companion object {
        var GOOD_CONTENT_TEXT_LENGTH = 2000
    }
}
