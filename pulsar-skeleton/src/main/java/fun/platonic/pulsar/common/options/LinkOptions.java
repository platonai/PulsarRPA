package fun.platonic.pulsar.common.options;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.persist.HypeLink;
import fun.platonic.pulsar.persist.gora.generated.GHypeLink;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PARSE_MAX_ANCHOR_LENGTH;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PARSE_MIN_ANCHOR_LENGTH;

/**
 * Created by vincent on 17-3-18.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class LinkOptions extends PulsarOptions {

    // shortest url example: http://news.baidu.com/
    // longest url example: http://data.news.163.com/special/datablog/
    public static final String DEFAULT_SEED_ARGS = "-amin 2 -amax 4 -umin 23 -umax 45";
    public static final LinkOptions DEFAULT_SEED_OPTIONS = LinkOptions.parse(DEFAULT_SEED_ARGS);

    public static final LinkOptions DEFAULT = new LinkOptions();

    @Parameter(names = {"-css", "--restrict-css"}, description = "Path to the DOM to follow links")
    private String restrictCss = "body";
    @Parameter(names = {"-amin", "--anchor-min-length"}, description = "Anchor min length")
    private int minAnchorLength = 5;
    @Parameter(names = {"-amax", "--anchor-max-length"}, description = "Anchor max length")
    private int maxAnchorLength = 50;
    @Parameter(names = {"-areg", "--anchor-regex"}, description = "Anchor regex")
    private String anchorRegex = ".+";
    @Parameter(names = {"-umin", "--url-min-length"}, description = "Min url length")
    private int minUrlLength = 23;
    @Parameter(names = {"-umax", "--url-max-length"}, description = "Max url length")
    private int maxUrlLength = 150;
    @Parameter(names = {"-upre", "--url-prefix"}, description = "Url prefix")
    private String urlPrefix = "";
    @Parameter(names = {"-ucon", "--url-contains"}, description = "Url contains")
    private String urlContains = "";
    @Parameter(names = {"-upos", "--url-postfix"}, description = "Url postfix")
    private String urlPostfix = "";
    @Parameter(names = {"-ureg", "--url-regex"}, description = "Url regex")
    private String urlRegex = ".+";
    @Parameter(names = {"-log", "--log-level"}, description = "Log level")
    private int logLevel = 0;

    private List<String> report = new LinkedList<>();

    public LinkOptions() {
    }

    public LinkOptions(String args) {
        super(args);
    }

    public LinkOptions(String args, ImmutableConfig conf) {
        super(args);
        init(conf);
    }

    public LinkOptions(String[] args) {
        super(args);
    }

    public LinkOptions(String[] args, ImmutableConfig conf) {
        super(args);
        init(conf);
    }

    public LinkOptions(Map<String, String> args) {
        super(args);
    }

    public static LinkOptions parse(String args) {
        LinkOptions options = new LinkOptions(args);
        options.parse();
        return options;
    }

    public static LinkOptions parse(String args, ImmutableConfig conf) {
        LinkOptions options = new LinkOptions(args, conf);
        options.parse();
        return options;
    }

    private void init(ImmutableConfig conf) {
        this.minAnchorLength = conf.getUint(PARSE_MIN_ANCHOR_LENGTH, 8);
        this.maxAnchorLength = conf.getUint(PARSE_MAX_ANCHOR_LENGTH, 40);
    }

    public String getRestrictCss() {
        return restrictCss;
    }

    public void setRestrictCss(String restrictCss) {
        this.restrictCss = restrictCss;
    }

    public int getMinAnchorLength() {
        return minAnchorLength;
    }

    public void setMinAnchorLength(int minAnchorLength) {
        this.minAnchorLength = minAnchorLength;
    }

    public int getMaxAnchorLength() {
        return maxAnchorLength;
    }

    public void setMaxAnchorLength(int maxAnchorLength) {
        this.maxAnchorLength = maxAnchorLength;
    }

    public int getMinUrlLength() {
        return minUrlLength;
    }

    public void setMinUrlLength(int minUrlLength) {
        this.minUrlLength = minUrlLength;
    }

    public int getMaxUrlLength() {
        return maxUrlLength;
    }

    public void setMaxUrlLength(int maxUrlLength) {
        this.maxUrlLength = maxUrlLength;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getUrlContains() {
        return urlContains;
    }

    public void setUrlContains(String urlContains) {
        this.urlContains = urlContains;
    }

    public String getUrlPostfix() {
        return urlPostfix;
    }

    public void setUrlPostfix(String urlPostfix) {
        this.urlPostfix = urlPostfix;
    }

    public String getUrlRegex() {
        return urlRegex;
    }

    public void setUrlRegex(String urlRegex) {
        this.urlRegex = urlRegex;
    }

    public int getLogLevel() {
        return this.logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int filter(HypeLink l) {
        return filter(l.getUrl(), l.getAnchor());
    }

    public int filter(String url, String anchor) {
        if (anchor.length() < minAnchorLength || anchor.length() > maxAnchorLength) {
            return 100;
        }

        if (!anchorRegex.isEmpty() && !".+".equals(anchorRegex)) {
            if (!anchor.matches(anchorRegex)) {
                return 101;
            }
        }

        return filter(url);
    }

    public int filter(String url) {
        if (url.length() < minUrlLength || url.length() > maxUrlLength) {
            return 200;
        }

        if (!urlPrefix.isEmpty() && !url.startsWith(urlPrefix)) {
            return 210;
        }

        if (!urlPostfix.isEmpty() && !url.endsWith(urlPostfix)) {
            return 211;
        }

        if (!urlContains.isEmpty() && !url.contains(urlContains)) {
            return 212;
        }

        if (!urlRegex.isEmpty() && !url.matches(urlRegex)) {
            return 213;
        }

        return 0;
    }

    public Predicate<String> asUrlPredicate() {
        report.clear();

        return url -> {
            int r = this.filter(url);
            if (logLevel > 0) {
                report.add(r + " <- " + url);
            }
            return 0 == r;
        };
    }

    public Predicate<HypeLink> asPredicate() {
        report.clear();

        return l -> {
            int r = this.filter(l.getUrl(), l.getAnchor());
            if (logLevel > 0) {
                report.add(r + " <- " + l.getUrl() + "\t" + l.getAnchor());
            }
            return 0 == r;
        };
    }

    public Predicate<GHypeLink> asGHypeLinkPredicate() {
        report.clear();

        return l -> {
            int r = this.filter(l.getUrl().toString(), l.getAnchor().toString());
            if (logLevel > 0) {
                report.add(r + " <- " + l.getUrl() + "\t" + l.getAnchor());
            }
            return 0 == r;
        };
    }

    public Params getParams() {
        return Params.of(
                "-css", restrictCss,
                "-amin", minAnchorLength,
                "-amax", maxAnchorLength,
                "-areg", anchorRegex,
                "-umin", minUrlLength,
                "-umax", maxUrlLength,
                "-upre", urlPrefix,
                "-ucon", urlContains,
                "-upos", urlPostfix,
                "-ureg", urlRegex
        )
                .filter(p -> p.getValue() != null)
                .filter(p -> !p.getValue().toString().isEmpty());
    }

    public String build() {
        return getParams().withKVDelimiter(" ").formatAsLine();
    }

    public List<String> getReport() {
        return report;
    }

    @Override
    public String toString() {
        return build();
    }
}
