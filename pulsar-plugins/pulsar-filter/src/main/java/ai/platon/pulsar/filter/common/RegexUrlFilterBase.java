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
package ai.platon.pulsar.filter.common;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.filter.UrlFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR;

/**
 * Generic {@link UrlFilter URL filter} based on regular
 * expressions.
 * <p>
 * <p>
 * The regular expressions rules are expressed in a file.
 * </p>
 * <p>
 * <p>
 * The format of this file is made of many rules (one per line):
 * <code>
 * [+-]&lt;regex&gt;
 * </code>
 * where plus (<code>+</code>)means go ahead and index it and minus (
 * <code>-</code>)means no.
 * </p>
 */
public abstract class RegexUrlFilterBase implements UrlFilter {

    protected String resourcePrefix = "";

    /**
     * Applicable rules
     */
    protected List<RegexRule> rules = Collections.emptyList();

    public RegexUrlFilterBase(ImmutableConfig conf) {
        try {
            resourcePrefix = conf.get(PULSAR_CONFIG_PREFERRED_DIR, "");
            rules = readRules(getRulesReader(conf));
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Constructs a new RegexUrlFilter and init it with a Reader of rules.
     *
     * @param reader is a reader of rules.
     */
    protected RegexUrlFilterBase(Reader reader) throws IllegalArgumentException, IOException {
        rules = readRules(reader);
    }

    /**
     * Filter the standard input using a RegexUrlFilterBase.
     *
     * @param filter is the RegexUrlFilterBase to use for filtering the standard input.
     * @param args   some optional parameters (not used).
     */
    public static void main(RegexUrlFilterBase filter, String args[]) throws IOException, IllegalArgumentException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            String out = filter.filter(line);
            if (out != null) {
                System.out.print("+");
                System.out.println(out);
            } else {
                System.out.print("-");
                System.out.println(line);
            }
        }
    }

    /**
     * Creates a new {@link RegexRule}.
     *
     * @param sign  of the regular expression. A <code>true</code> value means that
     *              any URL matching this rule must be included, whereas a
     *              <code>false</code> value means that any URL matching this rule
     *              must be excluded.
     * @param regex is the regular expression associated to this rule.
     */
    protected abstract RegexRule createRule(boolean sign, String regex);

    /**
     * Returns the name of the file of rules to use for a particular
     * implementation.
     *
     * @param conf is the current configuration.
     * @return the name of the resource containing the rules to use.
     */
    protected abstract Reader getRulesReader(ImmutableConfig conf) throws IOException;

    public String filter(String url) {
        if (url == null) return null;

        for (RegexRule rule : rules) {
            if (rule.match(url)) {
                return rule.accept() ? url : null;
            }
        }

        return null;
    }

    /**
     * Read the specified file of rules.
     *
     * @param reader is a reader of regular expressions rules.
     * @return the corresponding {@RegexRule rules}.
     */
    private List<RegexRule> readRules(Reader reader) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        List<RegexRule> rules = new ArrayList<>();
        String line;

        while ((line = in.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            char first = line.charAt(0);
            boolean sign;
            switch (first) {
                case '+':
                    sign = true;
                    break;
                case '-':
                    sign = false;
                    break;
                case ' ':
                case '\n':
                case '#': // skip blank & comment lines
                    continue;
                default:
                    throw new IOException("Invalid first character: " + line);
            }

            String regex = line.substring(1);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding rule [" + regex + "]");
            }
            RegexRule rule = createRule(sign, regex);
            rules.add(rule);
        }

        return rules;
    }
}
