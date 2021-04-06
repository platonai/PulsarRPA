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

import ai.platon.pulsar.common.AppPaths.PATH_BANNED_URLS
import ai.platon.pulsar.common.AppPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.FSUtils
import ai.platon.pulsar.common.LocalFSUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.filter.CrawlUrlFilter
import java.util.*

/**
 * Filters URLs based on a file of regular expressions using the
 * [Java Regex implementation][java.util.regex].
 */
class BannedUrlFilter(val conf: ImmutableConfig) : CrawlUrlFilter {
    private val bannedUrls: MutableSet<String> = HashSet()
    private val unreachableHosts: MutableSet<String> = HashSet()

    init {
        bannedUrls.addAll(FSUtils.readAllLinesSilent(PATH_BANNED_URLS, conf))
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_UNREACHABLE_HOSTS))
    }

    override fun filter(url: String): String? {
        return if (bannedUrls.contains(url) || unreachableHosts.contains(url)) null else url
    }

    companion object {
        const val URLFILTER_DATE_FILE = "urlfilter.date.file"
        const val URLFILTER_DATE_RULES = "urlfilter.date.rules"
    }
}