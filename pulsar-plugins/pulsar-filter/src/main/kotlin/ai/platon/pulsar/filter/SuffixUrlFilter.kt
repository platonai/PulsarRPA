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
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.SuffixStringMatcher
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.filter.UrlFilter
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Filters URLs based on a file of URL suffixes. The file is named by
 *
 *  1. property "urlfilter.suffix.file" in ./conf/pulsar-default.xml, and
 *  1. attribute "file" in plugin.xml of this plugin
 *
 * Attribute "file" has higher precedence if defined. If the config file is
 * missing, all URLs will be rejected.
 *
 *
 *
 *
 * This filter can be configured to work in one of two modes:
 *
 *  * **default to reject** ('-'): in this mode, only URLs that match
 * suffixes specified in the config file will be accepted, all other URLs will
 * be rejected.
 *  * **default to accept** ('+'): in this mode, only URLs that match
 * suffixes specified in the config file will be rejected, all other URLs will
 * be accepted.
 *
 *
 *
 * The format of this config file is one URL suffix per line, with no preceding
 * whitespace. Order, in which suffixes are specified, doesn't matter. Blank
 * lines and comments (#) are allowed.
 *
 *
 *
 * A single '+' or '-' sign not followed by any suffix must be used once, to
 * signify the mode this plugin operates in. An optional single 'I' can be
 * appended, to signify that suffix matches should be case-insensitive. The
 * default, if not specified, is to use case-sensitive matches, i.e. suffix
 * '.JPG' does not match '.jpg'.
 *
 *
 *
 * NOTE: the format of this file is different from urlfilter-prefix, because
 * that plugin doesn't support allowed/prohibited prefixes (only supports
 * allowed prefixes). Please note that this plugin does not support regular
 * expressions, it only accepts literal suffixes. I.e. a suffix "+*.jpg" is most
 * probably wrong, you should use "+.jpg" instead.
 *
 * <h4>Example 1</h4>
 *
 *
 * The configuration shown below will accept all URLs with '.html' or '.htm'
 * suffixes (case-sensitive - '.HTML' or '.HTM' will be rejected), and prohibit
 * all other suffixes.
 *
 *
 *
 *
 * <pre>
 * # this is a comment
 *
 * # prohibit all unknown, case-sensitive matching
 * -
 *
 * # collect only HTML files.
 * .html
 * .htm
</pre> *
 *
 *
 *
 * <h4>Example 2</h4>
 *
 *
 * The configuration shown below will accept all URLs except common graphical
 * formats.
 *
 *
 *
 *
 * <pre>
 * # this is a comment
 *
 * # allow all unknown, case-insensitive matching
 * +I
 *
 * # prohibited suffixes
 * .gif
 * .png
 * .jpg
 * .jpeg
 * .bmp
</pre> *
 *
 *
 *
 *
 * @author Andrzej Bialecki
 */
class SuffixUrlFilter(lines: List<String>, conf: ImmutableConfig) : UrlFilter {
    private lateinit var suffixes: SuffixStringMatcher
    var isModeAccept = false
    private var filterFromPath = false
    var isIgnoreCase = false

    init {
        parse(lines)
    }

    constructor(conf: ImmutableConfig): this(load(conf), conf)

    override fun filter(url: String): String? {
        var u = if (isIgnoreCase) url.toLowerCase() else url
        if (filterFromPath) {
            try {
                val pUrl = URL(u)
                u = pUrl.path
            } catch (e: MalformedURLException) { // don't care
            }
        }
        val a = suffixes.shortestMatch(u)
        return if (a == null) {
            if (isModeAccept) url else null
        } else {
            if (isModeAccept) null else url
        }
    }

    @Throws(IOException::class)
    fun parse(lines: List<String>) { // handle missing config file
        if (lines.isEmpty()) {
            LOG.warn("Missing urlfilter.suffix.file, all URLs will be rejected!")
            suffixes = SuffixStringMatcher(arrayOfNulls(0))
            isModeAccept = false
            isIgnoreCase = false
            return
        }

        val aSuffixes: MutableList<String> = ArrayList()
        var allow = false
        var ignore = false
        for (line in lines) {
            when (line[0]) {
                ' ', '\n', '#' -> {
                }
                '-' -> {
                    allow = false
                    if (line.contains("P")) filterFromPath = true
                    if (line.contains("I")) ignore = true
                }
                '+' -> {
                    allow = true
                    if (line.contains("P")) filterFromPath = true
                    if (line.contains("I")) ignore = true
                }
                else -> aSuffixes.add(line)
            }
        }

        if (ignore) {
            for (i in aSuffixes.indices) {
                aSuffixes[i] = aSuffixes[i].toLowerCase()
            }
        }

        suffixes = SuffixStringMatcher(aSuffixes)
        isModeAccept = allow
        isIgnoreCase = ignore
    }

    fun setFilterFromPath(filterFromPath: Boolean) {
        this.filterFromPath = filterFromPath
    }

    companion object {
        const val PARAM_URLFILTER_SUFFIX_FILE = "urlfilter.suffix.file"
        const val PARAM_URLFILTER_SUFFIX_RULES = "urlfilter.suffix.rules"
        private val LOG = LoggerFactory.getLogger(SuffixUrlFilter::class.java)

        private fun load(conf: ImmutableConfig): List<String> {
            val stringResource = conf[PARAM_URLFILTER_SUFFIX_RULES]
            val fileResource = conf[PARAM_URLFILTER_SUFFIX_FILE, "suffix-urlfilter.txt"]
            val resourcePrefix = conf[CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, ""]
            return ResourceLoader.readAllLines(stringResource, fileResource, resourcePrefix)
        }

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val conf = ImmutableConfig()
            val filter = if (args.isNotEmpty()) {
                SuffixUrlFilter(ResourceLoader.readAllLines(args[0]), conf)
            } else {
                SuffixUrlFilter(conf)
            }

            val `in` = BufferedReader(InputStreamReader(System.`in`))
            var line: String
            while (`in`.readLine().also { line = it } != null) {
                val out = filter.filter(line)
                if (out != null) {
                    println("ACCEPTED $out")
                } else {
                    println("REJECTED $out")
                }
            }
        }
    }
}
