/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.Strings;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.common.urls.UrlUtils;
import ai.platon.pulsar.persist.experimental.WebAsset;
import ai.platon.pulsar.persist.gora.generated.*;
import ai.platon.pulsar.persist.metadata.*;
import ai.platon.pulsar.persist.model.*;
import org.apache.avro.util.Utf8;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.gora.util.ByteUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS;
import static ai.platon.pulsar.common.config.AppConstants.*;

/**
 * The core web page structure
 */
final public class WebPage implements Comparable<WebPage>, WebAsset {
    /**
     * The WebPage object sequence number generator.
     * */
    private static final AtomicInteger SEQUENCER = new AtomicInteger();
    /**
     * The nil page.
     * */
    public static final WebPage NIL = newInternalPage(NIL_PAGE_URL, 0, "nil", "nil");
    /**
     * The page id which is unique in process scope.
     * */
    private Integer id = SEQUENCER.incrementAndGet();
    /**
     * The url is the permanent internal address, while the location is the last working address.
     */
    @NotNull
    private String url = "";
    /**
     * The reversed url of the web page, it's also the key of the underlying storage of this webpage.
     * It's faster to retrieve the page by the reversed url.
     */
    @NotNull
    private String reversedUrl = "";
    /**
     * The underlying persistent object.
     */
    @NotNull
    private GWebPage page;
    /**
     * A webpage scope configuration, any modifications made to it will exclusively impact this particular webpage.
     */
    @NotNull
    private VolatileConfig conf;
    /**
     * Web page scope variables
     */
    private final Variables variables = new Variables();
    /**
     * Store arbitrary data associated with the webpage.
     */
    private final Variables data = new Variables();
    /**
     * The page datum for update.
     * Page datum is collected during the fetch phrase and is used to update the page in the update phase.
     * */
    private PageDatum pageDatum = null;
    /**
     * If this page is fetched from Internet
     */
    private boolean isCached = false;

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    private boolean isLoaded = false;

    /**
     * If this page is fetched from Internet
     */
    private boolean isFetched = false;

    /**
     * If this page is canceled
     */
    private boolean isCanceled = false;

    /**
     * If this page is fetched and updated
     */
    private volatile boolean isContentUpdated = false;

    /**
     * The cached content.
     * TODO: use a loading cache for all cached page contents.
     */
    private volatile ByteBuffer tmpContent = null;

    /**
     * The delay time to retry if a retry is needed
     */
    private Duration retryDelay = Duration.ZERO;

    /**
     * The field loader to load fields lazily.
     */
    private Function<String, GWebPage> lazyFieldLoader = null;

    private final List<String> lazyLoadedFields = new ArrayList<>();

    private final Object CONTENT_MONITOR = new Object();
    private final Object PAGE_MODEL_MONITOR = new Object();
//    private final Deque<String> lazyLoadedFields = new ConcurrentLinkedDeque<>();

    private WebPage(
            @NotNull String urlOrKey, @NotNull GWebPage page, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        this.url = urlReversed ? UrlUtils.unreverseUrl(urlOrKey) : urlOrKey;
        this.reversedUrl = urlReversed ? urlOrKey : UrlUtils.reverseUrlOrEmpty(urlOrKey);
        this.conf = conf;
        this.page = page;

        if (page.getBaseUrl() == null) {
            setLocation(this.url);
        }
    }

    private WebPage(
            @NotNull String url, @NotNull String reversedUrl, @NotNull GWebPage page, @NotNull VolatileConfig conf
    ) {
        this.url = url;
        this.reversedUrl = reversedUrl;
        this.conf = conf;
        this.page = page;

        if (page.getBaseUrl() == null) {
            setLocation(this.url);
        }
    }

    @NotNull
    public static WebPage newWebPage(@NotNull String url, @NotNull VolatileConfig conf) {
        return newWebPage(url, conf, null);
    }

    @NotNull
    public static WebPage newWebPage(@NotNull String url, @NotNull VolatileConfig conf, @Nullable String href) {
        return newWebPageInternal(url, conf, href);
    }

    @NotNull
    private static WebPage newWebPageInternal(@NotNull String url, @NotNull VolatileConfig conf, @Nullable String href) {
        WebPage page = new WebPage(url, GWebPage.newBuilder().build(), false, conf);

        page.setLocation(url);
        page.setConf(conf);
        page.setHref(href);
        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);
        page.setCreateTime(Instant.now());
        page.setModifiedTime(Instant.now());
        page.setScore(0);
        page.setFetchCount(0);

        return page;
    }

    @NotNull
    public static WebPage newInternalPage(@NotNull String url) {
        return newInternalPage(url, "internal", "internal");
    }

    @NotNull
    public static WebPage newInternalPage(@NotNull String url, @NotNull String title) {
        return newInternalPage(url, title, "internal");
    }

    @NotNull
    public static WebPage newInternalPage(@NotNull String url, @NotNull String title, @NotNull String content) {
        return newInternalPage(url, -1, title, content);
    }

    @NotNull
    public static WebPage newInternalPage(@NotNull String url, int id, @NotNull String title, @NotNull String content) {
        VolatileConfig unsafe = VolatileConfig.Companion.getUNSAFE();
        WebPage page = WebPage.newWebPage(url, unsafe);
        if (id >= 0) {
            page.id = id;
        }

        page.setLocation(url);
        page.setModifiedTime(Instant.EPOCH);
        page.setPrevFetchTime(Instant.EPOCH);
        page.setFetchTime(DateTimes.INSTANCE.getDoomsday());
        page.setFetchInterval(ChronoUnit.CENTURIES.getDuration());
        page.setFetchPriority(FETCH_PRIORITY_MIN);
        page.setCrawlStatus(CrawlStatus.STATUS_UNFETCHED);

        page.setDistance(DISTANCE_INFINITE); // or -1?
        page.getMarks().put(Mark.INTERNAL, YES_STRING);
        page.getMarks().put(Mark.INACTIVE, YES_STRING);

        page.setPageTitle(title);
        page.setContent(content);

        return page;
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @NotNull
    public static WebPage box(
            @NotNull String url, @NotNull String reversedUrl, @NotNull GWebPage page, @NotNull VolatileConfig conf) {
        return new WebPage(url, reversedUrl, page, conf);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @NotNull
    public static WebPage box(@NotNull String url, @NotNull GWebPage page, @NotNull VolatileConfig conf) {
        return box(url, page, false, conf);
    }

    /**
     * Initialize a WebPage with the underlying GWebPage instance.
     */
    @NotNull
    public static WebPage box(
            @NotNull String url, @NotNull GWebPage page, boolean urlReversed, @NotNull VolatileConfig conf
    ) {
        return new WebPage(url, page, urlReversed, conf);
    }

    /**
     * Return a Utf8 string.
     *
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     * */
    @NotNull
    public static Utf8 wrapKey(@NotNull String key) {
        return u8(key);
    }

    /**
     * Return a Utf8 string.
     * <p>
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     * */
    @NotNull
    public static Utf8 wrapKey(@NotNull Mark mark) {
        return u8(mark.value());
    }

    /**
     * Return a Utf8 string.
     * <p>
     * Unlike {@link String}, instances are mutable. This is more
     * efficient than {@link String} when reading or writing a sequence of values,
     * as a single instance may be reused.
     * */
    @Nullable
    public static Utf8 u8(@Nullable String value) {
        if (value == null) {
            // TODO: return new Utf8.EMPTY?
            return null;
        }
        return new Utf8(value);
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    @NotNull
    public String getKey() {
        return getReversedUrl();
    }

    @NotNull
    public String getReversedUrl() {
        return reversedUrl;
    }

    /**
     * A process scope page id.
     * */
    public int getId() {
        return id;
    }

    /**
     * Get The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     * <p>
     * TODO: use a separate field for href
     *
     * @return The hypertext reference
     */
    @Nullable
    public String getHref() {
        return getMetadata().get(Name.HREF);
    }

    /**
     * Set The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * @param href The hypertext reference
     */
    public void setHref(@Nullable String href) {
        getMetadata().set(Name.HREF, href);
    }

    public boolean isNil() {
        return this == NIL;
    }

    public boolean isNotNil() {
        return !isNil();
    }

    public boolean isInternal() {
        return hasMark(Mark.INTERNAL);
    }

    public boolean isNotInternal() {
        return !isInternal();
    }

    @NotNull
    public GWebPage unbox() {
        return page;
    }

    public void unsafeSetGPage(@NotNull GWebPage page) {
        this.page = page;
    }

    public void unsafeCloneGPage(WebPage page) {
        unsafeSetGPage(GWebPage.newBuilder(page.unbox()).build());
    }

    //////////////////////////////////////////////////////////////////////////////////
    //
    // Common fields
    //

    @NotNull
    public Variables getVariables() {
        return variables;
    }

    /**
     * Check if the page scope temporary variable with {@code name} exists
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    public boolean hasVar(@NotNull String name) {
        return variables.contains(name);
    }

    /**
     * Returns the page scope temporary variable to which the specified name is mapped,
     * or {@code null} if the local variable map contains no mapping for the name.
     *
     * @param name the name whose associated value is to be returned
     * @return the value to which the specified name is mapped, or
     *         {@code null} if the local variable map contains no mapping for the key
     */
    @Nullable
    public Object getVar(@NotNull String name) {
        return variables.get(name);
    }

    /**
     * Retrieves and removes the local variable with the given name.
     */
    public Object removeVar(@NotNull String name) {
        return variables.remove(name);
    }

    /**
     * Set a page scope temporary variable.
     *
     * @param name  The variable name.
     * @param value The variable value.
     */
    public void setVar(@NotNull String name, @NotNull Object value) {
        variables.set(name, value);
    }

    /**
     * Returns the data to which the specified name is mapped,
     * or {@code null} if the data map contains no mapping for the name.
     *
     * @param name the name whose associated value is to be returned
     * @return the value to which the specified name is mapped, or
     *         {@code null} if the local variable map contains no mapping for the key
     */
    @Nullable
    public Object data(@NotNull String name) {
        return data.get(name);
    }

    /**
     * Store arbitrary data associated with the webpage.
     *
     * @param name  A string naming the piece of data to set.
     * @param value The new data value.
     */
    public void data(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            data.remove(name);
        } else {
            data.set(name, value);
        }
    }

    @Nullable
    public PageDatum getPageDatum() {
        return pageDatum;
    }

    public void setPageDatum(PageDatum pageDatum) {
        this.pageDatum = pageDatum;
    }

    public boolean isCached() {
        return isCached;
    }

    public void setCached(boolean cached) {
        isCached = cached;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public boolean isFetched() {
        return isFetched;
    }

    public void setFetched(boolean fetched) {
        isFetched = fetched;
    }

    /**
     * Check if the page is canceled.
     * <p>
     * If a page is canceled, it should not be updated.
     * */
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Check if the page is canceled.
     * <p>
     * If a page is canceled, it should not be updated.
     * */
    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public boolean isContentUpdated() {
        return isContentUpdated;
    }

    @NotNull
    public VolatileConfig getConf() {
        return conf;
    }

    public void setConf(@NotNull VolatileConfig conf) {
        this.conf = conf;
    }

    public Metadata getMetadata() {
        return Metadata.box(page.getMetadata());
    }

    /**
     * CrawlMarks are used for nutch style crawling.
     * */
    public CrawlMarks getMarks() {
        return CrawlMarks.box(page.getMarkers());
    }

    /**
     * Check if a mark is marked.
     * <p>
     * CrawlMarks are used for nutch style crawling.
     * */
    public boolean hasMark(Mark mark) {
        return page.getMarkers().get(wrapKey(mark)) != null;
    }

    /**
     * The load arguments is variant task by task, so the local version is the first choice,
     * while the persisted version is used for historical check only
     * <p>
     * Underlying gora field should not use name 'args' which is already used,
     * see GProtocolStatus.args and GParseStatus.args
     */
    @NotNull
    public String getArgs() {
        // Underlying gora field should not use name 'args' which is already used.
        CharSequence args = page.getParams();
        return args != null ? args.toString() : "";
    }

    /**
     * Set the arguments and clear the LoadOptions object.
     * */
    public void setArgs(@NotNull String args) {
        variables.remove(VAR_LOAD_OPTIONS);
        page.setParams(args);
    }

    @NotNull
    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(@NotNull Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Set a field loader, the loader takes a parameter as the field name,
     * and returns a GWebPage containing the field.
     * */
    public void setLazyFieldLoader(Function<String, GWebPage> lazyFieldLoader) {
        this.lazyFieldLoader = lazyFieldLoader;
    }

    public int getMaxRetries() {
        return getMetadata().getInt(Name.FETCH_MAX_RETRY, 3);
    }

    public void setMaxRetries(int maxRetries) {
        getMetadata().set(Name.FETCH_MAX_RETRY, maxRetries);
    }

    @NotNull
    public String getConfiguredUrl() {
        return UrlUtils.mergeUrlArgs(url, getArgs());
    }

    public int getFetchedLinkCount() {
        return getMetadata().getInt(Name.FETCHED_LINK_COUNT, 0);
    }

    public void setFetchedLinkCount(int count) {
        getMetadata().set(Name.FETCHED_LINK_COUNT, count);
    }

    @NotNull
    public ZoneId getZoneId() {
        return page.getZoneId() == null ? DateTimes.INSTANCE.getZoneId() : ZoneId.of(page.getZoneId().toString());
    }

    public void setZoneId(@NotNull ZoneId zoneId) {
        page.setZoneId(zoneId.getId());
    }

    @Nullable
    public String getBatchId() {
        return page.getBatchId() == null ? null : page.getBatchId().toString();
    }

    public void setBatchId(@Nullable String value) {
        // TODO: use Utf8 which is an optimized string
//        page.setBatchId(Utf8(value));

        page.setBatchId(value);
    }

    /**
     * Mark this page as a seed where a crawl job starts from.
     * */
    public void markSeed() {
        getMetadata().set(Name.IS_SEED, YES_STRING);
    }

    /**
     * Unmark this page to be a seed.
     * */
    public void unmarkSeed() {
        getMetadata().remove(Name.IS_SEED);
    }

    /**
     * Check whether this page is a seed.
     * */
    public boolean isSeed() {
        return getMetadata().contains(Name.IS_SEED);
    }

    /**
     * Get the distance of the page from the seed in the graph.
     * */
    public int getDistance() {
        int distance = page.getDistance();
        return distance < 0 ? DISTANCE_INFINITE : distance;
    }

    /**
     * Set the distance of the page from the seed in the graph.
     * */
    public void setDistance(int newDistance) {
        page.setDistance(newDistance);
    }

    /**
     * Get the fetch mode, only BROWSER mode is supported currently.
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch.
     */
    @NotNull
    public FetchMode getFetchMode() {
        return FetchMode.fromString(getMetadata().get(Name.FETCH_MODE));
    }

    /**
     * Get the fetch mode, only BROWSER mode is supported currently.
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    public void setFetchMode(@NotNull FetchMode mode) {
        getMetadata().set(Name.FETCH_MODE, mode.name());
    }

    /**
     * Get the browser used to fetch the page last time.
     */
    @NotNull
    public BrowserType getLastBrowser() {
        String browser = page.getBrowser() != null ? page.getBrowser().toString() : "";
        return BrowserType.fromString(browser);
    }

    /**
     * Set the browser used to fetch the page.
     */
    public void setLastBrowser(@NotNull BrowserType browser) {
        page.setBrowser(browser.name());
    }

    /**
     * Checks whether the page is a single resource which can be fetched by a single request.
     */
    public boolean isResource() {
        return page.getResource() != null;
    }

    /**
     * Indicates the page to be a single resource that can be fetched by a single request.
     */
    public void setResource(boolean resource) {
        if (resource) {
            page.setResource(1);
        }
    }

    @NotNull
    public HtmlIntegrity getHtmlIntegrity() {
        String integrity = page.getHtmlIntegrity() != null ? page.getHtmlIntegrity().toString() : "";
        return HtmlIntegrity.Companion.fromString(integrity);
    }

    public void setHtmlIntegrity(@NotNull HtmlIntegrity integrity) {
        page.setHtmlIntegrity(integrity.name());
    }

    public int getFetchPriority() {
        return page.getFetchPriority() > 0 ? page.getFetchPriority() : FETCH_PRIORITY_DEFAULT;
    }

    public void setFetchPriority(int priority) {
        page.setFetchPriority(priority);
    }

    public int sniffFetchPriority() {
        int priority = getFetchPriority();

        int depth = getDistance();
        if (depth < FETCH_PRIORITY_DEPTH_BASE) {
            priority = Math.max(priority, FETCH_PRIORITY_DEPTH_BASE - depth);
        }

        return priority;
    }

    @NotNull
    public Instant getCreateTime() {
        return Instant.ofEpochMilli(page.getCreateTime());
    }

    public void setCreateTime(@NotNull Instant createTime) {
        page.setCreateTime(createTime.toEpochMilli());
    }

    @NotNull
    public Instant getGenerateTime() {
        String generateTime = getMetadata().get(Name.GENERATE_TIME);
        if (generateTime == null) {
            return Instant.EPOCH;
        } else {
            return Instant.parse(generateTime);
        }
    }

    public void setGenerateTime(@NotNull Instant generateTime) {
        getMetadata().set(Name.GENERATE_TIME, generateTime.toString());
    }

    public int getFetchCount() {
        return page.getFetchCount();
    }

    public void setFetchCount(int count) {
        page.setFetchCount(count);
    }

    public void updateFetchCount() {
        int count = getFetchCount();
        setFetchCount(count + 1);
    }

    @NotNull
    public CrawlStatus getCrawlStatus() {
        return new CrawlStatus(page.getCrawlStatus().byteValue());
    }

    public void setCrawlStatus(@NotNull CrawlStatus crawlStatus) {
        page.setCrawlStatus(crawlStatus.getCode());
    }

    public void setCrawlStatus(int value) {
        page.setCrawlStatus(value);
    }

    /**
     * The URL where the HTML was retrieved from, to resolve relative links against.
     * <p>
     * A baseUrl has the same semantic with Jsoup.parse:
     *
     * @return a {@link String} object.
     * @see <a href="https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse">Jsoup.parse</a>
     * @see WebPage#getLocation
     */
    public String getBaseUrl() {
        return page.getBaseUrl() == null ? "" : page.getBaseUrl().toString();
    }

    /**
     * Get the URL this Document was parsed from. If the starting URL is a redirect,
     * this will return the final URL from which the document was served from.
     * <p>
     * WebPage.url is the permanent internal address, it might not still available to access the target.
     * And WebPage.location or WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url which is generally normalized.
     * <p>
     * TODO: location is usually not the same as baseUrl, set it properly
     */
    public String getLocation() {
        return getBaseUrl();
    }

    /**
     * The url is the permanent internal address, it might not still available to access the target.
     * <p>
     * Location is the last working address, it might redirect to url, or it might have additional random parameters.
     * <p>
     * Location may be different from url, it's generally normalized.
     *
     * TODO: location is usually not the same as baseUrl, set it properly
     *
     * @param location The location.
     */
    public void setLocation(@NotNull String location) {
        page.setBaseUrl(location);
    }

    /**
     * The latest fetch time
     *
     * @return The latest fetch time
     */
    @NotNull
    public Instant getFetchTime() {
        return Instant.ofEpochMilli(page.getFetchTime());
    }

    /**
     * The latest fetch time
     *
     * @param time The latest fetch time
     */
    public void setFetchTime(@NotNull Instant time) {
        page.setFetchTime(time.toEpochMilli());
    }

    /**
     * The previous fetch time, updated at the fetch stage
     *
     * @return The previous fetch time.
     */
    @NotNull
    public Instant getPrevFetchTime() {
        return Instant.ofEpochMilli(page.getPrevFetchTime());
    }

    public void setPrevFetchTime(@NotNull Instant time) {
        page.setPrevFetchTime(time.toEpochMilli());
    }

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     * */
    @NotNull
    public Instant getPrevCrawlTime1() {
        return Instant.ofEpochMilli(page.getPrevCrawlTime1());
    }

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     * */
    public void setPrevCrawlTime1(@NotNull Instant time) {
        page.setPrevCrawlTime1(time.toEpochMilli());
    }

    /**
     * Get fetch interval
     * */
    @NotNull
    public Duration getFetchInterval() {
        long seconds = page.getFetchInterval();
        if (seconds < 0) {
            seconds = ChronoUnit.CENTURIES.getDuration().getSeconds();
        }
        return Duration.ofSeconds(seconds);
    }

    /**
     * Set fetch interval
     * */
    public void setFetchInterval(@NotNull Duration duration) {
        page.setFetchInterval((int) duration.getSeconds());
    }

    /**
     * Set fetch interval in seconds
     * */
    public void setFetchInterval(long seconds) {
        page.setFetchInterval((int) seconds);
    }

    /**
     * Set fetch interval in seconds
     * */
    public void setFetchInterval(float seconds) {
        page.setFetchInterval(Math.round(seconds));
    }

    /**
     * Get protocol status
     * */
    @NotNull
    public ProtocolStatus getProtocolStatus() {
        GProtocolStatus protocolStatus = page.getProtocolStatus();
        if (protocolStatus == null) {
            protocolStatus = GProtocolStatus.newBuilder().build();
        }
        return ProtocolStatus.box(protocolStatus);
    }

    /**
     * Set protocol status
     * */
    public void setProtocolStatus(@NotNull ProtocolStatus protocolStatus) {
        page.setProtocolStatus(protocolStatus.unbox());
    }

    /**
     * Header information returned from the web server used to server the content which is subsequently fetched from.
     * This includes keys such as
     * TRANSFER_ENCODING,
     * CONTENT_ENCODING,
     * CONTENT_LANGUAGE,
     * CONTENT_LENGTH,
     * CONTENT_LOCATION,
     * CONTENT_DISPOSITION,
     * CONTENT_MD5,
     * CONTENT_TYPE,
     * LAST_MODIFIED
     * and LOCATION.
     */
    @NotNull
    public ProtocolHeaders getHeaders() {
        return ProtocolHeaders.box(page.getHeaders());
    }

    @NotNull
    public String getReprUrl() {
        return page.getReprUrl() == null ? "" : page.getReprUrl().toString();
    }

    public void setReprUrl(@NotNull String value) {
        page.setReprUrl(value);
    }

    public int getFetchRetries() {
        return page.getFetchRetries();
    }

    public void setFetchRetries(int value) {
        page.setFetchRetries(value);
    }

    @NotNull
    public Instant getModifiedTime() {
        return Instant.ofEpochMilli(page.getModifiedTime());
    }

    public void setModifiedTime(@NotNull Instant value) {
        page.setModifiedTime(value.toEpochMilli());
    }

    @NotNull
    public Instant getPrevModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevModifiedTime());
    }

    public void setPrevModifiedTime(@NotNull Instant value) {
        page.setPrevModifiedTime(value.toEpochMilli());
    }

    @NotNull
    public String getFetchTimeHistory(@NotNull String defaultValue) {
        String s = getMetadata().get(Name.FETCH_TIME_HISTORY);
        return s == null ? defaultValue : s;
    }

    @NotNull
    public PageCategory getPageCategory() {
        try {
            CharSequence pageCategory = page.getPageCategory();
            if (pageCategory != null) {
                return PageCategory.parse(pageCategory.toString());
            }
        } catch (Throwable ignored) {
        }

        return PageCategory.UNKNOWN;
    }

    @NotNull
    public OpenPageCategory getOpenPageCategory() {
        try {
            CharSequence pageCategory = page.getPageCategory();
            if (pageCategory != null) {
                return OpenPageCategory.Companion.parse(pageCategory.toString());
            }
        } catch (Throwable ignored) {
        }

        return new OpenPageCategory("", "");
    }

    /**
     * category : index, detail, review, media, search, etc
     *
     * @param pageCategory a {@link PageCategory} object.
     */
    public void setPageCategory(@NotNull PageCategory pageCategory) {
        page.setPageCategory(pageCategory.format());
    }

    public void setPageCategory(@NotNull OpenPageCategory pageCategory) {
        page.setPageCategory(pageCategory.format());
    }

    /**
     * Get the encoding of the content.
     * Content encoding is detected just before it's parsed.
     * <p>
     * Not used if fetch mode is browser since the page content retrieved from a browser will always be UTF-8.
     */
    @Nullable
    public String getEncoding() {
        return page.getEncoding() == null ? null : page.getEncoding().toString();
    }

    /**
     * Set the encoding of the content.
     * Content encoding is detected just before it's parsed.
     * <p>
     * Not used if fetch mode is browser since the page content retrieved from a browser will always be UTF-8.
     */
    public void setEncoding(@Nullable String encoding) {
        page.setEncoding(encoding);
    }

    /**
     * The entire raw document content e.g. raw XHTML
     *
     * @return The raw document content in {@link ByteBuffer}.
     */
    @Nullable
    public ByteBuffer getContent() {
        if (tmpContent != null) {
            return tmpContent;
        }

        return getPersistContent();
    }

    /**
     * Get the cached content
     */
    @Nullable
    public ByteBuffer getTmpContent() {
        return tmpContent;
    }

    /**
     * Set the cached content, keep the persisted page content unmodified
     */
    public void setTmpContent(ByteBuffer tmpContent) {
        this.tmpContent = tmpContent;
    }

    /**
     * Get the persisted page content
     */
    @Nullable
    public ByteBuffer getPersistContent() {
        synchronized (CONTENT_MONITOR) {
            String fieldName = GWebPage.Field.CONTENT.getName();
            // load content lazily
            if (page.getContent() == null && lazyFieldLoader != null && !lazyLoadedFields.contains(fieldName)) {
                lazyLoadedFields.add(fieldName);
                GWebPage lazyPage = lazyFieldLoader.apply(fieldName);
                page.setContent(lazyPage.getContent());
            }

            return page.getContent();
        }
    }

    /**
     * Get content as bytes, the underling buffer is duplicated
     *
     * @return a duplication of the underling buffer.
     */
    @NotNull
    public byte[] getContentAsBytes() {
        ByteBuffer content = getContent();
        if (content == null) {
            return ByteUtils.toBytes('\0');
        }
        return ByteUtils.toBytes(content);
    }

    /**
     * Get the page content as a string, if the underlying page content is null, return an empty string
     */
    @NotNull
    public String getContentAsString() {
        ByteBuffer buffer = getContent();
        if (buffer == null || buffer.remaining() == 0) {
            return "";
        }

        return new String(buffer.array(), buffer.arrayOffset(), buffer.limit());
    }

    /**
     * Get the page content as input stream
     */
    @NotNull
    public ByteArrayInputStream getContentAsInputStream() {
        ByteBuffer contentInOctets = getContent();
        if (contentInOctets == null) {
            return new ByteArrayInputStream(ByteUtils.toBytes('\0'));
        }

        return new ByteArrayInputStream(getContent().array(),
                contentInOctets.arrayOffset() + contentInOctets.position(),
                contentInOctets.remaining());
    }

    /**
     * Get the page content as sax input source
     */
    @NotNull
    public InputSource getContentAsSaxInputSource() {
        InputSource inputSource = new InputSource(getContentAsInputStream());
        String encoding = getEncoding();
        if (encoding != null) {
            inputSource.setEncoding(encoding);
        }
        return inputSource;
    }

    /**
     * Set the page content
     */
    public void setContent(@Nullable String value) {
        if (value != null) {
            setContent(value.getBytes());
        } else {
            setContent((ByteBuffer) null);
        }
    }

    /**
     * Set the page content
     */
    public void setContent(@Nullable byte[] value) {
        if (value != null) {
            setContent(ByteBuffer.wrap(value));
        } else {
            setContent((ByteBuffer) null);
        }
    }

    /**
     * Set the page content.
     *
     * TODO: check consistency again
     *
     * @param value a ByteBuffer
     */
    public void setContent(@Nullable ByteBuffer value) {
        synchronized (CONTENT_MONITOR) {
            if (value != null) {
                page.setContent(value);
                isContentUpdated = true;

                long length = value.array().length;
                // save the length of the persisted content,
                // so we can query the length without loading the big or even huge content field
                setPersistedContentLength(length);

                length = getOriginalContentLength();
                if (length <= 0) {
                    // TODO: it's for old version compatible
                    length = value.array().length;
                }
                computeContentLength(length);
            } else {
                clearPersistContent();
            }
        }
    }

    /**
     * Clear persist content, so the content will not write to the disk.
     * */
    public void clearPersistContent() {
        synchronized (CONTENT_MONITOR) {
            tmpContent = page.getContent();
            page.setContent(null);
            setPersistedContentLength(0);
            // TODO: check consistency
            // lazyLoadedFields.remove(GWebPage.Field.CONTENT.getName());
        }
    }

    /**
     * Get the length of content in bytes.
     *
     * @return The length of the content in bytes.
     */
    public long getContentLength() {
        return page.getContentLength() != null ? page.getContentLength() : 0;
    }

    /**
     * Get the length of the original page content in bytes, the content has no pulsar metadata inserted.
     *
     * @return The length of the original page content in bytes, nagative means not specified
     * */
    public long getOriginalContentLength() {
        return getMetadata().getLong(Name.ORIGINAL_CONTENT_LENGTH, -1);
    }

    /**
     * Set the length of the original page content in bytes, the content has no pulsar metadata inserted.
     * @param length The length of the original page content in bytes, nagative means not specified
     * */
    public void setOriginalContentLength(int length) {
        getMetadata().set(Name.ORIGINAL_CONTENT_LENGTH, "" + length);
    }

    /**
     * Compute the length of content in bytes.
     */
    private void computeContentLength(long bytes) {
        long lastBytes = getContentLength();
        page.setLastContentLength(lastBytes);
        page.setContentLength(bytes);
        computeAveContentLength(bytes);
    }

    private void computeAveContentLength(long bytes) {
        int count = getFetchCount();
        long lastAveBytes = page.getAveContentLength();

        long aveBytes;
        if (count > 0 && lastAveBytes == 0) {
            // old version, average bytes is not calculated
            aveBytes = bytes;
        } else {
            aveBytes = (lastAveBytes * count + bytes) / (count + 1);
        }

        page.setAveContentLength(aveBytes);
    }

    public long getPersistedContentLength() {
        return page.getPersistedContentLength() != null ? page.getPersistedContentLength() : 0;
    }

    private void setPersistedContentLength(long bytes) {
        page.setPersistedContentLength(bytes);
    }

    public long getLastContentLength() {
        return page.getLastContentLength() != null ? page.getLastContentLength() : 0;
    }

    public long getAveContentLength() {
        return page.getAveContentLength() != null ? page.getAveContentLength() : 0;
    }

    @NotNull
    public String getContentType() {
        return page.getContentType() == null ? "" : page.getContentType().toString();
    }

    public void setContentType(String value) {
        page.setContentType(value.trim().toLowerCase());
    }

    @Nullable
    public ByteBuffer getPrevSignature() {
        return page.getPrevSignature();
    }

    public void setPrevSignature(@Nullable ByteBuffer value) {
        page.setPrevSignature(value);
    }

    @NotNull
    public String getPrevSignatureAsString() {
        ByteBuffer sig = getPrevSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return Strings.toHexString(sig);
    }

    /**
     * The last proxy used to fetch the page
     */
    public String getProxy() {
        return page.getProxy() == null ? null : page.getProxy().toString();
    }

    /**
     * The last proxy used to fetch the page
     */
    public void setProxy(@Nullable String proxy) {
        page.setProxy(proxy);
    }

    @Nullable
    public ActiveDOMStatus getActiveDOMStatus() {
        GActiveDOMStatus s = page.getActiveDOMStatus();
        if (s == null) return null;

        return new ActiveDOMStatus(
                s.getN(),
                s.getScroll(),
                s.getSt().toString(),
                s.getR().toString(),
                s.getIdl().toString(),
                s.getEc().toString()
        );
    }

    public void setActiveDOMStatus(@Nullable ActiveDOMStatus s) {
        if (s == null) {
            return;
        }

        GActiveDOMStatus s2 = page.getActiveDOMStatus();
        if (s2 != null) {
            s2.setN(s.getN());
            s2.setScroll(s.getScroll());
            s2.setSt(s.getSt());
            s2.setR(s.getR());
            s2.setIdl(s.getIdl());
            s2.setEc(s.getEc());
        }
    }

    @NotNull
    public Map<String, ActiveDOMStat> getActiveDOMStatTrace() {
        Map<CharSequence, GActiveDOMStat> s = page.getActiveDOMStatTrace();
        return s.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Converters.INSTANCE.convert(e.getValue())
        ));
    }

    public void setActiveDOMStatTrace(@NotNull Map<String, ActiveDOMStat> trace) {
        Map<CharSequence, GActiveDOMStat> statTrace = trace.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> Converters.INSTANCE.convert(e.getValue())));
        page.setActiveDOMStatTrace(statTrace);
    }

    /**
     * An implementation of a WebPage's signature from which it can be identified and referenced at any point in time.
     * This is essentially the WebPage's fingerprint representing its state for any point in time.
     */
    @Nullable
    public ByteBuffer getSignature() {
        return page.getSignature();
    }

    public void setSignature(byte[] value) {
        page.setSignature(ByteBuffer.wrap(value));
    }

    @NotNull
    public String getSignatureAsString() {
        ByteBuffer sig = getSignature();
        if (sig == null) {
            sig = ByteBuffer.wrap("".getBytes());
        }
        return Strings.toHexString(sig);
    }

    @NotNull
    public String getPageTitle() {
        return page.getPageTitle() == null ? "" : page.getPageTitle().toString();
    }

    public void setPageTitle(String pageTitle) {
        page.setPageTitle(pageTitle);
    }

    @NotNull
    public String getContentTitle() {
        return page.getContentTitle() == null ? "" : page.getContentTitle().toString();
    }

    public void setContentTitle(String contentTitle) {
        if (contentTitle != null) {
            page.setContentTitle(contentTitle);
        }
    }

    @NotNull
    public String getPageText() {
        return page.getPageText() == null ? "" : page.getPageText().toString();
    }

    public void setPageText(String value) {
        if (value != null && !value.isEmpty()) page.setPageText(value);
    }

    @NotNull
    public String getContentText() {
        return page.getContentText() == null ? "" : page.getContentText().toString();
    }

    public void setContentText(String textContent) {
        if (textContent != null && !textContent.isEmpty()) {
            page.setContentText(textContent);
            page.setContentTextLen(textContent.length());
        }
    }

    public int getContentTextLen() {
        return page.getContentTextLen();
    }

    @NotNull
    public ParseStatus getParseStatus() {
        GParseStatus parseStatus = page.getParseStatus();
        return ParseStatus.box(parseStatus == null ? GParseStatus.newBuilder().build() : parseStatus);
    }

    public void setParseStatus(ParseStatus parseStatus) {
        page.setParseStatus(parseStatus.unbox());
    }

    public Map<CharSequence, GHypeLink> getLiveLinks() {
        return page.getLiveLinks();
    }

    public Collection<String> getSimpleLiveLinks() {
        return CollectionUtils.collect(page.getLiveLinks().keySet(), CharSequence::toString);
    }

    public void setLiveLinks(Iterable<HyperlinkPersistable> liveLinks) {
        page.getLiveLinks().clear();
        Map<CharSequence, GHypeLink> links = page.getLiveLinks();
        liveLinks.forEach(l -> links.put(l.getUrl(), l.unbox()));
    }

    public void setLiveLinks(Map<CharSequence, GHypeLink> links) {
        page.setLiveLinks(links);
    }

    public void addLiveLink(HyperlinkPersistable hyperLink) {
        page.getLiveLinks().put(hyperLink.getUrl(), hyperLink.unbox());
    }

    public Map<CharSequence, CharSequence> getVividLinks() {
        return page.getVividLinks();
    }

    public Collection<String> getSimpleVividLinks() {
        return CollectionUtils.collect(page.getVividLinks().keySet(), CharSequence::toString);
    }

    public void setVividLinks(Map<CharSequence, CharSequence> links) {
        page.setVividLinks(links);
    }

    public List<CharSequence> getDeadLinks() {
        return page.getDeadLinks();
    }

    public void setDeadLinks(List<CharSequence> deadLinks) {
        page.setDeadLinks(deadLinks);
    }

    public List<CharSequence> getLinks() {
        return page.getLinks();
    }

    public void setLinks(List<CharSequence> links) {
        page.setLinks(links);
    }

    public int getImpreciseLinkCount() {
        return getMetadata().getInt(Name.TOTAL_OUT_LINKS, 0);
    }

    public void setImpreciseLinkCount(int count) {
        getMetadata().set(Name.TOTAL_OUT_LINKS, String.valueOf(count));
    }

    public Map<CharSequence, CharSequence> getInlinks() {
        return page.getInlinks();
    }

    @NotNull
    public CharSequence getAnchor() {
        return page.getAnchor() != null ? page.getAnchor() : "";
    }

    public void setAnchor(CharSequence anchor) {
        page.setAnchor(anchor);
    }

    public String[] getInlinkAnchors() {
        return StringUtils.split(getMetadata().getOrDefault(Name.ANCHOR_COUNT, ""), "\n");
    }

    public void setInlinkAnchors(Collection<CharSequence> anchors) {
        getMetadata().set(Name.ANCHOR_COUNT, StringUtils.join(anchors, "\n"));
    }

    public int getAnchorOrder() {
        return page.getAnchorOrder() < 0 ? MAX_LIVE_LINK_PER_PAGE : page.getAnchorOrder();
    }

    public void setAnchorOrder(int order) {
        page.setAnchorOrder(order);
    }

    public Instant getContentPublishTime() {
        return Instant.ofEpochMilli(page.getContentPublishTime());
    }

    public void setContentPublishTime(Instant publishTime) {
        page.setContentPublishTime(publishTime.toEpochMilli());
    }

    public boolean isValidContentModifyTime(Instant publishTime) {
        return publishTime.isAfter(MIN_ARTICLE_PUBLISH_TIME);
    }

    public Instant getPrevContentPublishTime() {
        return Instant.ofEpochMilli(page.getPrevContentPublishTime());
    }

    public void setPrevContentPublishTime(Instant publishTime) {
        page.setPrevContentPublishTime(publishTime.toEpochMilli());
    }

    public Instant getRefContentPublishTime() {
        return Instant.ofEpochMilli(page.getRefContentPublishTime());
    }

    public void setRefContentPublishTime(Instant publishTime) {
        page.setRefContentPublishTime(publishTime.toEpochMilli());
    }

    public Instant getContentModifiedTime() {
        return Instant.ofEpochMilli(page.getContentModifiedTime());
    }

    public void setContentModifiedTime(Instant modifiedTime) {
        page.setContentModifiedTime(modifiedTime.toEpochMilli());
    }

    public Instant getPrevContentModifiedTime() {
        return Instant.ofEpochMilli(page.getPrevContentModifiedTime());
    }

    public void setPrevContentModifiedTime(Instant modifiedTime) {
        page.setPrevContentModifiedTime(modifiedTime.toEpochMilli());
    }

    public Instant getPrevRefContentPublishTime() {
        return Instant.ofEpochMilli(page.getPrevRefContentPublishTime());
    }

    public void setPrevRefContentPublishTime(Instant publishTime) {
        page.setPrevRefContentPublishTime(publishTime.toEpochMilli());
    }

    @Nullable
    public String getReferrer() {
        return page.getReferrer() == null ? null : page.getReferrer().toString();
    }

    public void setReferrer(@Nullable String referrer) {
        if (UrlUtils.isStandard(referrer)) {
            page.setReferrer(referrer);
        }
    }

    /**
     * *****************************************************************************
     * Page Model
     * ******************************************************************************
     */
    @Nullable
    public Instant getPageModelUpdateTime() {
        return Instant.ofEpochMilli(page.getPageModelUpdateTime());
    }

    public void setPageModelUpdateTime(@Nullable Instant time) {
        page.setPageModelUpdateTime(time == null ? 0 : time.toEpochMilli());
    }

    @Nullable
    public PageModel getPageModel() {
        synchronized (PAGE_MODEL_MONITOR) {
            String fieldName = GWebPage.Field.PAGE_MODEL.getName();
            // load content lazily
            if (page.getPageModel() == null && lazyFieldLoader != null && !lazyLoadedFields.contains(fieldName)) {
                lazyLoadedFields.add(fieldName);
                GWebPage lazyPage = lazyFieldLoader.apply(fieldName);
                page.setPageModel(lazyPage.getPageModel());
            }

            return page.getPageModel() == null ? null : PageModel.box(page.getPageModel());
        }
    }

    @NotNull
    public PageModel ensurePageModel() {
        synchronized (PAGE_MODEL_MONITOR) {
            if (page.getPageModel() == null) {
                page.setPageModel(GPageModel.newBuilder().build());
            }

            return Objects.requireNonNull(getPageModel());
        }
    }

    /**
     * *****************************************************************************
     * Scoring
     * ******************************************************************************
     */
    public float getScore() {
        return page.getScore();
    }

    public void setScore(float value) {
        page.setScore(value);
    }

    public float getContentScore() {
        return page.getContentScore() == null ? 0.0f : page.getContentScore();
    }

    public void setContentScore(float score) {
        page.setContentScore(score);
    }

    @NotNull
    public String getSortScore() {
        return page.getSortScore() == null ? "" : page.getSortScore().toString();
    }

    public void setSortScore(String score) {
        page.setSortScore(score);
    }

    public float getCash() {
        return getMetadata().getFloat(Name.CASH_KEY, 0.0f);
    }

    public void setCash(float cash) {
        getMetadata().set(Name.CASH_KEY, String.valueOf(cash));
    }

    @NotNull
    public PageCounters getPageCounters() {
        return PageCounters.box(page.getPageCounters());
    }

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     */

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public int compareTo(@NotNull WebPage o) {
        return url.compareTo(o.url);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof WebPage && ((WebPage) other).url.equals(url);
    }

    @Override
    public String toString() {
        return new WebPageFormatter(this).format();
    }
}
