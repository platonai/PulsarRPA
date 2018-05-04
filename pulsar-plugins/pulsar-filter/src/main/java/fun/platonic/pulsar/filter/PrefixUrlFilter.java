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

// $Id: PrefixUrlFilter.java 823614 2009-10-09 17:02:32Z ab $
package fun.platonic.pulsar.filter;

import fun.platonic.pulsar.common.PrefixStringMatcher;
import fun.platonic.pulsar.common.ResourceLoader;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.TrieStringMatcher;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.filter.UrlFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR;

/**
 * Filters URLs based on a file of URL prefixes. The file is named by (1)
 * property "urlfilter.prefix.file" in ./conf/pulsar-default.xml, and (2)
 * attribute "file" in plugin.xml of this plugin Attribute "file" has higher
 * precedence if defined.
 * <p>
 * <p>
 * The format of this file is one URL prefix per line.
 * </p>
 */
public class PrefixUrlFilter implements UrlFilter {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixUrlFilter.class);

    public static String URLFILTER_PREFIX_RULES = "urlfilter.prefix.rules";
    public static String URLFILTER_PREFIX_FILE = "urlfilter.prefix.file";

    private TrieStringMatcher trie;

    public PrefixUrlFilter(ImmutableConfig conf) throws IOException {
        try (Reader reader = getRulesReader(conf)) {
            trie = readConfiguration(reader);
        } catch (IOException e) {
            LOG.error(StringUtil.stringifyException(e));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void reload(String stringResource) throws IOException {
        trie = readConfiguration(new StringReader(stringResource));
    }

    public String filter(String url) {
        if (trie.shortestMatch(url) == null) return null;
        else return url;
    }

    private TrieStringMatcher readConfiguration(Reader reader) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        List<String> urlprefixes = new ArrayList<>();
        String line;

        while ((line = in.readLine()) != null) {
            if (line.length() == 0)
                continue;

            char first = line.charAt(0);
            switch (first) {
                case ' ':
                case '\n':
                case '#': // skip blank & comment lines
                    continue;
                default:
                    urlprefixes.add(line);
            }
        }

        return new PrefixStringMatcher(urlprefixes);
    }

    protected Reader getRulesReader(ImmutableConfig conf) throws FileNotFoundException {
        String stringResource = conf.get(URLFILTER_PREFIX_RULES);
        String fileResource = conf.get(URLFILTER_PREFIX_FILE, "prefix-urlfilter.txt");
        String resourcePrefix = conf.get(PULSAR_CONFIG_PREFERRED_DIR, "");
        return new ResourceLoader().getReader(stringResource, fileResource, resourcePrefix);
    }
}
