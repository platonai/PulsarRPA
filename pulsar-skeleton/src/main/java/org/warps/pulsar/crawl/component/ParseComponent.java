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

package org.warps.pulsar.crawl.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.crawl.filter.CrawlFilters;
import org.warps.pulsar.crawl.parse.PageParser;
import org.warps.pulsar.crawl.parse.ParseResult;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.metadata.Name;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.warps.pulsar.common.PulsarConstants.YES_STRING;

/**
 * Parser checker, useful for testing parser. It also accurately reports
 * possible fetching and parsing failures and presents protocol status signals
 * to aid debugging. The tool enables us to retrieve the following data from any
 */
@Component
public class ParseComponent {

    public static final Logger LOG = LoggerFactory.getLogger(ParseComponent.class);

    private ImmutableConfig conf;
    private CrawlFilters crawlFilters;
    private PageParser pageParser;
    private Map<String, Object> report = Collections.synchronizedMap(new HashMap<>());

    public ParseComponent(ImmutableConfig conf) {
        this.conf = conf;
    }

    @Autowired
    public ParseComponent(CrawlFilters crawlFilters, PageParser pageParser, ImmutableConfig conf) {
        this.conf = conf;
        this.crawlFilters = crawlFilters;
        this.pageParser = pageParser;
    }

    public CrawlFilters getCrawlFilters() {
        return crawlFilters;
    }

    public void setCrawlFilters(CrawlFilters crawlFilters) {
        this.crawlFilters = crawlFilters;
    }

    public PageParser getPageParser() {
        return pageParser;
    }

    public void setPageParser(PageParser pageParser) {
        this.pageParser = pageParser;
    }

    public ParseResult parse(WebPage page) {
        return parse(page, false, false);
    }

    public ParseResult parse(WebPage page, boolean reparseLinks, boolean noLinkFilter) {
        return parse(page, "", reparseLinks, noLinkFilter);
    }

    public ParseResult parse(WebPage page, @Nullable String query, boolean reparseLinks, boolean noLinkFilter) {
        Objects.requireNonNull(page);

        if (reparseLinks) {
            page.getVariables().set(Name.FORCE_FOLLOW, YES_STRING);
            page.getVariables().set(Name.REPARSE_LINKS, YES_STRING);
            page.getVariables().set(Name.PARSE_LINK_FILTER_DEBUG_LEVEL, 1);
        }

        if (noLinkFilter) {
            page.getVariables().set(Name.PARSE_NO_LINK_FILTER, YES_STRING);
        }

        if (query != null) {
            page.setQuery(query);
        }

        this.report.clear();
        ParseResult parseResult = pageParser.parse(page);

        page.getVariables().remove(Name.REPARSE_LINKS);
        page.getVariables().remove(Name.FORCE_FOLLOW);
        page.getVariables().remove(Name.PARSE_LINK_FILTER_DEBUG_LEVEL);
        page.getVariables().remove(Name.PARSE_NO_LINK_FILTER);

        return parseResult;
    }

    public Map<String, Object> getReport() {
        report.clear();
        report.put("linkFilterReport", pageParser.getLinkFilter()
                .getFilterReport().stream().collect(Collectors.joining("\n")));

        return report;
    }
}
