package ai.platon.pulsar.persist.experimental;

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.persist.*;
import ai.platon.pulsar.persist.gora.generated.GHypeLink;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.OpenPageCategory;
import ai.platon.pulsar.persist.metadata.PageCategory;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import ai.platon.pulsar.persist.model.PageModel;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WebAsset {
  int getId();
  String getUrl();
  String getKey();
  String getReversedUrl();
  String getLocation();
  String getBaseUrl();
  String getArgs();
  String getConfiguredUrl();
  String getReprUrl();
  String getReferrer();
  String getHref();

  Metadata getMetadata();
  CrawlMarks getMarks();
  ProtocolHeaders getHeaders();

  boolean isResource();

  Instant getCreateTime();
  ZoneId getZoneId();
  String getBatchId();
  int getDistance();
  int getFetchPriority();
  int getFetchCount();
  CrawlStatus getCrawlStatus();

  Instant getFetchTime();
  Instant getPrevFetchTime();
  Instant getPrevCrawlTime1();
  Duration getFetchInterval();
  ProtocolStatus getProtocolStatus();

  int getFetchRetries();
  Instant getModifiedTime();
  Instant getPrevModifiedTime();
  PageCategory getPageCategory();
  ByteBuffer getPrevSignature();

  Instant getContentPublishTime();
  Instant getPrevContentPublishTime();
  Instant getRefContentPublishTime();
  Instant getContentModifiedTime();
  Instant getPrevContentModifiedTime();
  Instant getPrevRefContentPublishTime();

  Duration getRetryDelay();
  int getFetchedLinkCount();
  Instant getGenerateTime();
  OpenPageCategory getOpenPageCategory();
  String getEncoding();
  String getEncodingClues();
  String getPageTitle();
  String getContentTitle();
  String getPageText();
  String getContentText();
  int getContentTextLen();

  String getContentType();
  long getContentLength();

  long getAveContentLength();
  long getPersistedContentLength();
  long getLastContentLength();

  ByteBuffer getTmpContent();
  ByteBuffer getContent();
  ByteBuffer getPersistContent();
  byte[] getContentAsBytes();
  String getContentAsString();
  InputSource getContentAsSaxInputSource();
  ByteArrayInputStream getContentAsInputStream();

  ByteBuffer getSignature();
  String getPrevSignatureAsString();
  String getSignatureAsString();

  ParseStatus getParseStatus();

  float getCash();
  float getScore();
  float getContentScore();
  String getSortScore();

  Instant getPageModelUpdateTime();
  PageModel getPageModel();
}
