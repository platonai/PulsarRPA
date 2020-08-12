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
// JDK imports
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.filter.common.RegexRule
import ai.platon.pulsar.filter.common.RegexUrlFilterBase
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.util.regex.Pattern

/**
 * Filters URLs based on a file of regular expressions using the
 * [Java Regex implementation][java.util.regex].
 */
class RegexUrlFilter(
        reader: Reader?,
        conf: ImmutableConfig
) : RegexUrlFilterBase(reader, conf) {

    constructor(conf: ImmutableConfig): this(null, conf)

    /**
     * Rules specified as a config property will override rules specified as a
     * config file.
     */
    @Throws(FileNotFoundException::class)
    override fun getRulesReader(conf: ImmutableConfig): Reader {
        val stringResource = conf[URLFILTER_REGEX_RULES]
        val fileResource = conf[URLFILTER_REGEX_FILE, "regex-urlfilter.txt"]
        val resourcePrefix = conf[CapabilityTypes.LEGACY_CONFIG_PROFILE, ""]
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix)
                ?:throw FileNotFoundException("Resource not found $stringResource/$fileResource, prefix: $resourcePrefix")
    }

    // Inherited Javadoc
    override fun createRule(sign: Boolean, regex: String): RegexRule {
        return RegexRuleImpl(sign, regex)
    }

    private inner class RegexRuleImpl(sign: Boolean, regex: String) : RegexRule(sign, regex) {
        private val pattern = Pattern.compile(regex)
        override fun match(url: String): Boolean {
            return pattern.matcher(url).find()
        }

    }

    companion object {
        const val URLFILTER_REGEX_FILE = "urlfilter.regex.file"
        const val URLFILTER_REGEX_RULES = "urlfilter.regex.rules"
        /*
         * ------------------------------------
         * </implementation:RegexUrlFilterBase>
         * * ------------------------------------
         */
        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val conf = MutableConfig()
            val filter = RegexUrlFilter(conf)
            conf[URLFILTER_REGEX_RULES] = "+^http://sh.lianjia.com/ershoufang/pg(.*)$\n+^http://sh.lianjia.com/ershoufang/SH(.+)/{0,1}$\n-.+\n "
            main(filter, args)
        }
    }
}
