/**
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

// JDK imports
package org.warps.pulsar.filter;

import org.warps.pulsar.common.ResourceLoader;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.MutableConfig;
import org.warps.pulsar.filter.common.RegexRule;
import org.warps.pulsar.filter.common.RegexUrlFilterBase;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import static org.warps.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_RESOURCE_PREFIX;

/**
 * Filters URLs based on a file of regular expressions using the
 * {@link java.util.regex Java Regex implementation}.
 */
public class RegexUrlFilter extends RegexUrlFilterBase {

    public static final String URLFILTER_REGEX_FILE = "urlfilter.regex.file";
    public static final String URLFILTER_REGEX_RULES = "urlfilter.regex.rules";

    public RegexUrlFilter(ImmutableConfig conf) {
        super(conf);
    }

    public RegexUrlFilter(Reader reader) throws IOException, IllegalArgumentException {
        super(reader);
    }

    /*
     * ------------------------------------ * </implementation:RegexUrlFilterBase>
     * * ------------------------------------
     */
    public static void main(String args[]) throws IOException {
        MutableConfig conf = new MutableConfig();
        RegexUrlFilter filter = new RegexUrlFilter(conf);
        conf.set(URLFILTER_REGEX_RULES, "+^http://sh.lianjia.com/ershoufang/pg(.*)$\n+^http://sh.lianjia.com/ershoufang/SH(.+)/{0,1}$\n-.+\n ");
        main(filter, args);
    }

    /**
     * Rules specified as a config property will override rules specified as a
     * config file.
     */
    protected Reader getRulesReader(ImmutableConfig conf) throws IOException {
        String stringResource = conf.get(URLFILTER_REGEX_RULES);
        String fileResource = conf.get(URLFILTER_REGEX_FILE, "regex-urlfilter.txt");
        String resourcePrefix = conf.get(PULSAR_CONFIG_RESOURCE_PREFIX, "");
        return new ResourceLoader().getReader(stringResource, fileResource, resourcePrefix);
    }

    // Inherited Javadoc
    protected RegexRule createRule(boolean sign, String regex) {
        return new RegexRuleImpl(sign, regex);
    }

    private class RegexRuleImpl extends RegexRule {
        private Pattern pattern;

        RegexRuleImpl(boolean sign, String regex) {
            super(sign, regex);
            pattern = Pattern.compile(regex);
        }

        protected boolean match(String url) {
            if (url == null) {
                LOG.error("Null url passed into to RegexRuleFilter");
                return false;
            }

            return pattern.matcher(url).find();
        }
    }
}
