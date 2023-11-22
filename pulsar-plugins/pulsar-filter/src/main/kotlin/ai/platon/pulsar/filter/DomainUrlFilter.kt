/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.domain.DomainSuffixes
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.filter.CrawlUrlFilter
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * Filters URLs based on a file containing domain suffixes, domain names, and
 * hostnames. Only a url that matches one of the suffixes, domains, or hosts
 * present in the file is allowed.
 *
 * Urls are checked in order of domain suffix, domain name, and hostname against
 * entries in the domain file. The domain file would be setup as follows with
 * one entry per line:
 *
 * <pre>
 * com apache.org www.apache.org
    </pre> *
 *
 * The first line is an example of a filter that would allow all .com domains.
 * The second line allows all urls from apache.org and all of its subdomains
 * such as lucene.apache.org and hadoop.apache.org. The third line would allow
 * only urls from www.apache.org. There is no specific ordering to entries. The
 * entries are from more general to more specific with the more general
 * overridding the more specific.
 *
 * The domain file defaults to domain-urlfilter.txt in the classpath but can be
 * overridden using the:
 *
 * property "urlfilter.domain.file" in ./config/pulsar-*.xml, and * attribute "file" in plugin.xml of this plugin
 */
class DomainUrlFilter(conf: ImmutableConfig) : CrawlUrlFilter {
    val domainSet: MutableSet<String> = LinkedHashSet()
    val tlds: DomainSuffixes = DomainSuffixes.getInstance()

    init {
        val stringResource = conf[PARAM_URLFILTER_DOMAIN_RULES]
        val resourcePrefix = conf[CapabilityTypes.LEGACY_CONFIG_PROFILE, ""]
        val fileResource = conf[PARAM_URLFILTER_DOMAIN_FILE, "domain-urlfilter.txt"]
        ResourceLoader.readAllLines(stringResource, fileResource, resourcePrefix).toCollection(domainSet)
        LOG.info(domainSet.joinToString(", ", "Allowed domains: "))
    }

    override fun filter(url: String): String? {
        try {
            // match for suffix, domain, and host in that order. more general will
            // override more specific
            var domain = URLUtil.getDomainName(url) ?: return null
            domain = domain.lowercase(Locale.getDefault()).trim { it <= ' ' }
            val host = URLUtil.getHostName(url)
            var suffix: String? = null
            val domainSuffix = URLUtil.getDomainSuffix(tlds, url)
            if (domainSuffix != null) {
                suffix = domainSuffix.domain
            }
            if (domainSet.contains(suffix) || domainSet.contains(domain) || domainSet.contains(host)) {
                return url
            }
        } catch (e: Exception) {
            LOG.error("Could not apply filter on url: " + url + "\n" + e.stringify())
        }
        return null
    }

    companion object {
        const val PARAM_URLFILTER_DOMAIN_RULES = "urlfilter.domain.rules"
        const val PARAM_URLFILTER_DOMAIN_FILE = "urlfilter.domain.file"
        private val LOG = LoggerFactory.getLogger(DomainUrlFilter::class.java)
    }
}
