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
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.filter.UrlNormalizer
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.Text
import org.xml.sax.InputSource
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.util.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Allows users to do regex substitutions on all/any URLs that are encountered,
 * which is useful for stripping session IDs from URLs.
 *
 * This class uses the <tt>urlnormalizer.regex.file</tt> property. It should be
 * set to the file name of an xml file which should contain the patterns and
 * substitutions to be done on encountered URLs.
 *
 * This class also supports different rules depending on the scope. Please see
 * the javadoc in [UrlNormalizers] for more details.
 *
 * @author Luke Baker
 * @author Andrzej Bialecki
 */
class RegexUrlNormalizer(private val conf: ImmutableConfig) : UrlNormalizer {
    private val scopedRulesThreadLocal = object : ThreadLocal<HashMap<String, List<Rule>>>() {
        override fun initialValue(): HashMap<String, List<Rule>> {
            return HashMap()
        }
    }

    private val defaultRules: List<Rule>
    val scopedRules: HashMap<String, List<Rule>> get() = scopedRulesThreadLocal.get()

    @Throws(FileNotFoundException::class)
    protected fun getRulesReader(conf: ImmutableConfig): Reader {
        val stringResource = conf[URLNORMALIZER_REGEX_RULES]
        val fileResource = conf[URLNORMALIZER_REGEX_FILE, "regex-normalize.xml"]
        val resourcePrefix = conf[CapabilityTypes.LEGACY_CONFIG_PROFILE, ""]
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix)
                ?:throw FileNotFoundException("Resource not found $stringResource/$fileResource, prefix: $resourcePrefix")
    }

    // used in JUnit test.
    fun setConfiguration(reader: Reader, scope: String) {
        val rules = readConfiguration(reader)
        scopedRules[scope] = rules
        LOG.debug("Set config for scope '" + scope + "': " + rules.size + " rules.")
    }

    /**
     * This function does the replacements by iterating through all the regex
     * patterns. It accepts a string url as input and returns the altered string.
     */
    fun regexNormalize(urlString_: String, scope: String): String? {
        var urlString = urlString_
        var curRules = scopedRules[scope]
        if (curRules == null) { // try to populate
            val fileResource = conf["$URLNORMALIZER_REGEX_FILE.$scope"]
            if (fileResource != null) {
                val resourcePrefix = conf[CapabilityTypes.LEGACY_CONFIG_PROFILE, ""]
                LOG.debug("resource for scope '$scope': $fileResource")
                ResourceLoader.getResourceAsReader(fileResource, resourcePrefix).use {
                    reader -> curRules = readConfiguration(reader!!)
                }
            }
            if (curRules == null) {
                curRules = EMPTY_RULES
            }

            scopedRules[scope] = curRules!!
        }

        if (curRules!!.isEmpty()) {
            curRules = defaultRules
        }

        for (r in curRules!!) {
            val matcher = r.pattern!!.matcher(urlString)
            urlString = matcher.replaceAll(r.substitution)
        }

        return urlString
    }

    override fun normalize(url: String, scope: String): String? {
        return regexNormalize(url, scope)
    }

    private fun readConfiguration(reader: Reader): List<Rule> {
        val rules: MutableList<Rule> = ArrayList()
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(reader))
            val root = doc.documentElement
            if ("regex-normalize" != root.tagName) {
                LOG.error("bad conf file: top-level element not <regex-normalize>")
            }
            val regexes = root.childNodes
            for (i in 0 until regexes.length) {
                val regexNode = regexes.item(i) as? Element ?: continue
                val regex = regexNode
                if ("regex" != regex.tagName) {
                    LOG.warn("bad conf file: element not <regex>")
                }
                val fields = regex.childNodes
                var patternValue: String? = null
                var subValue: String? = null
                for (j in 0 until fields.length) {
                    val fieldNode = fields.item(j) as? Element ?: continue
                    val field = fieldNode
                    if ("pattern" == field.tagName && field.hasChildNodes()) {
                        patternValue = (field.firstChild as Text).data
                    }
                    if ("substitution" == field.tagName && field.hasChildNodes()) {
                        subValue = (field.firstChild as Text).data
                    }
                    if (!field.hasChildNodes()) {
                        subValue = ""
                    }
                }
                if (patternValue != null && subValue != null) {
                    val rule = Rule()
                    try {
                        rule.pattern = Pattern.compile(patternValue)
                    } catch (e: PatternSyntaxException) {
                        LOG.error("skipped rule: $patternValue -> $subValue : invalid regular expression pattern: $e")
                        continue
                    }
                    rule.substitution = subValue
                    rules.add(rule)
                }
            }
        } catch (e: Exception) {
            LOG.error("error parsing conf file: $e")
            return EMPTY_RULES
        }
        return if (rules.size == 0) {
            EMPTY_RULES
        } else rules
    }

    /**
     * Class which holds a compiled pattern and its corresponding substition
     * string.
     */
    class Rule {
        var pattern: Pattern? = null
        var substitution: String? = null
    }

    companion object {
        const val URLNORMALIZER_REGEX_FILE = "urlnormalizer.regex.file"
        const val URLNORMALIZER_REGEX_RULES = "urlnormalizer.regex.rules"
        private val LOG = LoggerFactory.getLogger(RegexUrlNormalizer::class.java)
        private val EMPTY_RULES: List<Rule> = emptyList()
    }

    init {
        var rules = EMPTY_RULES
        try {
            getRulesReader(conf).use { reader -> rules = readConfiguration(reader) }
        } catch (e: IOException) {
            LOG.error(e.stringify())
        }
        defaultRules = rules
    }
}
