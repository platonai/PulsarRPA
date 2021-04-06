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
package ai.platon.pulsar.filter.common

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.filter.CrawlUrlFilter
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.util.*

/**
 * Generic [URL filter][CrawlUrlFilter] based on regular
 * expressions.
 *
 * The regular expressions rules are expressed in a file.
 *
 * The format of this file is made of many rules (one per line):
 * `
 * [+-]<regex>
` *
 * where plus (`+`)means go ahead and index it and minus (
 * `-`)means no.
 */
abstract class AbstractRegexUrlFilter(
        var reader: Reader?,
        val conf: ImmutableConfig
): CrawlUrlFilter {
    val LOG = LoggerFactory.getLogger(AbstractRegexUrlFilter::class.java)

    /**
     * Applicable rules
     */
    protected var rules: List<RegexRule> = listOf()

    /**
     * Constructs a new RegexUrlFilter and init it with a Reader of rules.
     *
     * @param reader is a reader of rules.
     */
    constructor(conf: ImmutableConfig): this(null, conf)

    init {
        if (reader == null) {
            reader = getRulesReader(conf)
        }

        if (reader != null) {
            try {
                rules = readRules(reader!!)
            } catch (e: Exception) {
                LOG.error(e.message)
                throw RuntimeException(e.message, e)
            }
        }
    }

    /**
     * Creates a new [RegexRule].
     *
     * @param sign  of the regular expression. A `true` value means that
     * any URL matching this rule must be included, whereas a
     * `false` value means that any URL matching this rule
     * must be excluded.
     * @param regex is the regular expression associated to this rule.
     */
    protected abstract fun createRule(sign: Boolean, regex: String): RegexRule

    /**
     * Returns the name of the file of rules to use for a particular
     * implementation.
     *
     * @param conf is the current configuration.
     * @return the name of the resource containing the rules to use.
     */
    @Throws(IOException::class)
    protected abstract fun getRulesReader(conf: ImmutableConfig): Reader

    override fun filter(url: String): String? {
        for (rule in rules) {
            if (rule.match(url)) {
                return if (rule.accept()) url else null
            }
        }
        return null
    }

    /**
     * Read the specified file of rules.
     *
     * @param reader is a reader of regular expressions rules.
     * @return the corresponding {@RegexRule rules}.
     */
    @Throws(IOException::class)
    private fun readRules(reader: Reader): List<RegexRule> {
        val br = BufferedReader(reader)
        val rules: MutableList<RegexRule> = ArrayList()
        var line: String?
        loop@ while (br.readLine().also { line = it } != null) {
            val l = line?:continue
            if (l.isEmpty()) continue

            val first = l[0]
            val sign = when (first) {
                '+' -> true
                '-' -> false
                ' ', '\n', '#' -> continue@loop
                else -> throw IOException("Invalid first character: $line")
            }
            val regex = l.substring(1)
            if (LOG.isTraceEnabled) {
                LOG.trace("Adding rule [$regex]")
            }
            val rule = createRule(sign, regex)
            rules.add(rule)
        }
        return rules
    }

    companion object {
        /**
         * Filter the standard input using a RegexUrlFilterBase.
         *
         * @param filter is the RegexUrlFilterBase to use for filtering the standard input.
         * @param args   some optional parameters (not used).
         */
        @JvmStatic
        @Throws(IOException::class, IllegalArgumentException::class)
        fun main(filter: AbstractRegexUrlFilter, args: Array<String>) {
            val reader = BufferedReader(InputStreamReader(System.`in`))
            var line: String
            while (reader.readLine().also { line = it } != null) {
                val out = filter.filter(line)
                if (out != null) {
                    print("+")
                    println(out)
                } else {
                    print("-")
                    println(line)
                }
            }
        }
    }
}
