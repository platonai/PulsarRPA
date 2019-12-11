package ai.platon.pulsar.filter

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.filter.common.RegexRule
import ai.platon.pulsar.filter.common.RegexUrlFilterBase
import dk.brics.automaton.RegExp
import dk.brics.automaton.RunAutomaton
import java.io.IOException
import java.io.Reader

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
/**
 * RegexUrlFilterBase implementation based on the [dk.brics.automaton](http://www.brics.dk/automaton/) Finite-State
 * Automata for Java<sup>TM</sup>.
 *
 * @author Jrme Charron
 * @see [dk.brics.automaton](http://www.brics.dk/automaton/)
 */
class AutomatonUrlFilter(reader: Reader?, conf: ImmutableConfig) : RegexUrlFilterBase(reader, conf) {

    constructor(conf: ImmutableConfig): this(null, conf)

    /**
     * Rules specified as a config property will override rules specified as a
     * config file.
     */
    @Throws(IOException::class)
    override fun getRulesReader(conf: ImmutableConfig): Reader {
        val stringResource = conf[URLFILTER_AUTOMATON_RULES]
        val fileResource = conf[URLFILTER_AUTOMATON_FILE, "automaton-urlfilter.txt"]
        val resourcePrefix = conf[CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, ""]
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix)
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