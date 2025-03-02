
// $Id: PrefixUrlFilter.java 823614 2009-10-09 17:02:32Z ab $
package ai.platon.pulsar.filter

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.filter.CrawlUrlFilter
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*

/**
 * Filters URLs based on a file of URL prefixes. The file is named by (1)
 * property "urlfilter.prefix.file" in ./config/pulsar-default.xml, and (2)
 * attribute "file" in plugin.xml of this plugin Attribute "file" has higher
 * precedence if defined.
 *
 *
 *
 *
 * The format of this file is one URL prefix per line.
 *
 */
class PrefixUrlFilter(conf: ImmutableConfig) : CrawlUrlFilter {
    private var trie: TrieStringMatcher? = null

    init {
        try {
            getRulesReader(conf).use { reader -> trie = readConfiguration(reader) }
        } catch (e: IOException) {
            LOG.error("IOException", e)
            throw RuntimeException(e.message, e)
        }
    }

    @Throws(IOException::class)
    fun reload(stringResource: String) {
        trie = readConfiguration(StringReader(stringResource))
    }

    override fun filter(url: String): String? {
        return if (trie!!.shortestMatch(url) == null) null else url
    }

    @Throws(IOException::class)
    private fun readConfiguration(reader: Reader): TrieStringMatcher {
        val br = BufferedReader(reader)
        val urlPrefixes: MutableList<String> = ArrayList()
        var line: String?
        loop@ while (br.readLine().also { line = it } != null) {
            val l = line?:continue
            if (l.isEmpty()) continue

            when (l[0]) {
                ' ', '\n', '#' -> continue@loop
                else -> urlPrefixes.add(l)
            }
        }

        return PrefixStringMatcher(urlPrefixes)
    }

    @Throws(FileNotFoundException::class)
    protected fun getRulesReader(conf: ImmutableConfig): Reader {
        val stringResource = conf[URLFILTER_PREFIX_RULES]
        val fileResource = conf[URLFILTER_PREFIX_FILE, "prefix-urlfilter.txt"]
        val resourcePrefix = conf[CapabilityTypes.PROFILE_KEY, ""]
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix)
                ?:throw FileNotFoundException("Resource not found $stringResource/$fileResource, prefix: $resourcePrefix")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PrefixUrlFilter::class.java)
        var URLFILTER_PREFIX_RULES = "urlfilter.prefix.rules"
        var URLFILTER_PREFIX_FILE = "urlfilter.prefix.file"
    }
}
