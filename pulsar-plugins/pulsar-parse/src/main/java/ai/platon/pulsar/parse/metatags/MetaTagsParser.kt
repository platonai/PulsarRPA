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
package ai.platon.pulsar.parse.metatags

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.parse.ParseFilter
import ai.platon.pulsar.crawl.parse.html.ParseContext
import ai.platon.pulsar.persist.Metadata
import org.apache.commons.logging.LogFactory
import java.util.*
import kotlin.collections.HashSet

/**
 * ParseResult HTML meta tags (keywords, description) and store them in the parse
 * metadata so that they can be indexed with the index-metadata plugin with the
 * prefix 'metatag.'. Metatags are matched ignoring case.
 */
class MetaTagsParser(val conf: ImmutableConfig) : ParseFilter {
    private val metatagset: Set<String> = conf.getStrings(CapabilityTypes.METATAG_NAMES, "*")
            .mapTo(HashSet()) { it.toLowerCase() }

    override fun filter(parseContext: ParseContext) {
        val page = parseContext.page
        val metaTags = parseContext.metaTags
        val generalMetaTags = metaTags.generalTags
        for (tagName in generalMetaTags.names()) { // multiple values of a metadata field are separated by '\t' in persist.
            val sb = StringBuilder()
            for (value in generalMetaTags.getValues(tagName)) {
                if (sb.length > 0) {
                    sb.append("\t")
                }
                sb.append(value)
            }
            addIndexedMetatags(page.metadata, tagName, sb.toString())
        }
        val httpequiv = metaTags.httpEquivTags
        val tagNames = httpequiv.propertyNames()
        while (tagNames.hasMoreElements()) {
            val name = tagNames.nextElement() as String
            val value = httpequiv.getProperty(name)
            addIndexedMetatags(page.metadata, name, value)
        }
    }

    /**
     * Check whether the metatag is in the list of metatags to be indexed (or if
     * '*' is specified). If yes, add it to parse metadata.
     */
    private fun addIndexedMetatags(metadata: Metadata, metatag: String, value: String) {
        val lcMetatag = metatag.toLowerCase(Locale.ROOT)
        if (metatagset.contains("*") || metatagset.contains(lcMetatag)) { // log.trace("Found meta tag: " + lcMetatag + "\t" + value);
            metadata[PARSE_META_PREFIX + lcMetatag] = value
        }
    }

    companion object {
        const val PARSE_META_PREFIX = "meta_"
        private val LOG = LogFactory.getLog(MetaTagsParser::class.java.name)
    }
}