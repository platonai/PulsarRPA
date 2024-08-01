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

import ai.platon.pulsar.common.DateTimeDetector
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilter
import java.time.ZoneId
import java.util.*

/**
 * Filters URLs based on a file of regular expressions using the
 * [Java Regex implementation][java.util.regex].
 */
class DateUrlFilter(val conf: ImmutableConfig) : CrawlUrlFilter {
    private val configFile: String? = null
    private val rules: Set<String> = LinkedHashSet()
    private val detector = DateTimeDetector()
    var oldDays = conf.getInt(CapabilityTypes.RECENT_DAYS_WINDOW, 7)

    constructor(zoneId: ZoneId, conf: ImmutableConfig): this(conf) {
        detector.zoneId = zoneId
    }

    /**
     * TODO : Not implemented yet
     */
    private fun load() {
        val resourceFile = configFile ?: conf.get(URLFILTER_DATE_FILE, "date-urlfilter.txt")
        val stringResource = conf.get(URLFILTER_DATE_RULES)
        ResourceLoader.readAllLines(stringResource, resourceFile)
    }

    override fun filter(url: String): String? {
        // TODO : The timezone is the where the article published
        return if (detector.containsOldDate(url, oldDays, ZoneId.systemDefault())) null else url
    }

    companion object {
        const val URLFILTER_DATE_FILE = "urlfilter.date.file"
        const val URLFILTER_DATE_RULES = "urlfilter.date.rules"
    }
}