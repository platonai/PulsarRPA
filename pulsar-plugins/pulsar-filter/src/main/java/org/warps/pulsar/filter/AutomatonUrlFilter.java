package org.warps.pulsar.filter; /**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import org.warps.pulsar.common.ResourceLoader;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.filter.common.RegexRule;
import org.warps.pulsar.filter.common.RegexUrlFilterBase;

import java.io.IOException;
import java.io.Reader;

import static org.warps.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_RESOURCE_PREFIX;

/**
 * RegexUrlFilterBase implementation based on the <a
 * href="http://www.brics.dk/automaton/">dk.brics.automaton</a> Finite-State
 * Automata for Java<sup>TM</sup>.
 *
 * @author J&eacute;r&ocirc;me Charron
 * @see <a href="http://www.brics.dk/automaton/">dk.brics.automaton</a>
 */
public class AutomatonUrlFilter extends RegexUrlFilterBase {
    public static final String URLFILTER_AUTOMATON_FILE = "urlfilter.automaton.file";
    public static final String URLFILTER_AUTOMATON_RULES = "urlfilter.automaton.rules";

    public AutomatonUrlFilter(ImmutableConfig conf) {
        super(conf);
    }

    public AutomatonUrlFilter(Reader reader) throws IOException, IllegalArgumentException {
        super(reader);
    }

    /**
     * Rules specified as a config property will override rules specified as a
     * config file.
     */
    @Override
    protected Reader getRulesReader(ImmutableConfig conf) throws IOException {
        String stringResource = conf.get(URLFILTER_AUTOMATON_RULES);
        String fileResource = conf.get(URLFILTER_AUTOMATON_FILE, "automaton-urlfilter.txt");
        String resourcePrefix = conf.get(PULSAR_CONFIG_RESOURCE_PREFIX, "");
        return new ResourceLoader().getReader(stringResource, fileResource, resourcePrefix);
    }

    // Inherited Javadoc
    protected RegexRule createRule(boolean sign, String regex) {
        return new Rule(sign, regex);
    }

    private class Rule extends RegexRule {

        private RunAutomaton automaton;

        Rule(boolean sign, String regex) {
            super(sign, regex);
            automaton = new RunAutomaton(new RegExp(regex, RegExp.ALL).toAutomaton());
        }

        protected boolean match(String url) {
            return automaton.run(url);
        }
    }
}
