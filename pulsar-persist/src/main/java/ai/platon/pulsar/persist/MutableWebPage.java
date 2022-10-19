package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.persist.metadata.OpenPageCategory;
import ai.platon.pulsar.persist.metadata.PageCategory;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import ai.platon.pulsar.persist.model.PageModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MutableWebPage extends WebPage {

    /**
     * Set The hypertext reference of this page.
     * It defines the address of the document, which this time is linked from
     *
     * @param href The hypertext reference
     */
    void setHref(@Nullable String href);

    /**
     * *****************************************************************************
     * Common fields
     * ******************************************************************************
     */

    /**
     * Retrieves and removes the local variable with the given name.
     */
    Object removeVar(@NotNull String name);

    /**
     * Get a page scope temporary variable
     *
     * @param name  The variable name.
     * @param value The variable value.
     */
    void setVar(@NotNull String name, @NotNull Object value);

    void setCached(boolean cached);

    void setLoaded(boolean loaded);

    void setFetched(boolean fetched);

    /**
     * Check if the page is canceled.
     * <p>
     * If a page is canceled, it should not be updated.
     */
    void setCanceled(boolean canceled);

    void setConf(@NotNull VolatileConfig conf);

    /**
     * Set the arguments and clear the LoadOptions object.
     */
    void setArgs(@NotNull String args);

    void setRetryDelay(@NotNull Duration retryDelay);

    void setMaxRetries(int maxRetries);

    void setFetchedLinkCount(int count);

    void setZoneId(@NotNull ZoneId zoneId);

    void setBatchId(String value);

    void markSeed();

    void unmarkSeed();

    void setDistance(int newDistance);

    /**
     * Fetch mode is used to determine the protocol before fetch, so it shall be set before fetch
     */
    void setFetchMode(@NotNull FetchMode mode);

    void setLastBrowser(@NotNull BrowserType browser);

    void setResource(boolean resource);

    void setHtmlIntegrity(@NotNull HtmlIntegrity integrity);

    void setFetchPriority(int priority);

    void setCreateTime(@NotNull Instant createTime);

    void setGenerateTime(@NotNull Instant generateTime);

    void setFetchCount(int count);

    void updateFetchCount();

    void setCrawlStatus(@NotNull CrawlStatus crawlStatus);

    void setCrawlStatus(int value);

    /**
     * The url is the permanent internal address, it might not still available to access the target.
     * <p>
     * Location is the last working address, it might redirect to url, or it might have additional random parameters.
     * <p>
     * Location may be different from url, it's generally normalized.
     *
     * @param location The location.
     */
    void setLocation(@NotNull String location);

    /**
     * The latest fetch time
     *
     * @param time The latest fetch time
     */
    void setFetchTime(@NotNull Instant time);

    void setPrevFetchTime(@NotNull Instant time);

    /**
     * The previous crawl time, used for fat link crawl, which means both the page itself and out pages are fetched
     */
    void setPrevCrawlTime1(@NotNull Instant time);

    /**
     * Set fetch interval
     */
    void setFetchInterval(@NotNull Duration duration);

    /**
     * Set fetch interval in seconds
     */
    void setFetchInterval(long seconds);

    /**
     * Set fetch interval in seconds
     */
    void setFetchInterval(float seconds);

    /**
     * Set protocol status
     */
    void setProtocolStatus(@NotNull ProtocolStatus protocolStatus);

    void setReprUrl(String value);

    void setFetchRetries(int value);

    void setModifiedTime(@NotNull Instant value);

    void setPrevModifiedTime(@NotNull Instant value);

    /**
     * category : index, detail, review, media, search, etc
     *
     * @param pageCategory a {@link PageCategory} object.
     */
    void setPageCategory(@NotNull PageCategory pageCategory);

    void setPageCategory(@NotNull OpenPageCategory pageCategory);

    /**
     * Set the encoding of the content.
     * Content encoding is detected just before it's parsed.
     */
    void setEncoding(@Nullable String encoding);

    /**
     * The clues are used to determine the encoding of the page content
     */
    void setEncodingClues(@NotNull String clues);

    /**
     * Set the cached content, keep the persisted page content unmodified
     */
    void setTmpContent(ByteBuffer tmpContent);

    /**
     * Set the page content
     */
    void setContent(@Nullable String value);

    /**
     * Set the page content
     */
    void setContent(@Nullable byte[] value);

    /**
     * Set the page content
     *
     * @param value a ByteBuffer.
     */
    void setContent(@Nullable ByteBuffer value);

    void clearPersistContent();

    void setContentType(String value);

    void setPrevSignature(@Nullable ByteBuffer value);

    /**
     * The last proxy used to fetch the page
     */
    void setProxy(@Nullable String proxy);

    void setActiveDOMStatTrace(@NotNull Map<String, ActiveDOMStat> trace);

    void setActiveDOMStatus(@Nullable ActiveDOMStatus s);

    void setSignature(byte[] value);

    void setPageTitle(String pageTitle);

    void setContentTitle(String contentTitle);

    void setPageText(String value);

    void setContentText(String textContent);

    void setParseStatus(ParseStatus parseStatus);

    void setLiveLinks(Iterable<HyperlinkPersistable> liveLinks);

    void setLiveLinks(Map<CharSequence, GHypeLink> links);

    void addLiveLink(HyperlinkPersistable hyperLink);

    void setVividLinks(Map<CharSequence, CharSequence> links);

    void setDeadLinks(List<CharSequence> deadLinks);

    void setLinks(List<CharSequence> links);

    void setImpreciseLinkCount(int count);

    void setAnchor(CharSequence anchor);

    void setInlinkAnchors(Collection<CharSequence> anchors);

    void setAnchorOrder(int order);

    void setContentPublishTime(Instant publishTime);

    void setPrevContentPublishTime(Instant publishTime);

    void setRefContentPublishTime(Instant publishTime);

    void setContentModifiedTime(Instant modifiedTime);

    void setPrevContentModifiedTime(Instant modifiedTime);

    void setPrevRefContentPublishTime(Instant publishTime);

    void setReferrer(@Nullable String referrer);

    void setPageModelUpdateTime(@Nullable Instant time);

    void setPageModel(@NotNull PageModel pageModel);

    PageModel ensurePageModel();

    /**
     * *****************************************************************************
     * Scoring
     * ******************************************************************************
     */
    void setScore(float value);

    void setContentScore(float score);

    void setSortScore(String score);

    void setCash(float cash);

    /**
     * *****************************************************************************
     * Index
     * ******************************************************************************
     */
}
