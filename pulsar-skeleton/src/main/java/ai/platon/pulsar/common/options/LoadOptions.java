package ai.platon.pulsar.common.options;

import ai.platon.pulsar.common.config.CapabilityTypes;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.options.converters.DurationConverter;
import ai.platon.pulsar.persist.metadata.BrowserType;
import ai.platon.pulsar.persist.metadata.FetchMode;
import com.beust.jcommander.Parameter;

import java.time.Duration;

/**
 * Created by vincent on 17-7-14.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * The expires field supports both ISO-8601 standard and hadoop time duration format
 * ISO-8601 standard : PnDTnHnMn.nS
 * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
 */
public class LoadOptions extends CommonOptions {

    public static LoadOptions DEFAULT = new LoadOptions();

    @Parameter(names = {"-i", "--expires"}, converter = DurationConverter.class, description = "Page datum expire time")
    private Duration expires;

    @Parameter(names = {"-pst", "-persist", "--persist"}, description = "Persist page(s) once fetched")
    private boolean persist = true;

    @Parameter(names = {"-shortenKey", "--shorten-key"}, description = "Page key is generated from baseUrl with parameters removed")
    private boolean shortenKey = false;

    @Parameter(names = {"-retry", "--retry"}, description = "Retry fetching the page if it's failed last time")
    private boolean retry = false;

    @Parameter(names = {"-autoFlush", "--auto-flush"}, arity = 1, description = "Auto flush db whenever a fetch task finished")
    private boolean autoFlush = true;

    @Parameter(names = {"-preferParallel", "--prefer-parallel"}, arity = 1, description = "Parallel fetch urls whenever applicable")
    private boolean preferParallel = true;

    @Parameter(names = {"-fetchMode", "--fetch-mode"}, description = "The fetch mode")
    private String fetchMode;

    @Parameter(names = {"-browser", "--browser"}, description = "The browser to use")
    private String browser;

    @Parameter(names = {"-ignoreFailed", "--ignore-failed"}, arity = 1, description = "Ignore all failed pages in batch loading")
    private boolean ignoreFailed = true;

    @Parameter(names = {"-background", "--background"}, description = "Fetch the page in background")
    private boolean background;

    @Parameter(names = {"-nord", "-noRedirect", "--no-redirect"}, description = "Do not redirect")
    private boolean noRedirect = false;

    @Parameter(names = {"-hardRedirect", "--hard-redirect"}, arity = 1, description = "Return the entire page record " +
            "instead of the temp page with the target's content when redirect")
    private boolean hardRedirect = true;

    @Parameter(names = {"-ps", "-parse", "--parse"}, description = "Parse the page")
    private boolean parse = false;

    @Parameter(names = {"-q", "-query", "--query"}, description = "Extract query to extract data from")
    private String query;

    @Parameter(names = {"-m", "-withModel", "--with-model"}, description = "Also load page model")
    private boolean withModel = false;

    @Parameter(names = {"-lk", "-withLinks", "--with-links"}, description = "Contains links when loading page model")
    private boolean withLinks = false;

    @Parameter(names = {"-tt", "-withText", "--with-text"}, description = "Contains text when loading page model")
    private boolean withText = false;

    @Parameter(names = {"-rpl", "-reparseLinks", "--reparse-links"}, description = "Re-parse all links if the page is parsed")
    private boolean reparseLinks = false;

    @Parameter(names = {"-nolf", "-noLinkFilter", "--no-link-filter"}, description = "No filters applied to parse links")
    private boolean noLinkFilter = false;

    private MutableConfig mutableConfig;

    public LoadOptions() {
        parse();
    }

    public LoadOptions(String[] args) {
        super(args);
    }

    public LoadOptions(String args) {
        super(args.trim().replaceAll("=", " "));
    }

    public static LoadOptions parse(String args) {
        LoadOptions options = new LoadOptions(args);
        options.parse();
        return options;
    }

    public static LoadOptions parse(String args, MutableConfig mutableConfig) {
        LoadOptions options = new LoadOptions(args);
        options.parse();
        options.setMutableConfig(mutableConfig);
        return options;
    }

    public Duration getExpires() {
        if (expires == null && mutableConfig != null) {
            return mutableConfig.getDuration(CapabilityTypes.STORAGE_DATUM_EXPIRES, Duration.ofDays(3650));
        }

        return expires == null ? Duration.ofDays(3650): expires;
    }

    public void setExpires(Duration expires) {
        this.expires = expires;
    }

    public boolean isWithModel() {
        return withModel;
    }

    public void setWithModel(boolean withModel) {
        this.withModel = withModel;
    }

    public boolean isWithLinks() {
        return withLinks;
    }

    public void setWithLinks(boolean withLinks) {
        this.withLinks = withLinks;
    }

    public boolean isWithText() {
        return withText;
    }

    public void setWithText(boolean withText) {
        this.withText = withText;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

    public boolean isParse() {
        return parse;
    }

    public void setParse(boolean parse) {
        this.parse = parse;
    }

    public boolean isShortenKey() {
        return shortenKey;
    }

    public void setShortenKey(boolean shortenKey) {
        this.shortenKey = shortenKey;
    }

    public boolean isReparseLinks() {
        return reparseLinks;
    }

    public void setReparseLinks(boolean reparseLinks) {
        this.reparseLinks = reparseLinks;
    }

    public boolean isNoLinkFilter() {
        return noLinkFilter;
    }

    public void setNoLinkFilter(boolean noLinkFilter) {
        this.noLinkFilter = noLinkFilter;
    }

    public boolean isNoRedirect() {
        return noRedirect;
    }

    public void setNoRedirect(boolean noRedirect) {
        this.noRedirect = noRedirect;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isPreferParallel() {
        return preferParallel;
    }

    public void setPreferParallel(boolean preferParallel) {
        this.preferParallel = preferParallel;
    }

//    public void setNoNormalize(boolean noNormalize) {
//        this.noNormalize = noNormalize;
//    }
//
//    public boolean isNoNormalize() {
//        if (noNormalize == null && mutableConfig != null) {
//            return !mutableConfig.getBoolean(PARSE_NORMALISE, true);
//        }
//
//        return noNormalize != null ? noNormalize : false;
//    }

    public boolean isPersist() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public boolean isAutoFlush() {
        return autoFlush;
    }

    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }

    public FetchMode getFetchMode() {
        if (getBrowser() != BrowserType.NATIVE) {
            return FetchMode.SELENIUM;
        }

        if (fetchMode != null) {
            return FetchMode.fromString(fetchMode);
        } else if (mutableConfig != null) {
            return mutableConfig.getEnum(CapabilityTypes.FETCH_MODE, FetchMode.SELENIUM);
        } else {
            return FetchMode.SELENIUM;
        }
    }

    public void setFetchMode(FetchMode fetchMode) {
        this.fetchMode = fetchMode.toString().toLowerCase();
    }

    public BrowserType getBrowser() {
        if (browser != null) {
            return BrowserType.fromString(this.browser);
        } else if (mutableConfig != null) {
            return mutableConfig.getEnum(CapabilityTypes.SELENIUM_BROWSER, BrowserType.CHROME);
        } else {
            return BrowserType.CHROME;
        }
    }

    public void setBrowser(BrowserType browser) {
        this.browser = browser.name().toLowerCase();
    }

    public boolean isIgnoreFailed() {
        return ignoreFailed;
    }

    public void setIgnoreFailed(boolean ignoreFailed) {
        this.ignoreFailed = ignoreFailed;
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public boolean isHardRedirect() {
        return hardRedirect;
    }

    public void setHardRedirect(boolean hardRedirect) {
        this.hardRedirect = hardRedirect;
    }

    public MutableConfig getMutableConfig() {
        return mutableConfig;
    }

    public void setMutableConfig(MutableConfig mutableConfig) {
        this.mutableConfig = mutableConfig;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "-ps", isParse(),
                "-q", getQuery(),
                "-m", isWithModel(),
                "-lk", isWithLinks(),
                "-tt", isWithText(),
                "-retry", isRetry(),
                "-rpl", isReparseLinks(),
                "-nord", isNoRedirect(),
                "-nolf", isNoLinkFilter(),
                "-prst", isPersist(),
                "-shortenKey", isShortenKey(),
                "-expires", getExpires(),
                "-autoFlush", isAutoFlush(),
                "-fetchMode", getFetchMode(),
                "-browser", getBrowser(),
                "-preferParallel", isPreferParallel(),
                "-background", isBackground(),
                "-hardRedirect", isHardRedirect()
        );
    }

    @Override
    public String toString() {
        return getParams().withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replaceAll("\\s+", " ");
    }
}
