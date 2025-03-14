package ai.platon.pulsar.persist.experimental;

import ai.platon.pulsar.common.HtmlIntegrity;
import ai.platon.pulsar.common.browser.BrowserType;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.persist.*;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.OpenPageCategory;
import ai.platon.pulsar.persist.metadata.PageCategory;
import ai.platon.pulsar.persist.model.ActiveDOMStat;
import ai.platon.pulsar.persist.model.ActiveDOMStatus;
import ai.platon.pulsar.persist.model.PageModel;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
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

  Variables getVariables();
  Object getVar(String name);
  VolatileConfig getConf();

  Metadata getMetadata();
  CrawlMarks getMarks();
  ProtocolHeaders getHeaders();

  Instant getCreateTime();
  int getDistance();
  int getFetchCount();
  CrawlStatus getCrawlStatus();

  Instant getFetchTime();
  Instant getPrevFetchTime();
  Duration getFetchInterval();
  ProtocolStatus getProtocolStatus();

  int getFetchRetries();
  Instant getModifiedTime();
  Instant getPrevModifiedTime();
  PageCategory getPageCategory();
  OpenPageCategory getOpenPageCategory();
  ByteBuffer getPrevSignature();
  String getProxy();
  ActiveDOMStatus getActiveDOMStatus();
  Map<String, ActiveDOMStat> getActiveDOMStatTrace();

  Duration getRetryDelay();
  int getMaxRetries();

  FetchMode getFetchMode();
  BrowserType getLastBrowser();

  String getEncoding();

  HtmlIntegrity getHtmlIntegrity();

  String getPageTitle();
  String getPageText();

  String getContentType();
  long getContentLength();

  long getPersistedContentLength();
  long getLastContentLength();

  ByteBuffer getTmpContent();
  ByteBuffer getContent();
  ByteBuffer getPersistContent();
  byte[] getContentAsBytes();
  String getContentAsString();
  ByteArrayInputStream getContentAsInputStream();

  ByteBuffer getSignature();
  String getPrevSignatureAsString();
  String getSignatureAsString();

  ParseStatus getParseStatus();

  Instant getPageModelUpdateTime();
  PageModel getPageModel();
}
