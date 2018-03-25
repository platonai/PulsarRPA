package fun.platonic.pulsar.common.options;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.options.converters.DurationConverter;
import fun.platonic.pulsar.persist.metadata.BrowserType;
import fun.platonic.pulsar.persist.metadata.FetchMode;

import java.time.Duration;

import static fun.platonic.pulsar.common.config.CapabilityTypes.*;

/**
 * Created by vincent on 17-7-14.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 * <p>
 * The expires field supports both ISO-8601 standard and hadoop time duration format
 * ISO-8601 standard : PnDTnHnMn.nS
 * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
 */
public class LoadOptions extends CommonOptions {

    @Parameter(names = {"-i", "--expires"}, converter = DurationConverter.class, description = "Web page expire time")
    private Duration expires;

    @Parameter(names = {"-pst", "--persist"}, description = "Persist page(s) once fetched")
    private boolean persist = true;

    @Parameter(names = {"-retry", "--retry"}, description = "Retry fetch the page if it's failed last time")
    private boolean retry = false;

    @Parameter(names = {"--auto-flush"}, description = "Auto flush db when page(s)")
    private boolean autoFlush = true;

    @Parameter(names = {"--prefer-parallel"}, description = "Parallel fetch urls whenever applicable")
    private Boolean preferParallel;

    @Parameter(names = {"--fetch-mode"}, description = "The fetch mode")
    private String fetchMode;

    @Parameter(names = {"--browser"}, description = "The browser")
    private String browser;

    @Parameter(names = {"--ignore-failed"}, description = "Ignore all failed pages in batch loading")
    private Boolean ignoreFailed;

    @Parameter(names = {"--background"}, description = "Fetch the page in background")
    private Boolean background;

    @Parameter(names = {"-nord", "--no-redirect"}, description = "Do not redirect")
    private boolean noRedirect = false;

    @Parameter(names = {"--hard-redirect"}, description = "Return the entire WebPage " +
            "instead of the temp WebPage with the target's content when redirect")
    private Boolean hardRedirect = true;

    @Parameter(names = {"-q", "--query"}, description = "Extract query for model")
    private String query;

    @Parameter(names = {"-ps", "--parse"}, description = "Parse the page")
    private boolean parse = false;

    @Parameter(names = {"-m", "--with-model"}, description = "Also load page model")
    private boolean withModel = false;

    @Parameter(names = {"-lk", "--with-links"}, description = "Contains links when loading page model")
    private boolean withLinks = false;

    @Parameter(names = {"-tt", "--with-text"}, description = "Contains text when loading page model")
    private boolean withText = false;

    @Parameter(names = {"-rpl", "--reparse-links"}, description = "Re-parse all links if the page is parsed")
    private boolean reparseLinks = false;

    @Parameter(names = {"-nolf", "--no-link-filter"}, description = "No filters applied to parse links")
    private boolean noLinkFilter = false;


    private MutableConfig mutableConfig;

    public LoadOptions() {
    }

    public LoadOptions(String[] args) {
        super(args);
    }

    private LoadOptions(String args) {
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
            return mutableConfig.getDuration(STORAGE_DATUM_EXPIRES, Duration.ofDays(3650));
        }

        return expires;
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
        if (preferParallel == null && mutableConfig != null) {
            return mutableConfig.getBoolean(FETCH_PREFER_PARALLEL, false);
        }

        return preferParallel != null ? preferParallel : false;
    }

    public void setPreferParallel(boolean preferParallel) {
        this.preferParallel = preferParallel;
    }

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
            return mutableConfig.getEnum(FETCH_MODE, FetchMode.SELENIUM);
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
            return mutableConfig.getEnum(SELENIUM_BROWSER, BrowserType.CHROME);
        } else {
            return BrowserType.CHROME;
        }
    }

    public void setBrowser(BrowserType browser) {
        this.browser = browser.name().toLowerCase();
    }

    public boolean isIgnoreFailed() {
        if (ignoreFailed == null && mutableConfig != null) {
            return mutableConfig.getBoolean(LOAD_IGNORE_FAILED_IN_BATCH, true);
        }

        return ignoreFailed != null ? ignoreFailed : true;
    }

    public void setIgnoreFailed(boolean ignoreFailed) {
        this.ignoreFailed = ignoreFailed;
    }

    public boolean isBackground() {
        if (background != null) {
            return background;
        } else if (mutableConfig != null) {
            return mutableConfig.getBoolean(FETCH_BACKGROUND, false);
        } else {
            return false;
        }
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public boolean isHardRedirect() {
        if (hardRedirect != null) {
            return hardRedirect;
        }

        return mutableConfig != null && mutableConfig.getBoolean(LOAD_HARD_REDIRECT, false);
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
                "--retry", isRetry(),
                "-rpl", isReparseLinks(),
                "-nord", isNoRedirect(),
                "-nolf", isNoLinkFilter(),
                "-prst", isPersist(),
                "--expires", getExpires(),
                "--auto-flush", isAutoFlush(),
                "--fetch-mode", getFetchMode(),
                "--browser", getBrowser(),
                "--prefer-parallel", isPreferParallel(),
                "--background", isBackground(),
                "--hard-redirect", isHardRedirect()
        );
    }

    @Override
    public String toString() {
        return getParams().withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replaceAll("\\s+", " ");
    }
}
