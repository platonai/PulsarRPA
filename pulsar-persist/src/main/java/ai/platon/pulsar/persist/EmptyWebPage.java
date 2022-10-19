package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.OpenPageCategory;
import ai.platon.pulsar.persist.metadata.PageCategory;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import ai.platon.pulsar.persist.model.PageModel;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EmptyWebPage implements WebPage {

    @Override
    public boolean isNil() {
        return false;
    }

    @Override
    public boolean isNotNil() {
        return false;
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public boolean isNotInternal() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public boolean isCached() {
        return false;
    }
    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public boolean isFetched() {
        return false;
    }

    @Override
    public boolean isContentUpdated() {
        return false;
    }

    @Override
    public Variables getVariables() {
        return null;
    }

    @Override
    public Object getVar(String name) {
        return null;
    }

    @Override
    public boolean hasVar(String name) {
        return false;
    }

    @Override
    public VolatileConfig getConf() {
        return null;
    }

    @Override
    public String getProxy() {
        return null;
    }

    @Override
    public boolean isSeed() {
        return false;
    }

    @Override
    public boolean hasMark(String mark) {
        return false;
    }

    @Override
    public PageCounters getPageCounters() {
        return null;
    }

    @Override
    public int getMaxRetries() {
        return 0;
    }

    @Override
    public FetchMode getFetchMode() {
        return null;
    }

    @Override
    public BrowserType getLastBrowser() {
        return null;
    }

    @Override
    public boolean isResource() {
        return false;
    }

    @Override
    public ActiveDOMStatus getActiveDOMStatus() {
        return null;
    }

    @Override
    public Map<String, ActiveDOMStat> getActiveDOMStatTrace() {
        return null;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getReversedUrl() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getBaseUrl() {
        return null;
    }

    @Override
    public String getArgs() {
        return null;
    }

    @Override
    public String getConfiguredUrl() {
        return null;
    }

    @Override
    public String getReprUrl() {
        return null;
    }

    @Override
    public String getReferrer() {
        return null;
    }

    @Override
    public String getHref() {
        return null;
    }

    @Override
    public Metadata getMetadata() {
        return null;
    }

    @Override
    public CrawlMarks getMarks() {
        return null;
    }

    @Override
    public ProtocolHeaders getHeaders() {
        return null;
    }

    @Override
    public Instant getCreateTime() {
        return null;
    }

    @Override
    public ZoneId getZoneId() {
        return null;
    }

    @Override
    public String getBatchId() {
        return null;
    }

    @Override
    public int getDistance() {
        return 0;
    }

    @Override
    public int getFetchPriority() {
        return 0;
    }

    @Override
    public int getFetchCount() {
        return 0;
    }

    @Override
    public CrawlStatus getCrawlStatus() {
        return null;
    }

    @Override
    public Instant getFetchTime() {
        return null;
    }

    @Override
    public Instant getPrevFetchTime() {
        return null;
    }

    @Override
    public Instant getPrevCrawlTime1() {
        return null;
    }

    @Override
    public Duration getFetchInterval() {
        return null;
    }

    @Override
    public ProtocolStatus getProtocolStatus() {
        return null;
    }

    @Override
    public int getFetchRetries() {
        return 0;
    }

    @Override
    public Instant getModifiedTime() {
        return null;
    }

    @Override
    public Instant getPrevModifiedTime() {
        return null;
    }

    @Override
    public PageCategory getPageCategory() {
        return null;
    }

    @Override
    public ByteBuffer getPrevSignature() {
        return null;
    }

    @Override
    public Instant getContentPublishTime() {
        return null;
    }

    @Override
    public Instant getPrevContentPublishTime() {
        return null;
    }

    @Override
    public Instant getRefContentPublishTime() {
        return null;
    }

    @Override
    public Instant getContentModifiedTime() {
        return null;
    }

    @Override
    public Instant getPrevContentModifiedTime() {
        return null;
    }

    @Override
    public Instant getPrevRefContentPublishTime() {
        return null;
    }

    @Override
    public Duration getRetryDelay() {
        return null;
    }

    @Override
    public int getFetchedLinkCount() {
        return 0;
    }

    @Override
    public Instant getGenerateTime() {
        return null;
    }

    @Override
    public OpenPageCategory getOpenPageCategory() {
        return null;
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public String getEncodingClues() {
        return null;
    }

    @Override
    public String getPageTitle() {
        return null;
    }

    @Override
    public String getContentTitle() {
        return null;
    }

    @Override
    public String getPageText() {
        return null;
    }

    @Override
    public String getContentText() {
        return null;
    }

    @Override
    public int getContentTextLen() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public long getAveContentLength() {
        return 0;
    }

    @Override
    public long getPersistedContentLength() {
        return 0;
    }

    @Override
    public long getLastContentLength() {
        return 0;
    }

    @Override
    public ByteBuffer getTmpContent() {
        return null;
    }

    @Override
    public ByteBuffer getContent() {
        return null;
    }

    @Override
    public ByteBuffer getPersistContent() {
        return null;
    }

    @Override
    public byte[] getContentAsBytes() {
        return new byte[0];
    }

    @Override
    public String getContentAsString() {
        return null;
    }

    @Override
    public InputSource getContentAsSaxInputSource() {
        return null;
    }

    @Override
    public ByteArrayInputStream getContentAsInputStream() {
        return null;
    }

    @Override
    public ByteBuffer getSignature() {
        return null;
    }

    @Override
    public String getPrevSignatureAsString() {
        return null;
    }

    @Override
    public String getSignatureAsString() {
        return null;
    }

    @NotNull
    @Override
    public ParseStatus getParseStatus() {
        return null;
    }

    @Override
    public float getCash() {
        return 0;
    }

    @Override
    public float getScore() {
        return 0;
    }

    @Override
    public float getContentScore() {
        return 0;
    }

    @Override
    public String getSortScore() {
        return null;
    }

    @Override
    public Instant getPageModelUpdateTime() {
        return null;
    }

    @Override
    public PageModel getPageModel() {
        return null;
    }

    @Override
    public HtmlIntegrity getHtmlIntegrity() {
        return null;
    }

    @Override
    public Map<CharSequence, GHypeLink> getLiveLinks() {
        return null;
    }

    @Override
    public Collection<String> getSimpleLiveLinks() {
        return null;
    }

    @Override
    public void addLiveLink(HyperlinkPersistable hyperLink) {

    }

    @Override
    public Map<CharSequence, CharSequence> getVividLinks() {
        return null;
    }

    @Override
    public Collection<String> getSimpleVividLinks() {
        return null;
    }

    @Override
    public List<CharSequence> getDeadLinks() {
        return null;
    }

    @Override
    public List<CharSequence> getLinks() {
        return null;
    }

    @Override
    public int getImpreciseLinkCount() {
        return 0;
    }

    @Override
    public Map<CharSequence, CharSequence> getInlinks() {
        return null;
    }

    @NotNull
    @Override
    public CharSequence getAnchor() {
        return null;
    }

    @Override
    public String[] getInlinkAnchors() {
        return new String[0];
    }

    @Override
    public int getAnchorOrder() {
        return 0;
    }

    @Override
    public int compareTo(@NotNull WebPage webPage) {
        return 0;
    }
}
