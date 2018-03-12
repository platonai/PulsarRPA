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
package org.warps.pulsar.crawl.parse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.MimeUtil;
import org.warps.pulsar.common.config.ImmutableConfig;

import java.util.*;
import java.util.stream.Collectors;

/** Creates {@link Parser}. */
public final class ParserFactory {
    public static final Logger LOG = LoggerFactory.getLogger(ParserFactory.class);

    public static final String DEFAULT_MINE_TYPE = "*";

    // Thread safe for both outer map and inner list
    private Map<String, List<Parser>> mineType2Parsers = Collections.synchronizedMap(new HashMap<>());

    public ParserFactory(ImmutableConfig conf) {
        this(Collections.emptyList(), conf);
    }

    public ParserFactory(List<Parser> availableParsers, ImmutableConfig conf) {
        this(new ParserConfigReader().parse(conf), availableParsers);
    }

    public ParserFactory(ParserConfig parserConfig, List<Parser> availableParsers) {
        Map<String, Parser> availableNamedParsers = availableParsers.stream()
                .collect(Collectors.toMap(parser -> parser.getClass().getName(), parser -> parser, (p1, p2) -> p1));

        parserConfig.getParsers().forEach((key, value) -> {
            List<Parser> parsers = value.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(availableNamedParsers::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            mineType2Parsers.put(key, Collections.synchronizedList(parsers));
        });
        LOG.info("Active parsers : " + parserConfig.toString());
    }

    public ParserFactory(Map<String, List<Parser>> parses) {
        mineType2Parsers.putAll(parses);
    }

    /**
     * Function returns an array of {@link Parser}s for a given content type.
     *
     * The function consults the internal list of parse plugins for the
     * ParserFactory to determine the list of pluginIds, then gets the appropriate
     * extension points to instantiate as {@link Parser}s.
     *
     * The function is thread safe
     *
     * @param contentType
     *          The contentType to return the <code>Array</code> of {@link Parser}
     *          s for.
     * @param url
     *          The url for the content that may allow us to get the type from the
     *          file suffix.
     * @return An <code>List</code> of {@link Parser}s for the given contentType.
     */
    public List<Parser> getParsers(String contentType, String url) throws ParserNotFound {
        String mimeType = MimeUtil.cleanMimeType(contentType);

        List<Parser> parsers = mineType2Parsers.get(mimeType);
        if (parsers == null) {
            parsers = mineType2Parsers.get(DEFAULT_MINE_TYPE);
        }

        return parsers;
    }

    private String escapeContentType(String contentType) {
        // Escapes contentType in order to use as a regex
        // (and keep backwards compatibility).
        // This enables to accept multiple types for a single parser.
        return contentType.replace("+", "\\+").replace(".", "\\.");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        mineType2Parsers.values().forEach(parser -> sb.append(parser.getClass().getSimpleName()).append(", "));
        return sb.toString();
    }
}
