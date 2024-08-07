package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.filter.common.RegexRule
import ai.platon.pulsar.filter.common.AbstractRegexUrlFilter
import dk.brics.automaton.RegExp
import dk.brics.automaton.RunAutomaton
import java.io.IOException
import java.io.Reader


/**
 * RegexUrlFilterBase implementation based on the [dk.brics.automaton](http://www.brics.dk/automaton/) Finite-State
 * Automata for Java<sup>TM</sup>.
 *
 * @author Jrme Charron
 * @see [dk.brics.automaton](http://www.brics.dk/automaton/)
 */
class AutomatonUrlFilter(reader: Reader?, conf: ImmutableConfig) : AbstractRegexUrlFilter(reader, conf) {

    constructor(conf: ImmutableConfig): this(null, conf)

    /**
     * Rules specified as a config property will override rules specified as a
     * config file.
     */
    @Throws(IOException::class)
    override fun getRulesReader(conf: ImmutableConfig): Reader {
        val stringResource = conf[URLFILTER_AUTOMATON_RULES]
        val fileResource = conf[URLFILTER_AUTOMATON_FILE, "automaton-urlfilter.txt"]
        val resourcePrefix = conf[CapabilityTypes.LEGACY_CONFIG_PROFILE, ""]
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix)
                ?:throw IOException("Failed to find resource $stringResource or $fileResource with prefix $resourcePrefix")
    }

    // Inherited Javadoc
    override fun createRule(sign: Boolean, regex: String): RegexRule {
        return Rule(sign, regex)
    }

    private inner class Rule internal constructor(sign: Boolean, regex: String) : RegexRule(sign, regex) {
        private val automaton = RunAutomaton(RegExp(regex, RegExp.ALL).toAutomaton())
        override fun match(url: String): Boolean {
            return automaton.run(url)
        }

    }

    companion object {
        const val URLFILTER_AUTOMATON_FILE = "urlfilter.automaton.file"
        const val URLFILTER_AUTOMATON_RULES = "urlfilter.automaton.rules"
    }
}
