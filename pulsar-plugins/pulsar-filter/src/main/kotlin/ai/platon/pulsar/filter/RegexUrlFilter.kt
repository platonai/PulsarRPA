

package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.filter.common.RegexRule
import ai.platon.pulsar.filter.common.AbstractRegexUrlFilter
import java.io.FileNotFoundException
import java.io.Reader
import java.util.regex.Pattern

/**
 * Filters URLs based on a file of regular expressions using the
 * [Java Regex implementation][java.util.regex].
 */
class RegexUrlFilter(
        reader: Reader?,
        conf: ImmutableConfig
) : AbstractRegexUrlFilter(reader, conf) {

    constructor(conf: ImmutableConfig): this(null, conf)

    /**
     * Rules specified as a config property will override rules specified as a
     * config file.
     */
    @Throws(FileNotFoundException::class)
    override fun getRulesReader(conf: ImmutableConfig): Reader {
        val stringResource = conf[URLFILTER_REGEX_RULES]
        val fileResource = conf[URLFILTER_REGEX_FILE, "regex-urlfilter.txt"]
        val resourcePrefix = conf[CapabilityTypes.PROFILE_KEY, ""]
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix)
                ?:throw FileNotFoundException("Resource not found $stringResource/$fileResource, prefix: $resourcePrefix")
    }

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
    }
}
