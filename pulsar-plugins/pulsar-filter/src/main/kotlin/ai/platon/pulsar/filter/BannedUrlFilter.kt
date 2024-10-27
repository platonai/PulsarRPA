

package ai.platon.pulsar.filter

import ai.platon.pulsar.common.AppPaths.PATH_BANNED_URLS
import ai.platon.pulsar.common.AppPaths.PATH_UNREACHABLE_HOSTS
import ai.platon.pulsar.common.FSUtils
import ai.platon.pulsar.common.LocalFSUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilter
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