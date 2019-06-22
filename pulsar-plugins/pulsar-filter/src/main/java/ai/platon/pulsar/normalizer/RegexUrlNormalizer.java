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
package ai.platon.pulsar.normalizer;

import ai.platon.pulsar.common.ResourceLoader;
import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.filter.UrlNormalizer;
import ai.platon.pulsar.crawl.filter.UrlNormalizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR;

/**
 * Allows users to do regex substitutions on all/any URLs that are encountered,
 * which is useful for stripping session IDs from URLs.
 * <p>
 * <p>
 * This class uses the <tt>urlnormalizer.regex.file</tt> property. It should be
 * set to the file name of an xml file which should contain the patterns and
 * substitutions to be done on encountered URLs.
 * </p>
 * <p>
 * This class also supports different rules depending on the scope. Please see
 * the javadoc in {@link UrlNormalizers} for more details.
 * </p>
 *
 * @author Luke Baker
 * @author Andrzej Bialecki
 */
public class RegexUrlNormalizer implements UrlNormalizer {

    public static final String URLNORMALIZER_REGEX_FILE = "urlnormalizer.regex.file";
    public static final String URLNORMALIZER_REGEX_RULES = "urlnormalizer.regex.rules";
    private static final Logger LOG = LoggerFactory.getLogger(RegexUrlNormalizer.class);
    private static final List<Rule> EMPTY_RULES = Collections.emptyList();
    private ThreadLocal<HashMap<String, List<Rule>>> scopedRulesThreadLocal = new ThreadLocal<HashMap<String, List<Rule>>>() {
        protected java.util.HashMap<String, java.util.List<Rule>> initialValue() {
            return new HashMap<>();
        }
    };
    private ImmutableConfig conf;
    private List<Rule> defaultRules;

    public RegexUrlNormalizer(ImmutableConfig conf) {
        this.conf = conf;

        List<Rule> rules = EMPTY_RULES;
        try (Reader reader = getRulesReader(conf)) {
            rules = readConfiguration(reader);
        } catch (IOException e) {
            LOG.error(StringUtil.stringifyException(e));
        }

        defaultRules = rules;
    }

    public HashMap<String, List<Rule>> getScopedRules() {
        return scopedRulesThreadLocal.get();
    }

    protected Reader getRulesReader(ImmutableConfig conf) throws FileNotFoundException {
//    String stringResource = conf.get(URLNORMALIZER_REGEX_RULES);
//    if (stringResource != null) {
//      return new StringReader(stringResource);
//    }
//    String fileRules = conf.get(URLNORMALIZER_REGEX_FILE, "regex-normalize.xml");
//    return conf.getConfResourceAsReader(fileRules);

        String stringResource = conf.get(URLNORMALIZER_REGEX_RULES);
        String fileResource = conf.get(URLNORMALIZER_REGEX_FILE, "regex-normalize.xml");
        String resourcePrefix = conf.get(PULSAR_CONFIG_PREFERRED_DIR, "");
        return ResourceLoader.getMultiSourceReader(stringResource, fileResource, resourcePrefix);
    }

    // used in JUnit test.
    public void setConfiguration(Reader reader, String scope) {
        List<Rule> rules = readConfiguration(reader);
        getScopedRules().put(scope, rules);
        LOG.debug("Set config for scope '" + scope + "': " + rules.size() + " rules.");
    }

    /**
     * This function does the replacements by iterating through all the regex
     * patterns. It accepts a string url as input and returns the altered string.
     */
    public String regexNormalize(String urlString, String scope) {
        HashMap<String, List<Rule>> scopedRules = getScopedRules();
        List<Rule> curRules = scopedRules.get(scope);
        if (curRules == null) {
            // try to populate
            String fileResource = conf.get(URLNORMALIZER_REGEX_FILE + "." + scope);
            if (fileResource != null) {
                String resourcePrefix = conf.get(PULSAR_CONFIG_PREFERRED_DIR, "");
                LOG.debug("resource for scope '" + scope + "': " + fileResource);
                try (Reader reader = ResourceLoader.getResourceAsReader(fileResource, resourcePrefix)) {
                    curRules = readConfiguration(reader);
                } catch (Exception e) {
                    LOG.warn("Couldn't load resource '" + fileResource + "': " + e);
                }
            }

            if (curRules == null) {
                curRules = EMPTY_RULES;
            }
            scopedRules.put(scope, curRules);
        }

        if (curRules.isEmpty()) {
            curRules = defaultRules;
        }

        for (Rule r : curRules) {
            Matcher matcher = r.pattern.matcher(urlString);
            urlString = matcher.replaceAll(r.substitution);
        }

        return urlString;
    }

    @Override
    public String normalize(String urlString, String scope) {
        return regexNormalize(urlString, scope);
    }

    private List<Rule> readConfiguration(Reader reader) {
        List<Rule> rules = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
            Element root = doc.getDocumentElement();
            if (!"regex-normalize".equals(root.getTagName())) {
                LOG.error("bad conf file: top-level element not <regex-normalize>");
            }

            NodeList regexes = root.getChildNodes();
            for (int i = 0; i < regexes.getLength(); i++) {
                Node regexNode = regexes.item(i);
                if (!(regexNode instanceof Element)) {
                    continue;
                }
                Element regex = (Element) regexNode;
                if (!"regex".equals(regex.getTagName())) {
                    LOG.warn("bad conf file: element not <regex>");
                }

                NodeList fields = regex.getChildNodes();
                String patternValue = null;
                String subValue = null;
                for (int j = 0; j < fields.getLength(); j++) {
                    Node fieldNode = fields.item(j);
                    if (!(fieldNode instanceof Element)) {
                        continue;
                    }
                    Element field = (Element) fieldNode;
                    if ("pattern".equals(field.getTagName()) && field.hasChildNodes()) {
                        patternValue = ((org.w3c.dom.Text) field.getFirstChild()).getData();
                    }

                    if ("substitution".equals(field.getTagName()) && field.hasChildNodes()) {
                        subValue = ((org.w3c.dom.Text) field.getFirstChild()).getData();
                    }

                    if (!field.hasChildNodes()) {
                        subValue = "";
                    }
                }

                if (patternValue != null && subValue != null) {
                    Rule rule = new Rule();
                    try {
                        rule.pattern = Pattern.compile(patternValue);
                    } catch (PatternSyntaxException e) {
                        LOG.error("skipped rule: " + patternValue + " -> " + subValue + " : invalid regular expression pattern: " + e);
                        continue;
                    }
                    rule.substitution = subValue;
                    rules.add(rule);
                }
            }
        } catch (Exception e) {
            LOG.error("error parsing conf file: " + e);
            return EMPTY_RULES;
        }

        if (rules.size() == 0) {
            return EMPTY_RULES;
        }

        return rules;
    }

    /**
     * Class which holds a compiled pattern and its corresponding substition
     * string.
     */
    private static class Rule {
        public Pattern pattern;
        public String substitution;
    }
}
