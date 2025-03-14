package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.Mark;
import ai.platon.pulsar.persist.metadata.OpenPageCategory;
import ai.platon.pulsar.persist.metadata.PageCategory;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import ai.platon.pulsar.persist.model.PageModel;
import org.xml.sax.InputSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface WebPage extends Comparable<WebPage> {

    // Getters and Setters

    int getId();

    String getUrl();

    String getKey();

    String getReversedUrl();

    @Nullable String getHref();

    void setHref(@Nullable String href);

    boolean isNil();

    boolean isNotNil();

    boolean isInternal();

    boolean isNotInternal();

    GWebPage unbox();

    void unsafeSetGPage(@Nonnull GWebPage page);

    void unsafeCloneGPage(WebPage page);

    Variables getVariables();

    boolean hasVar(@Nonnull String name);

    @Nullable
    Object getVar(@Nonnull String name);

    @Nullable
    Object getVar(Class<?> clazz);

    Object removeVar(@Nonnull String name);

    void setVar(@Nonnull String name, @Nonnull Object value);

    Object getBean(Class<?> clazz);

    @Nullable
    Object getBeanOrNull(@Nonnull Class<?> clazz);

    <T> void putBean(@Nonnull T bean);

    @Nullable
    Object data(@Nonnull String name);

    void data(@Nonnull String name, @Nullable Object value);

    @Nullable
    PageDatum getPageDatum();

    void setPageDatum(PageDatum pageDatum);

    boolean isCached();

    void setCached(boolean cached);

    boolean isLoaded();

    void setLoaded(boolean loaded);

    boolean isFetched();

    void setFetched(boolean fetched);

    boolean isCanceled();

    void setCanceled(boolean canceled);

    boolean isContentUpdated();

    @Nonnull
    VolatileConfig getConf();

    void setConf(@Nonnull VolatileConfig conf);

    Metadata getMetadata();

    CrawlMarks getMarks();

    boolean hasMark(Mark mark);

    @Nonnull
    String getArgs();

    void setArgs(@Nonnull String args);

    @Nonnull
    Duration getRetryDelay();

    void setRetryDelay(@Nonnull Duration retryDelay);

    void setLazyFieldLoader(Function<String, GWebPage> lazyFieldLoader);

    int getMaxRetries();

    void setMaxRetries(int maxRetries);

    @Nonnull
    String getConfiguredUrl();

    int getFetchedLinkCount();

    void setFetchedLinkCount(int count);

    @Nonnull
    ZoneId getZoneId();

    void setZoneId(@Nonnull ZoneId zoneId);

    @Nullable
    String getBatchId();

    void setBatchId(@Nullable String value);

    void markSeed();

    void unmarkSeed();

    boolean isSeed();

    int getDistance();

    void setDistance(int newDistance);

    @Nonnull
    FetchMode getFetchMode();

    void setFetchMode(@Nonnull FetchMode mode);

    @Nonnull
    BrowserType getLastBrowser();

    void setLastBrowser(@Nonnull BrowserType browser);

    boolean isResource();

    void setResource(boolean resource);

    @Nonnull
    HtmlIntegrity getHtmlIntegrity();

    void setHtmlIntegrity(@Nonnull HtmlIntegrity integrity);

    int getFetchPriority();

    void setFetchPriority(int priority);

    @Nonnull
    Instant getCreateTime();

    void setCreateTime(@Nonnull Instant createTime);

    @Nonnull
    Instant getGenerateTime();

    void setGenerateTime(@Nonnull Instant generateTime);

    int getFetchCount();

    void setFetchCount(int count);

    void updateFetchCount();

    @Nonnull
    CrawlStatus getCrawlStatus();

    void setCrawlStatus(@Nonnull CrawlStatus crawlStatus);

    void setCrawlStatus(int value);

    String getBaseUrl();

    String getLocation();

    void setLocation(@Nonnull String location);

    @Nonnull
    Instant getFetchTime();

    void setFetchTime(@Nonnull Instant time);

    @Nonnull
    Instant getPrevFetchTime();

    void setPrevFetchTime(@Nonnull Instant time);

    @Nonnull
    Instant getPrevCrawlTime1();

    void setPrevCrawlTime1(@Nonnull Instant time);

    @Nonnull
    Duration getFetchInterval();

    void setFetchInterval(@Nonnull Duration duration);

    void setFetchInterval(long seconds);

    void setFetchInterval(float seconds);

    @Nonnull
    ProtocolStatus getProtocolStatus();

    void setProtocolStatus(@Nonnull ProtocolStatus protocolStatus);

    @Nonnull
    ProtocolHeaders getHeaders();

    @Nonnull
    String getReprUrl();

    void setReprUrl(@Nonnull String value);

    int getFetchRetries();

    void setFetchRetries(int value);

    @Nonnull
    Instant getModifiedTime();

    void setModifiedTime(@Nonnull Instant value);

    @Nonnull
    Instant getPrevModifiedTime();

    void setPrevModifiedTime(@Nonnull Instant value);

    @Nonnull
    String getFetchTimeHistory(@Nonnull String defaultValue);

    @Nonnull
    PageCategory getPageCategory();

    @Nonnull
    OpenPageCategory getOpenPageCategory();

    void setPageCategory(@Nonnull PageCategory pageCategory);

    void setPageCategory(@Nonnull OpenPageCategory pageCategory);

    @Nullable
    String getEncoding();

    void setEncoding(@Nullable String encoding);

    @Nullable
    ByteBuffer getContent();

    @Nullable
    ByteBuffer getTmpContent();

    void setTmpContent(ByteBuffer tmpContent);

    @Nullable
    ByteBuffer getPersistContent();

    @Nonnull
    byte[] getContentAsBytes();

    @Nonnull
    String getContentAsString();

    @Nonnull
    ByteArrayInputStream getContentAsInputStream();

    @Nonnull
    InputSource getContentAsSaxInputSource();

    void setContent(@Nullable String value);

    void setContent(@Nullable byte[] value);

    void setContent(@Nullable ByteBuffer value);

    void clearPersistContent();

    long getContentLength();

    long getOriginalContentLength();

    void setOriginalContentLength(int length);

    long getPersistedContentLength();

    long getLastContentLength();

    long getAveContentLength();

    @Nonnull
    String getContentType();

    void setContentType(String value);

    @Nullable
    ByteBuffer getPrevSignature();

    void setPrevSignature(@Nullable ByteBuffer value);

    @Nonnull
    String getPrevSignatureAsString();

    String getProxy();

    void setProxy(@Nullable String proxy);

    @Nullable
    ActiveDOMStatus getActiveDOMStatus();

    void setActiveDOMStatus(@Nullable ActiveDOMStatus s);

    @Nonnull
    Map<String, ActiveDOMStat> getActiveDOMStatTrace();

    void setActiveDOMStatTrace(@Nonnull Map<String, ActiveDOMStat> trace);

    @Nullable
    ByteBuffer getSignature();

    void setSignature(byte[] value);

    @Nonnull
    String getSignatureAsString();

    @Nonnull
    String getPageTitle();

    void setPageTitle(String pageTitle);

    @Nonnull
    String getContentTitle();

    void setContentTitle(String contentTitle);

    @Nonnull
    String getPageText();

    void setPageText(String value);

    @Nonnull
    String getContentText();

    @Nonnull
    ParseStatus getParseStatus();

    void setParseStatus(ParseStatus parseStatus);

    @Deprecated
    Map<CharSequence, GHypeLink> getLiveLinks();

    Map<CharSequence, CharSequence> getVividLinks();

    List<CharSequence> getLinks();

    void setLinks(List<CharSequence> links);

    Map<CharSequence, CharSequence> getInlinks();

    @Nonnull
    CharSequence getAnchor();

    void setAnchor(CharSequence anchor);

    boolean isValidContentModifyTime(Instant publishTime);

    @Nullable
    String getReferrer();

    void setReferrer(@Nullable String referrer);

    @Nullable
    Instant getPageModelUpdateTime();

    void setPageModelUpdateTime(@Nullable Instant time);

    @Nullable
    PageModel getPageModel();

    @Nonnull
    PageModel ensurePageModel();
}
