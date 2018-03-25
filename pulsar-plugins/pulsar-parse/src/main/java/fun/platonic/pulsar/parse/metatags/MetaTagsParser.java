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
package fun.platonic.pulsar.parse.metatags;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.parse.ParseFilter;
import fun.platonic.pulsar.crawl.parse.html.HTMLMetaTags;
import fun.platonic.pulsar.crawl.parse.html.ParseContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fun.platonic.pulsar.persist.Metadata;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.MultiMetadata;

import java.util.*;

import static fun.platonic.pulsar.common.config.CapabilityTypes.METATAG_NAMES;

/**
 * ParseResult HTML meta tags (keywords, description) and store them in the parse
 * metadata so that they can be indexed with the index-metadata plugin with the
 * prefix 'metatag.'. Metatags are matched ignoring case.
 */
public class MetaTagsParser implements ParseFilter {

    public static final String PARSE_META_PREFIX = "meta_";
    private static final Log LOG = LogFactory.getLog(MetaTagsParser.class.getName());
    private ImmutableConfig conf;
    private Set<String> metatagset = new HashSet<>();

    public MetaTagsParser() {
    }

    public MetaTagsParser(ImmutableConfig conf) {
        reload(conf);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
        // specify whether we want a specific subset of metadata
        // by default take everything we can find
        String[] values = conf.getStrings(METATAG_NAMES, "*");
        for (String val : values) {
            metatagset.add(val.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

    public void filter(ParseContext parseContext) {
        WebPage page = parseContext.getPage();
        HTMLMetaTags metaTags = parseContext.getMetaTags();

        MultiMetadata generalMetaTags = metaTags.getGeneralTags();
        for (String tagName : generalMetaTags.names()) {
            // multiple values of a metadata field are separated by '\t' in persist.
            StringBuilder sb = new StringBuilder();
            for (String value : generalMetaTags.getValues(tagName)) {
                if (sb.length() > 0) {
                    sb.append("\t");
                }
                sb.append(value);
            }

            addIndexedMetatags(page.getMetadata(), tagName, sb.toString());
        }

        Properties httpequiv = metaTags.getHttpEquivTags();
        Enumeration<?> tagNames = httpequiv.propertyNames();
        while (tagNames.hasMoreElements()) {
            String name = (String) tagNames.nextElement();
            String value = httpequiv.getProperty(name);
            addIndexedMetatags(page.getMetadata(), name, value);
        }
    }

    /**
     * Check whether the metatag is in the list of metatags to be indexed (or if
     * '*' is specified). If yes, add it to parse metadata.
     */
    private void addIndexedMetatags(Metadata metadata, String metatag, String value) {
        String lcMetatag = metatag.toLowerCase(Locale.ROOT);
        if (metatagset.contains("*") || metatagset.contains(lcMetatag)) {
            // LOG.trace("Found meta tag: " + lcMetatag + "\t" + value);
            metadata.set(PARSE_META_PREFIX + lcMetatag, value);
        }
    }
}
