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
package ai.platon.pulsar.crawl.parse

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.html.ParseContext
import org.slf4j.LoggerFactory

/**
 * Creates and caches [ParseFilter] implementing plugins.
 */
class ParseFilters(val parseFilters: List<ParseFilter>, val conf: ImmutableConfig) {
    private val log = LoggerFactory.getLogger(ParseFilters::class.java)

    /**
     * Run all defined filters.
     */
    fun filter(parseContext: ParseContext) {
        // loop on each filter
        parseFilters.forEach {
            val shouldContinue = filterSilence(it, parseContext)
            if (!shouldContinue) {
                return@forEach
            }
        }
    }

    private fun filterSilence(parseFilter: ParseFilter, parseContext: ParseContext): Boolean {
        try {
            parseFilter.filter(parseContext)
        } catch (e: Throwable) {
            log.warn(StringUtil.stringifyException(e))
        }

        return parseContext.parseResult.shouldContinue
    }

    override fun toString(): String {
        return parseFilters.joinToString { it.javaClass.simpleName }
    }
}
