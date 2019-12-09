package ai.platon.pulsar.crawl.parse;

import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.common.options.LinkOptions;
import ai.platon.pulsar.crawl.filter.CrawlFilters;
import ai.platon.pulsar.persist.HypeLink;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;

/**
 * Created by vincent on 17-5-21.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class LinkFilter implements ReloadableParameterized {
    public static final Logger LOG = LoggerFactory.getLogger(LinkFilter.class);

    private URLUtil.GroupMode groupMode;
    private boolean ignoreExternalLinks;

    private int maxUrlLength;

    private ImmutableConfig conf;
    private CrawlFilters crawlFilters;
    private String sourceHost;
    private LinkOptions linkOptions;
    private boolean reparseLinks = false;
    private boolean noFilter = false;
    private int debugLevel = 0;
    private Set<String> links = new TreeSet<>();
    private List<String> filterReport = new LinkedList<>();

    public LinkFilter(CrawlFilters crawlFilters, ImmutableConfig conf) {
        this.crawlFilters = crawlFilters;

        reload(conf);
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "groupMode", groupMode,
                "ignoreExternalLinks", ignoreExternalLinks,
                "maxUrlLength", maxUrlLength,
                "defaultAnchorLenMin", conf.get(PARSE_MIN_ANCHOR_LENGTH),
                "defaultAnchorLenMax", conf.get(PARSE_MAX_ANCHOR_LENGTH)
        );
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;

        groupMode = conf.getEnum(FETCH_QUEUE_MODE, URLUtil.GroupMode.BY_HOST);
        ignoreExternalLinks = conf.getBoolean(PARSE_IGNORE_EXTERNAL_LINKS, false);
        maxUrlLength = conf.getInt(PARSE_MAX_URL_LENGTH, 1024);
    }

    public void reset(WebPage page) {
        Objects.requireNonNull(page);

        linkOptions = LinkOptions.parse(page.getOptions().toString(), conf);
        sourceHost = ignoreExternalLinks ? URLUtil.getHost(page.getUrl(), groupMode) : "";

        reparseLinks = page.getVariables().contains(Name.REPARSE_LINKS);
        noFilter = page.getVariables().contains(Name.PARSE_NO_LINK_FILTER);
        debugLevel = page.getVariables().get(Name.PARSE_LINK_FILTER_DEBUG_LEVEL, 0);

        links.clear();
        page.getLinks().forEach(l -> links.add(l.toString()));

        filterReport.clear();
    }

    public List<String> getFilterReport() {
        return filterReport;
    }

    public Predicate<HypeLink> asPredicate(WebPage page) {
        reset(page);
        return l -> {
            int r = this.filter(l);
            if (debugLevel > 0) {
                filterReport.add(r + " <- " + l.getUrl() + "\t" + l.getAnchor());
            }
            return 0 == r;
        };
    }

    public int filter(HypeLink link) {
        if (noFilter) {
            return 0;
        }

        String url = link.getUrl();

        if (link.getUrl().isEmpty()) {
            return 110;
        }

        if (link.getUrl().length() > maxUrlLength) {
            return 112;
        }

        String destHost = URLUtil.getHost(url, groupMode);
        if (destHost.isEmpty()) {
            return 104;
        }

        if (ignoreExternalLinks && !sourceHost.equals(destHost)) {
            return 106;
        }

        int r = linkOptions.filter(link.getUrl(), link.getAnchor());
        if (r > 0) {
            return 2000 + r;
        }

        if (!reparseLinks && links.contains(link.getUrl())) {
            return 118;
        }

        url = crawlFilters.normalizeToEmpty(link.getUrl());
        if (url.isEmpty()) {
            return 1000;
        }

        if (!reparseLinks && links.contains(url)) {
            return 120;
        }

        return 0;
    }
}
