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

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.html.ParseContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates and caches [ParseFilter] implementing plugins.
 */
class ParseFilters(initParseFilters: List<ParseFilter>, val conf: ImmutableConfig): AutoCloseable {
    private val log = LoggerFactory.getLogger(ParseFilters::class.java)

    private val parseFilters = initParseFilters.toMutableList()
    private val closed = AtomicBoolean()

    fun addFirst(parseFilter: ParseFilter) = parseFilters.add(0, parseFilter)

    fun addLast(parseFilter: ParseFilter) = parseFilters.add(parseFilter)

    /**
     * Run all defined filters.
     */
    fun filter(parseContext: ParseContext) {
        // loop on each filter
        parseFilters.forEach {
            log.trace("Applying parse filter {} {}", it.javaClass.simpleName, parseContext.page.url)

            kotlin.runCatching { it.filter(parseContext) }.onFailure { log.warn(Strings.simplifyException(it)) }

            val shouldContinue = parseContext.parseResult.shouldContinue
            if (!shouldContinue) {
                return
            }
        }
    }

    override fun toString(): String {
        return parseFilters.joinToString { it.javaClass.simpleName }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            parseFilters.forEach { it.runCatching { it.close() }.onFailure { log.warn(it.message) } }
        }
    }
}
