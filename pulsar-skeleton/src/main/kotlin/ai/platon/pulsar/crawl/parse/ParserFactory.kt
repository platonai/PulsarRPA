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

import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.parse.html.PrimerHtmlParser
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates [Parser].
 */
class ParserFactory(private val conf: ImmutableConfig) {
    // Thread safe for both outer map and inner list
    private val mineType2Parsers = ConcurrentHashMap<String, List<Parser>>()

    constructor(
        availableParsers: List<Parser>, conf: ImmutableConfig
    ) : this(ParserConfigReader().parse(conf), availableParsers, conf)

    constructor(
        parserConfig: ParserConfig,
        availableParsers: List<Parser>,
        conf: ImmutableConfig
    ): this(conf) {
        val availableNamedParsers = availableParsers.associateBy { it.javaClass.name }
        parserConfig.parsers.forEach { (mimeType: String, parserClasses: List<String>) ->
            val parsers = parserClasses.mapNotNull { name -> availableNamedParsers[name] }
            mineType2Parsers[mimeType] = Collections.synchronizedList(parsers)
        }

        mineType2Parsers.keys.associateWith { mineType2Parsers[it]?.joinToString { it.javaClass.name } }
                .let { Params(it) }.withLogger(LOG).info("Active parsers: ", "", false)
    }

    constructor(parses: Map<String, List<Parser>>, conf: ImmutableConfig): this(conf) {
        mineType2Parsers.putAll(parses)
    }

    init {
        if (mineType2Parsers.isEmpty()) {
            val htmlParsers = listOf(PrimerHtmlParser(conf))
            listOf("text/html", "application/xhtml+xml").forEach {
                mineType2Parsers[it] = htmlParsers
            }
        }
    }

    /**
     * Function returns an array of [Parser]s for a given content type.
     *
     * The function consults the internal list of parse plugins for the
     * ParserFactory to determine the list of pluginIds, then gets the appropriate
     * extension points to instantiate as [Parser]s.
     *
     * The function is thread safe
     *
     * @param contentType The contentType to return the `Array` of [Parser]'s for.
     * @param url         The url for the content that may allow us to get the type from the file suffix.
     * @return An `List` of [Parser]s for the given contentType.
     */
    @Throws(ParserNotFound::class)
    fun getParsers(contentType: String, url: String = ""): List<Parser> {
        val mimeType = MimeTypeResolver.cleanMimeType(contentType)
        return mineType2Parsers[mimeType]?: mineType2Parsers[DEFAULT_MINE_TYPE] ?: listOf()
    }

    private fun escapeContentType(contentType: String): String {
        // Escapes contentType in order to use as a regex (and keep backwards compatibility).
        // This enables to accept multiple types for a single parser.
        return contentType.replace("+", "\\+").replace(".", "\\.")
    }

    override fun toString(): String {
        return mineType2Parsers.values.joinToString { it.javaClass.simpleName }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(ParserFactory::class.java)
        const val DEFAULT_MINE_TYPE = "*"
    }
}
