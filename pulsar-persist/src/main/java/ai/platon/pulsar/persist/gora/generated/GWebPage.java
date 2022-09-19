/**
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the"
 *License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at
 *
  * http://www.apache.org/licenses/LICENSE-2.0
 * 
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */
package ai.platon.pulsar.persist.gora.generated;  

public class GWebPage extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"GWebPage\",\"namespace\":\"ai.platon.pulsar.persist.gora.generated\",\"fields\":[{\"name\":\"baseUrl\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"createTime\",\"type\":\"long\",\"default\":0},{\"name\":\"distance\",\"type\":\"int\",\"default\":-1},{\"name\":\"fetchCount\",\"type\":\"int\",\"default\":0},{\"name\":\"fetchPriority\",\"type\":\"int\",\"default\":0},{\"name\":\"fetchInterval\",\"type\":\"int\",\"default\":864000000},{\"name\":\"zoneId\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"params\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"batchId\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"resource\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"crawlStatus\",\"type\":\"int\",\"default\":0},{\"name\":\"browser\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"proxy\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"prevFetchTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevCrawlTime1\",\"type\":\"long\",\"default\":0},{\"name\":\"fetchTime\",\"type\":\"long\",\"default\":0},{\"name\":\"fetchRetries\",\"type\":\"int\",\"default\":0},{\"name\":\"reprUrl\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"prevModifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"modifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"protocolStatus\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"GProtocolStatus\",\"fields\":[{\"name\":\"majorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"minorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"args\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}}]}],\"default\":null},{\"name\":\"encoding\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentType\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"content\",\"type\":[\"null\",\"bytes\"],\"doc\":\"The entire raw document content e.g. raw XHTML\",\"default\":null},{\"name\":\"contentLength\",\"type\":\"long\",\"default\":0},{\"name\":\"lastContentLength\",\"type\":\"long\",\"default\":0},{\"name\":\"aveContentLength\",\"type\":\"long\",\"default\":0},{\"name\":\"persistedContentLength\",\"type\":\"long\",\"default\":0},{\"name\":\"referrer\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"htmlIntegrity\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"anchor\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"anchorOrder\",\"type\":\"int\",\"default\":-1},{\"name\":\"parseStatus\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"GParseStatus\",\"fields\":[{\"name\":\"majorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"minorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"args\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}}]}],\"default\":null},{\"name\":\"pageTitle\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"pageText\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentTitle\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentText\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentTextLen\",\"type\":\"int\",\"default\":0},{\"name\":\"pageCategory\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentModifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevContentModifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"contentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevContentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"refContentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevRefContentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"pageModelUpdateTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevSignature\",\"type\":[\"null\",\"bytes\"],\"default\":null},{\"name\":\"signature\",\"type\":[\"null\",\"bytes\"],\"default\":null},{\"name\":\"contentScore\",\"type\":\"float\",\"default\":0},{\"name\":\"score\",\"type\":\"float\",\"default\":0},{\"name\":\"sortScore\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"pageCounters\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"int\"]},\"default\":{}},{\"name\":\"headers\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"links\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":[]},{\"name\":\"deadLinks\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":[]},{\"name\":\"liveLinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",{\"type\":\"record\",\"name\":\"GHypeLink\",\"fields\":[{\"name\":\"url\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"anchor\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"order\",\"type\":\"int\",\"default\":0}]}]},\"default\":[]},{\"name\":\"vividLinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"inlinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"markers\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"metadata\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"bytes\"]},\"default\":{}},{\"name\":\"activeDOMStatus\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"GActiveDOMStatus\",\"fields\":[{\"name\":\"n\",\"type\":\"int\",\"default\":0},{\"name\":\"scroll\",\"type\":\"int\",\"default\":0},{\"name\":\"st\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"r\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"idl\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"ec\",\"type\":[\"null\",\"string\"],\"default\":null}]}],\"default\":null},{\"name\":\"activeDOMStatTrace\",\"type\":{\"type\":\"map\",\"values\":[\"null\",{\"type\":\"record\",\"name\":\"GActiveDOMStat\",\"fields\":[{\"name\":\"ni\",\"type\":\"int\",\"default\":0},{\"name\":\"na\",\"type\":\"int\",\"default\":0},{\"name\":\"nnm\",\"type\":\"int\",\"default\":0},{\"name\":\"nst\",\"type\":\"int\",\"default\":0},{\"name\":\"w\",\"type\":\"int\",\"default\":0},{\"name\":\"h\",\"type\":\"int\",\"default\":0}]}]},\"default\":null},{\"name\":\"pageModel\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"GFieldGroup\",\"fields\":[{\"name\":\"id\",\"type\":\"long\",\"default\":0},{\"name\":\"parentId\",\"type\":\"long\",\"default\":0},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"fields\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}}]}},\"default\":[]}]}");
  private static final long serialVersionUID = 1768432174987969493L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    BASE_URL(0, "baseUrl"),
    CREATE_TIME(1, "createTime"),
    DISTANCE(2, "distance"),
    FETCH_COUNT(3, "fetchCount"),
    FETCH_PRIORITY(4, "fetchPriority"),
    FETCH_INTERVAL(5, "fetchInterval"),
    ZONE_ID(6, "zoneId"),
    PARAMS(7, "params"),
    BATCH_ID(8, "batchId"),
    RESOURCE(9, "resource"),
    CRAWL_STATUS(10, "crawlStatus"),
    BROWSER(11, "browser"),
    PROXY(12, "proxy"),
    PREV_FETCH_TIME(13, "prevFetchTime"),
    PREV_CRAWL_TIME1(14, "prevCrawlTime1"),
    FETCH_TIME(15, "fetchTime"),
    FETCH_RETRIES(16, "fetchRetries"),
    REPR_URL(17, "reprUrl"),
    PREV_MODIFIED_TIME(18, "prevModifiedTime"),
    MODIFIED_TIME(19, "modifiedTime"),
    PROTOCOL_STATUS(20, "protocolStatus"),
    ENCODING(21, "encoding"),
    CONTENT_TYPE(22, "contentType"),
    CONTENT(23, "content"),
    CONTENT_LENGTH(24, "contentLength"),
    LAST_CONTENT_LENGTH(25, "lastContentLength"),
    AVE_CONTENT_LENGTH(26, "aveContentLength"),
    PERSISTED_CONTENT_LENGTH(27, "persistedContentLength"),
    REFERRER(28, "referrer"),
    HTML_INTEGRITY(29, "htmlIntegrity"),
    ANCHOR(30, "anchor"),
    ANCHOR_ORDER(31, "anchorOrder"),
    PARSE_STATUS(32, "parseStatus"),
    PAGE_TITLE(33, "pageTitle"),
    PAGE_TEXT(34, "pageText"),
    CONTENT_TITLE(35, "contentTitle"),
    CONTENT_TEXT(36, "contentText"),
    CONTENT_TEXT_LEN(37, "contentTextLen"),
    PAGE_CATEGORY(38, "pageCategory"),
    CONTENT_MODIFIED_TIME(39, "contentModifiedTime"),
    PREV_CONTENT_MODIFIED_TIME(40, "prevContentModifiedTime"),
    CONTENT_PUBLISH_TIME(41, "contentPublishTime"),
    PREV_CONTENT_PUBLISH_TIME(42, "prevContentPublishTime"),
    REF_CONTENT_PUBLISH_TIME(43, "refContentPublishTime"),
    PREV_REF_CONTENT_PUBLISH_TIME(44, "prevRefContentPublishTime"),
    PAGE_MODEL_UPDATE_TIME(45, "pageModelUpdateTime"),
    PREV_SIGNATURE(46, "prevSignature"),
    SIGNATURE(47, "signature"),
    CONTENT_SCORE(48, "contentScore"),
    SCORE(49, "score"),
    SORT_SCORE(50, "sortScore"),
    PAGE_COUNTERS(51, "pageCounters"),
    HEADERS(52, "headers"),
    LINKS(53, "links"),
    DEAD_LINKS(54, "deadLinks"),
    LIVE_LINKS(55, "liveLinks"),
    VIVID_LINKS(56, "vividLinks"),
    INLINKS(57, "inlinks"),
    MARKERS(58, "markers"),
    METADATA(59, "metadata"),
    ACTIVE_DOMSTATUS(60, "activeDOMStatus"),
    ACTIVE_DOMSTAT_TRACE(61, "activeDOMStatTrace"),
    PAGE_MODEL(62, "pageModel"),
    ;
    /**
     * Field's index.
     */
    private int index;

    /**
     * Field's name.
     */
    private String name;

    /**
     * Field's constructor
     * @param index field's index.
     * @param name field's name.
     */
    Field(int index, String name) {this.index=index;this.name=name;}

    /**
     * Gets field's index.
     * @return int field's index.
     */
    public int getIndex() {return index;}

    /**
     * Gets field's name.
     * @return String field's name.
     */
    public String getName() {return name;}

    /**
     * Gets field's attributes to string.
     * @return String field's attributes to string.
     */
    public String toString() {return name;}
  };

  public static final String[] _ALL_FIELDS = {
  "baseUrl",
  "createTime",
  "distance",
  "fetchCount",
  "fetchPriority",
  "fetchInterval",
  "zoneId",
  "params",
  "batchId",
  "resource",
  "crawlStatus",
  "browser",
  "proxy",
  "prevFetchTime",
  "prevCrawlTime1",
  "fetchTime",
  "fetchRetries",
  "reprUrl",
  "prevModifiedTime",
  "modifiedTime",
  "protocolStatus",
  "encoding",
  "contentType",
  "content",
  "contentLength",
  "lastContentLength",
  "aveContentLength",
  "persistedContentLength",
  "referrer",
  "htmlIntegrity",
  "anchor",
  "anchorOrder",
  "parseStatus",
  "pageTitle",
  "pageText",
  "contentTitle",
  "contentText",
  "contentTextLen",
  "pageCategory",
  "contentModifiedTime",
  "prevContentModifiedTime",
  "contentPublishTime",
  "prevContentPublishTime",
  "refContentPublishTime",
  "prevRefContentPublishTime",
  "pageModelUpdateTime",
  "prevSignature",
  "signature",
  "contentScore",
  "score",
  "sortScore",
  "pageCounters",
  "headers",
  "links",
  "deadLinks",
  "liveLinks",
  "vividLinks",
  "inlinks",
  "markers",
  "metadata",
  "activeDOMStatus",
  "activeDOMStatTrace",
  "pageModel",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return GWebPage._ALL_FIELDS.length;
  }

  private java.lang.CharSequence baseUrl;
  private long createTime;
  private int distance;
  private int fetchCount;
  private int fetchPriority;
  private int fetchInterval;
  private java.lang.CharSequence zoneId;
  private java.lang.CharSequence params;
  private java.lang.CharSequence batchId;
  private java.lang.Integer resource;
  private int crawlStatus;
  private java.lang.CharSequence browser;
  private java.lang.CharSequence proxy;
  private long prevFetchTime;
  private long prevCrawlTime1;
  private long fetchTime;
  private int fetchRetries;
  private java.lang.CharSequence reprUrl;
  private long prevModifiedTime;
  private long modifiedTime;
  private ai.platon.pulsar.persist.gora.generated.GProtocolStatus protocolStatus;
  private java.lang.CharSequence encoding;
  private java.lang.CharSequence contentType;
  /** The entire raw document content e.g. raw XHTML */
  private java.nio.ByteBuffer content;
  private long contentLength;
  private long lastContentLength;
  private long aveContentLength;
  private long persistedContentLength;
  private java.lang.CharSequence referrer;
  private java.lang.CharSequence htmlIntegrity;
  private java.lang.CharSequence anchor;
  private int anchorOrder;
  private ai.platon.pulsar.persist.gora.generated.GParseStatus parseStatus;
  private java.lang.CharSequence pageTitle;
  private java.lang.CharSequence pageText;
  private java.lang.CharSequence contentTitle;
  private java.lang.CharSequence contentText;
  private int contentTextLen;
  private java.lang.CharSequence pageCategory;
  private long contentModifiedTime;
  private long prevContentModifiedTime;
  private long contentPublishTime;
  private long prevContentPublishTime;
  private long refContentPublishTime;
  private long prevRefContentPublishTime;
  private long pageModelUpdateTime;
  private java.nio.ByteBuffer prevSignature;
  private java.nio.ByteBuffer signature;
  private float contentScore;
  private float score;
  private java.lang.CharSequence sortScore;
  private java.util.Map<java.lang.CharSequence,java.lang.Integer> pageCounters;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> headers;
  private java.util.List<java.lang.CharSequence> links;
  private java.util.List<java.lang.CharSequence> deadLinks;
  private java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> liveLinks;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> vividLinks;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> inlinks;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> markers;
  private java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> metadata;
  private ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus activeDOMStatus;
  private java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> activeDOMStatTrace;
  private java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> pageModel;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return baseUrl;
    case 1: return createTime;
    case 2: return distance;
    case 3: return fetchCount;
    case 4: return fetchPriority;
    case 5: return fetchInterval;
    case 6: return zoneId;
    case 7: return params;
    case 8: return batchId;
    case 9: return resource;
    case 10: return crawlStatus;
    case 11: return browser;
    case 12: return proxy;
    case 13: return prevFetchTime;
    case 14: return prevCrawlTime1;
    case 15: return fetchTime;
    case 16: return fetchRetries;
    case 17: return reprUrl;
    case 18: return prevModifiedTime;
    case 19: return modifiedTime;
    case 20: return protocolStatus;
    case 21: return encoding;
    case 22: return contentType;
    case 23: return content;
    case 24: return contentLength;
    case 25: return lastContentLength;
    case 26: return aveContentLength;
    case 27: return persistedContentLength;
    case 28: return referrer;
    case 29: return htmlIntegrity;
    case 30: return anchor;
    case 31: return anchorOrder;
    case 32: return parseStatus;
    case 33: return pageTitle;
    case 34: return pageText;
    case 35: return contentTitle;
    case 36: return contentText;
    case 37: return contentTextLen;
    case 38: return pageCategory;
    case 39: return contentModifiedTime;
    case 40: return prevContentModifiedTime;
    case 41: return contentPublishTime;
    case 42: return prevContentPublishTime;
    case 43: return refContentPublishTime;
    case 44: return prevRefContentPublishTime;
    case 45: return pageModelUpdateTime;
    case 46: return prevSignature;
    case 47: return signature;
    case 48: return contentScore;
    case 49: return score;
    case 50: return sortScore;
    case 51: return pageCounters;
    case 52: return headers;
    case 53: return links;
    case 54: return deadLinks;
    case 55: return liveLinks;
    case 56: return vividLinks;
    case 57: return inlinks;
    case 58: return markers;
    case 59: return metadata;
    case 60: return activeDOMStatus;
    case 61: return activeDOMStatTrace;
    case 62: return pageModel;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: baseUrl = (java.lang.CharSequence)(value); break;
    case 1: createTime = (java.lang.Long)(value); break;
    case 2: distance = (java.lang.Integer)(value); break;
    case 3: fetchCount = (java.lang.Integer)(value); break;
    case 4: fetchPriority = (java.lang.Integer)(value); break;
    case 5: fetchInterval = (java.lang.Integer)(value); break;
    case 6: zoneId = (java.lang.CharSequence)(value); break;
    case 7: params = (java.lang.CharSequence)(value); break;
    case 8: batchId = (java.lang.CharSequence)(value); break;
    case 9: resource = (java.lang.Integer)(value); break;
    case 10: crawlStatus = (java.lang.Integer)(value); break;
    case 11: browser = (java.lang.CharSequence)(value); break;
    case 12: proxy = (java.lang.CharSequence)(value); break;
    case 13: prevFetchTime = (java.lang.Long)(value); break;
    case 14: prevCrawlTime1 = (java.lang.Long)(value); break;
    case 15: fetchTime = (java.lang.Long)(value); break;
    case 16: fetchRetries = (java.lang.Integer)(value); break;
    case 17: reprUrl = (java.lang.CharSequence)(value); break;
    case 18: prevModifiedTime = (java.lang.Long)(value); break;
    case 19: modifiedTime = (java.lang.Long)(value); break;
    case 20: protocolStatus = (ai.platon.pulsar.persist.gora.generated.GProtocolStatus)(value); break;
    case 21: encoding = (java.lang.CharSequence)(value); break;
    case 22: contentType = (java.lang.CharSequence)(value); break;
    case 23: content = (java.nio.ByteBuffer)(value); break;
    case 24: contentLength = (java.lang.Long)(value); break;
    case 25: lastContentLength = (java.lang.Long)(value); break;
    case 26: aveContentLength = (java.lang.Long)(value); break;
    case 27: persistedContentLength = (java.lang.Long)(value); break;
    case 28: referrer = (java.lang.CharSequence)(value); break;
    case 29: htmlIntegrity = (java.lang.CharSequence)(value); break;
    case 30: anchor = (java.lang.CharSequence)(value); break;
    case 31: anchorOrder = (java.lang.Integer)(value); break;
    case 32: parseStatus = (ai.platon.pulsar.persist.gora.generated.GParseStatus)(value); break;
    case 33: pageTitle = (java.lang.CharSequence)(value); break;
    case 34: pageText = (java.lang.CharSequence)(value); break;
    case 35: contentTitle = (java.lang.CharSequence)(value); break;
    case 36: contentText = (java.lang.CharSequence)(value); break;
    case 37: contentTextLen = (java.lang.Integer)(value); break;
    case 38: pageCategory = (java.lang.CharSequence)(value); break;
    case 39: contentModifiedTime = (java.lang.Long)(value); break;
    case 40: prevContentModifiedTime = (java.lang.Long)(value); break;
    case 41: contentPublishTime = (java.lang.Long)(value); break;
    case 42: prevContentPublishTime = (java.lang.Long)(value); break;
    case 43: refContentPublishTime = (java.lang.Long)(value); break;
    case 44: prevRefContentPublishTime = (java.lang.Long)(value); break;
    case 45: pageModelUpdateTime = (java.lang.Long)(value); break;
    case 46: prevSignature = (java.nio.ByteBuffer)(value); break;
    case 47: signature = (java.nio.ByteBuffer)(value); break;
    case 48: contentScore = (java.lang.Float)(value); break;
    case 49: score = (java.lang.Float)(value); break;
    case 50: sortScore = (java.lang.CharSequence)(value); break;
    case 51: pageCounters = (java.util.Map<java.lang.CharSequence,java.lang.Integer>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 52: headers = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 53: links = (java.util.List<java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 54: deadLinks = (java.util.List<java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 55: liveLinks = (java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 56: vividLinks = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 57: inlinks = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 58: markers = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 59: metadata = (java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 60: activeDOMStatus = (ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus)(value); break;
    case 61: activeDOMStatTrace = (java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 62: pageModel = (java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'baseUrl' field.
   */
  public java.lang.CharSequence getBaseUrl() {
    return baseUrl;
  }

  /**
   * Sets the value of the 'baseUrl' field.
   * @param value the value to set.
   */
  public void setBaseUrl(java.lang.CharSequence value) {
    this.baseUrl = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'baseUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isBaseUrlDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'createTime' field.
   */
  public java.lang.Long getCreateTime() {
    return createTime;
  }

  /**
   * Sets the value of the 'createTime' field.
   * @param value the value to set.
   */
  public void setCreateTime(java.lang.Long value) {
    this.createTime = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'createTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isCreateTimeDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'distance' field.
   */
  public java.lang.Integer getDistance() {
    return distance;
  }

  /**
   * Sets the value of the 'distance' field.
   * @param value the value to set.
   */
  public void setDistance(java.lang.Integer value) {
    this.distance = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'distance' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDistanceDirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'fetchCount' field.
   */
  public java.lang.Integer getFetchCount() {
    return fetchCount;
  }

  /**
   * Sets the value of the 'fetchCount' field.
   * @param value the value to set.
   */
  public void setFetchCount(java.lang.Integer value) {
    this.fetchCount = value;
    setDirty(3);
  }
  
  /**
   * Checks the dirty status of the 'fetchCount' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isFetchCountDirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'fetchPriority' field.
   */
  public java.lang.Integer getFetchPriority() {
    return fetchPriority;
  }

  /**
   * Sets the value of the 'fetchPriority' field.
   * @param value the value to set.
   */
  public void setFetchPriority(java.lang.Integer value) {
    this.fetchPriority = value;
    setDirty(4);
  }
  
  /**
   * Checks the dirty status of the 'fetchPriority' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isFetchPriorityDirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'fetchInterval' field.
   */
  public java.lang.Integer getFetchInterval() {
    return fetchInterval;
  }

  /**
   * Sets the value of the 'fetchInterval' field.
   * @param value the value to set.
   */
  public void setFetchInterval(java.lang.Integer value) {
    this.fetchInterval = value;
    setDirty(5);
  }
  
  /**
   * Checks the dirty status of the 'fetchInterval' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isFetchIntervalDirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'zoneId' field.
   */
  public java.lang.CharSequence getZoneId() {
    return zoneId;
  }

  /**
   * Sets the value of the 'zoneId' field.
   * @param value the value to set.
   */
  public void setZoneId(java.lang.CharSequence value) {
    this.zoneId = value;
    setDirty(6);
  }
  
  /**
   * Checks the dirty status of the 'zoneId' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isZoneIdDirty() {
    return isDirty(6);
  }

  /**
   * Gets the value of the 'params' field.
   */
  public java.lang.CharSequence getParams() {
    return params;
  }

  /**
   * Sets the value of the 'params' field.
   * @param value the value to set.
   */
  public void setParams(java.lang.CharSequence value) {
    this.params = value;
    setDirty(7);
  }
  
  /**
   * Checks the dirty status of the 'params' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isParamsDirty() {
    return isDirty(7);
  }

  /**
   * Gets the value of the 'batchId' field.
   */
  public java.lang.CharSequence getBatchId() {
    return batchId;
  }

  /**
   * Sets the value of the 'batchId' field.
   * @param value the value to set.
   */
  public void setBatchId(java.lang.CharSequence value) {
    this.batchId = value;
    setDirty(8);
  }
  
  /**
   * Checks the dirty status of the 'batchId' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isBatchIdDirty() {
    return isDirty(8);
  }

  /**
   * Gets the value of the 'resource' field.
   */
  public java.lang.Integer getResource() {
    return resource;
  }

  /**
   * Sets the value of the 'resource' field.
   * @param value the value to set.
   */
  public void setResource(java.lang.Integer value) {
    this.resource = value;
    setDirty(9);
  }
  
  /**
   * Checks the dirty status of the 'resource' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isResourceDirty() {
    return isDirty(9);
  }

  /**
   * Gets the value of the 'crawlStatus' field.
   */
  public java.lang.Integer getCrawlStatus() {
    return crawlStatus;
  }

  /**
   * Sets the value of the 'crawlStatus' field.
   * @param value the value to set.
   */
  public void setCrawlStatus(java.lang.Integer value) {
    this.crawlStatus = value;
    setDirty(10);
  }
  
  /**
   * Checks the dirty status of the 'crawlStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isCrawlStatusDirty() {
    return isDirty(10);
  }

  /**
   * Gets the value of the 'browser' field.
   */
  public java.lang.CharSequence getBrowser() {
    return browser;
  }

  /**
   * Sets the value of the 'browser' field.
   * @param value the value to set.
   */
  public void setBrowser(java.lang.CharSequence value) {
    this.browser = value;
    setDirty(11);
  }
  
  /**
   * Checks the dirty status of the 'browser' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isBrowserDirty() {
    return isDirty(11);
  }

  /**
   * Gets the value of the 'proxy' field.
   */
  public java.lang.CharSequence getProxy() {
    return proxy;
  }

  /**
   * Sets the value of the 'proxy' field.
   * @param value the value to set.
   */
  public void setProxy(java.lang.CharSequence value) {
    this.proxy = value;
    setDirty(12);
  }
  
  /**
   * Checks the dirty status of the 'proxy' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isProxyDirty() {
    return isDirty(12);
  }

  /**
   * Gets the value of the 'prevFetchTime' field.
   */
  public java.lang.Long getPrevFetchTime() {
    return prevFetchTime;
  }

  /**
   * Sets the value of the 'prevFetchTime' field.
   * @param value the value to set.
   */
  public void setPrevFetchTime(java.lang.Long value) {
    this.prevFetchTime = value;
    setDirty(13);
  }
  
  /**
   * Checks the dirty status of the 'prevFetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevFetchTimeDirty() {
    return isDirty(13);
  }

  /**
   * Gets the value of the 'prevCrawlTime1' field.
   */
  public java.lang.Long getPrevCrawlTime1() {
    return prevCrawlTime1;
  }

  /**
   * Sets the value of the 'prevCrawlTime1' field.
   * @param value the value to set.
   */
  public void setPrevCrawlTime1(java.lang.Long value) {
    this.prevCrawlTime1 = value;
    setDirty(14);
  }
  
  /**
   * Checks the dirty status of the 'prevCrawlTime1' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevCrawlTime1Dirty() {
    return isDirty(14);
  }

  /**
   * Gets the value of the 'fetchTime' field.
   */
  public java.lang.Long getFetchTime() {
    return fetchTime;
  }

  /**
   * Sets the value of the 'fetchTime' field.
   * @param value the value to set.
   */
  public void setFetchTime(java.lang.Long value) {
    this.fetchTime = value;
    setDirty(15);
  }
  
  /**
   * Checks the dirty status of the 'fetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isFetchTimeDirty() {
    return isDirty(15);
  }

  /**
   * Gets the value of the 'fetchRetries' field.
   */
  public java.lang.Integer getFetchRetries() {
    return fetchRetries;
  }

  /**
   * Sets the value of the 'fetchRetries' field.
   * @param value the value to set.
   */
  public void setFetchRetries(java.lang.Integer value) {
    this.fetchRetries = value;
    setDirty(16);
  }
  
  /**
   * Checks the dirty status of the 'fetchRetries' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isFetchRetriesDirty() {
    return isDirty(16);
  }

  /**
   * Gets the value of the 'reprUrl' field.
   */
  public java.lang.CharSequence getReprUrl() {
    return reprUrl;
  }

  /**
   * Sets the value of the 'reprUrl' field.
   * @param value the value to set.
   */
  public void setReprUrl(java.lang.CharSequence value) {
    this.reprUrl = value;
    setDirty(17);
  }
  
  /**
   * Checks the dirty status of the 'reprUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isReprUrlDirty() {
    return isDirty(17);
  }

  /**
   * Gets the value of the 'prevModifiedTime' field.
   */
  public java.lang.Long getPrevModifiedTime() {
    return prevModifiedTime;
  }

  /**
   * Sets the value of the 'prevModifiedTime' field.
   * @param value the value to set.
   */
  public void setPrevModifiedTime(java.lang.Long value) {
    this.prevModifiedTime = value;
    setDirty(18);
  }
  
  /**
   * Checks the dirty status of the 'prevModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevModifiedTimeDirty() {
    return isDirty(18);
  }

  /**
   * Gets the value of the 'modifiedTime' field.
   */
  public java.lang.Long getModifiedTime() {
    return modifiedTime;
  }

  /**
   * Sets the value of the 'modifiedTime' field.
   * @param value the value to set.
   */
  public void setModifiedTime(java.lang.Long value) {
    this.modifiedTime = value;
    setDirty(19);
  }
  
  /**
   * Checks the dirty status of the 'modifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isModifiedTimeDirty() {
    return isDirty(19);
  }

  /**
   * Gets the value of the 'protocolStatus' field.
   */
  public ai.platon.pulsar.persist.gora.generated.GProtocolStatus getProtocolStatus() {
    return protocolStatus;
  }

  /**
   * Sets the value of the 'protocolStatus' field.
   * @param value the value to set.
   */
  public void setProtocolStatus(ai.platon.pulsar.persist.gora.generated.GProtocolStatus value) {
    this.protocolStatus = value;
    setDirty(20);
  }
  
  /**
   * Checks the dirty status of the 'protocolStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isProtocolStatusDirty() {
    return isDirty(20);
  }

  /**
   * Gets the value of the 'encoding' field.
   */
  public java.lang.CharSequence getEncoding() {
    return encoding;
  }

  /**
   * Sets the value of the 'encoding' field.
   * @param value the value to set.
   */
  public void setEncoding(java.lang.CharSequence value) {
    this.encoding = value;
    setDirty(21);
  }
  
  /**
   * Checks the dirty status of the 'encoding' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isEncodingDirty() {
    return isDirty(21);
  }

  /**
   * Gets the value of the 'contentType' field.
   */
  public java.lang.CharSequence getContentType() {
    return contentType;
  }

  /**
   * Sets the value of the 'contentType' field.
   * @param value the value to set.
   */
  public void setContentType(java.lang.CharSequence value) {
    this.contentType = value;
    setDirty(22);
  }
  
  /**
   * Checks the dirty status of the 'contentType' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentTypeDirty() {
    return isDirty(22);
  }

  /**
   * Gets the value of the 'content' field.
   * The entire raw document content e.g. raw XHTML   */
  public java.nio.ByteBuffer getContent() {
    return content;
  }

  /**
   * Sets the value of the 'content' field.
   * The entire raw document content e.g. raw XHTML   * @param value the value to set.
   */
  public void setContent(java.nio.ByteBuffer value) {
    this.content = value;
    setDirty(23);
  }
  
  /**
   * Checks the dirty status of the 'content' field. A field is dirty if it represents a change that has not yet been written to the database.
   * The entire raw document content e.g. raw XHTML   * @param value the value to set.
   */
  public boolean isContentDirty() {
    return isDirty(23);
  }

  /**
   * Gets the value of the 'contentLength' field.
   */
  public java.lang.Long getContentLength() {
    return contentLength;
  }

  /**
   * Sets the value of the 'contentLength' field.
   * @param value the value to set.
   */
  public void setContentLength(java.lang.Long value) {
    this.contentLength = value;
    setDirty(24);
  }
  
  /**
   * Checks the dirty status of the 'contentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentLengthDirty() {
    return isDirty(24);
  }

  /**
   * Gets the value of the 'lastContentLength' field.
   */
  public java.lang.Long getLastContentLength() {
    return lastContentLength;
  }

  /**
   * Sets the value of the 'lastContentLength' field.
   * @param value the value to set.
   */
  public void setLastContentLength(java.lang.Long value) {
    this.lastContentLength = value;
    setDirty(25);
  }
  
  /**
   * Checks the dirty status of the 'lastContentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isLastContentLengthDirty() {
    return isDirty(25);
  }

  /**
   * Gets the value of the 'aveContentLength' field.
   */
  public java.lang.Long getAveContentLength() {
    return aveContentLength;
  }

  /**
   * Sets the value of the 'aveContentLength' field.
   * @param value the value to set.
   */
  public void setAveContentLength(java.lang.Long value) {
    this.aveContentLength = value;
    setDirty(26);
  }
  
  /**
   * Checks the dirty status of the 'aveContentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isAveContentLengthDirty() {
    return isDirty(26);
  }

  /**
   * Gets the value of the 'persistedContentLength' field.
   */
  public java.lang.Long getPersistedContentLength() {
    return persistedContentLength;
  }

  /**
   * Sets the value of the 'persistedContentLength' field.
   * @param value the value to set.
   */
  public void setPersistedContentLength(java.lang.Long value) {
    this.persistedContentLength = value;
    setDirty(27);
  }
  
  /**
   * Checks the dirty status of the 'persistedContentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPersistedContentLengthDirty() {
    return isDirty(27);
  }

  /**
   * Gets the value of the 'referrer' field.
   */
  public java.lang.CharSequence getReferrer() {
    return referrer;
  }

  /**
   * Sets the value of the 'referrer' field.
   * @param value the value to set.
   */
  public void setReferrer(java.lang.CharSequence value) {
    this.referrer = value;
    setDirty(28);
  }
  
  /**
   * Checks the dirty status of the 'referrer' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isReferrerDirty() {
    return isDirty(28);
  }

  /**
   * Gets the value of the 'htmlIntegrity' field.
   */
  public java.lang.CharSequence getHtmlIntegrity() {
    return htmlIntegrity;
  }

  /**
   * Sets the value of the 'htmlIntegrity' field.
   * @param value the value to set.
   */
  public void setHtmlIntegrity(java.lang.CharSequence value) {
    this.htmlIntegrity = value;
    setDirty(29);
  }
  
  /**
   * Checks the dirty status of the 'htmlIntegrity' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isHtmlIntegrityDirty() {
    return isDirty(29);
  }

  /**
   * Gets the value of the 'anchor' field.
   */
  public java.lang.CharSequence getAnchor() {
    return anchor;
  }

  /**
   * Sets the value of the 'anchor' field.
   * @param value the value to set.
   */
  public void setAnchor(java.lang.CharSequence value) {
    this.anchor = value;
    setDirty(30);
  }
  
  /**
   * Checks the dirty status of the 'anchor' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isAnchorDirty() {
    return isDirty(30);
  }

  /**
   * Gets the value of the 'anchorOrder' field.
   */
  public java.lang.Integer getAnchorOrder() {
    return anchorOrder;
  }

  /**
   * Sets the value of the 'anchorOrder' field.
   * @param value the value to set.
   */
  public void setAnchorOrder(java.lang.Integer value) {
    this.anchorOrder = value;
    setDirty(31);
  }
  
  /**
   * Checks the dirty status of the 'anchorOrder' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isAnchorOrderDirty() {
    return isDirty(31);
  }

  /**
   * Gets the value of the 'parseStatus' field.
   */
  public ai.platon.pulsar.persist.gora.generated.GParseStatus getParseStatus() {
    return parseStatus;
  }

  /**
   * Sets the value of the 'parseStatus' field.
   * @param value the value to set.
   */
  public void setParseStatus(ai.platon.pulsar.persist.gora.generated.GParseStatus value) {
    this.parseStatus = value;
    setDirty(32);
  }
  
  /**
   * Checks the dirty status of the 'parseStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isParseStatusDirty() {
    return isDirty(32);
  }

  /**
   * Gets the value of the 'pageTitle' field.
   */
  public java.lang.CharSequence getPageTitle() {
    return pageTitle;
  }

  /**
   * Sets the value of the 'pageTitle' field.
   * @param value the value to set.
   */
  public void setPageTitle(java.lang.CharSequence value) {
    this.pageTitle = value;
    setDirty(33);
  }
  
  /**
   * Checks the dirty status of the 'pageTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPageTitleDirty() {
    return isDirty(33);
  }

  /**
   * Gets the value of the 'pageText' field.
   */
  public java.lang.CharSequence getPageText() {
    return pageText;
  }

  /**
   * Sets the value of the 'pageText' field.
   * @param value the value to set.
   */
  public void setPageText(java.lang.CharSequence value) {
    this.pageText = value;
    setDirty(34);
  }
  
  /**
   * Checks the dirty status of the 'pageText' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPageTextDirty() {
    return isDirty(34);
  }

  /**
   * Gets the value of the 'contentTitle' field.
   */
  public java.lang.CharSequence getContentTitle() {
    return contentTitle;
  }

  /**
   * Sets the value of the 'contentTitle' field.
   * @param value the value to set.
   */
  public void setContentTitle(java.lang.CharSequence value) {
    this.contentTitle = value;
    setDirty(35);
  }
  
  /**
   * Checks the dirty status of the 'contentTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentTitleDirty() {
    return isDirty(35);
  }

  /**
   * Gets the value of the 'contentText' field.
   */
  public java.lang.CharSequence getContentText() {
    return contentText;
  }

  /**
   * Sets the value of the 'contentText' field.
   * @param value the value to set.
   */
  public void setContentText(java.lang.CharSequence value) {
    this.contentText = value;
    setDirty(36);
  }
  
  /**
   * Checks the dirty status of the 'contentText' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentTextDirty() {
    return isDirty(36);
  }

  /**
   * Gets the value of the 'contentTextLen' field.
   */
  public java.lang.Integer getContentTextLen() {
    return contentTextLen;
  }

  /**
   * Sets the value of the 'contentTextLen' field.
   * @param value the value to set.
   */
  public void setContentTextLen(java.lang.Integer value) {
    this.contentTextLen = value;
    setDirty(37);
  }
  
  /**
   * Checks the dirty status of the 'contentTextLen' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentTextLenDirty() {
    return isDirty(37);
  }

  /**
   * Gets the value of the 'pageCategory' field.
   */
  public java.lang.CharSequence getPageCategory() {
    return pageCategory;
  }

  /**
   * Sets the value of the 'pageCategory' field.
   * @param value the value to set.
   */
  public void setPageCategory(java.lang.CharSequence value) {
    this.pageCategory = value;
    setDirty(38);
  }
  
  /**
   * Checks the dirty status of the 'pageCategory' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPageCategoryDirty() {
    return isDirty(38);
  }

  /**
   * Gets the value of the 'contentModifiedTime' field.
   */
  public java.lang.Long getContentModifiedTime() {
    return contentModifiedTime;
  }

  /**
   * Sets the value of the 'contentModifiedTime' field.
   * @param value the value to set.
   */
  public void setContentModifiedTime(java.lang.Long value) {
    this.contentModifiedTime = value;
    setDirty(39);
  }
  
  /**
   * Checks the dirty status of the 'contentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentModifiedTimeDirty() {
    return isDirty(39);
  }

  /**
   * Gets the value of the 'prevContentModifiedTime' field.
   */
  public java.lang.Long getPrevContentModifiedTime() {
    return prevContentModifiedTime;
  }

  /**
   * Sets the value of the 'prevContentModifiedTime' field.
   * @param value the value to set.
   */
  public void setPrevContentModifiedTime(java.lang.Long value) {
    this.prevContentModifiedTime = value;
    setDirty(40);
  }
  
  /**
   * Checks the dirty status of the 'prevContentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevContentModifiedTimeDirty() {
    return isDirty(40);
  }

  /**
   * Gets the value of the 'contentPublishTime' field.
   */
  public java.lang.Long getContentPublishTime() {
    return contentPublishTime;
  }

  /**
   * Sets the value of the 'contentPublishTime' field.
   * @param value the value to set.
   */
  public void setContentPublishTime(java.lang.Long value) {
    this.contentPublishTime = value;
    setDirty(41);
  }
  
  /**
   * Checks the dirty status of the 'contentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentPublishTimeDirty() {
    return isDirty(41);
  }

  /**
   * Gets the value of the 'prevContentPublishTime' field.
   */
  public java.lang.Long getPrevContentPublishTime() {
    return prevContentPublishTime;
  }

  /**
   * Sets the value of the 'prevContentPublishTime' field.
   * @param value the value to set.
   */
  public void setPrevContentPublishTime(java.lang.Long value) {
    this.prevContentPublishTime = value;
    setDirty(42);
  }
  
  /**
   * Checks the dirty status of the 'prevContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevContentPublishTimeDirty() {
    return isDirty(42);
  }

  /**
   * Gets the value of the 'refContentPublishTime' field.
   */
  public java.lang.Long getRefContentPublishTime() {
    return refContentPublishTime;
  }

  /**
   * Sets the value of the 'refContentPublishTime' field.
   * @param value the value to set.
   */
  public void setRefContentPublishTime(java.lang.Long value) {
    this.refContentPublishTime = value;
    setDirty(43);
  }
  
  /**
   * Checks the dirty status of the 'refContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isRefContentPublishTimeDirty() {
    return isDirty(43);
  }

  /**
   * Gets the value of the 'prevRefContentPublishTime' field.
   */
  public java.lang.Long getPrevRefContentPublishTime() {
    return prevRefContentPublishTime;
  }

  /**
   * Sets the value of the 'prevRefContentPublishTime' field.
   * @param value the value to set.
   */
  public void setPrevRefContentPublishTime(java.lang.Long value) {
    this.prevRefContentPublishTime = value;
    setDirty(44);
  }
  
  /**
   * Checks the dirty status of the 'prevRefContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevRefContentPublishTimeDirty() {
    return isDirty(44);
  }

  /**
   * Gets the value of the 'pageModelUpdateTime' field.
   */
  public java.lang.Long getPageModelUpdateTime() {
    return pageModelUpdateTime;
  }

  /**
   * Sets the value of the 'pageModelUpdateTime' field.
   * @param value the value to set.
   */
  public void setPageModelUpdateTime(java.lang.Long value) {
    this.pageModelUpdateTime = value;
    setDirty(45);
  }
  
  /**
   * Checks the dirty status of the 'pageModelUpdateTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPageModelUpdateTimeDirty() {
    return isDirty(45);
  }

  /**
   * Gets the value of the 'prevSignature' field.
   */
  public java.nio.ByteBuffer getPrevSignature() {
    return prevSignature;
  }

  /**
   * Sets the value of the 'prevSignature' field.
   * @param value the value to set.
   */
  public void setPrevSignature(java.nio.ByteBuffer value) {
    this.prevSignature = value;
    setDirty(46);
  }
  
  /**
   * Checks the dirty status of the 'prevSignature' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevSignatureDirty() {
    return isDirty(46);
  }

  /**
   * Gets the value of the 'signature' field.
   */
  public java.nio.ByteBuffer getSignature() {
    return signature;
  }

  /**
   * Sets the value of the 'signature' field.
   * @param value the value to set.
   */
  public void setSignature(java.nio.ByteBuffer value) {
    this.signature = value;
    setDirty(47);
  }
  
  /**
   * Checks the dirty status of the 'signature' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isSignatureDirty() {
    return isDirty(47);
  }

  /**
   * Gets the value of the 'contentScore' field.
   */
  public java.lang.Float getContentScore() {
    return contentScore;
  }

  /**
   * Sets the value of the 'contentScore' field.
   * @param value the value to set.
   */
  public void setContentScore(java.lang.Float value) {
    this.contentScore = value;
    setDirty(48);
  }
  
  /**
   * Checks the dirty status of the 'contentScore' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isContentScoreDirty() {
    return isDirty(48);
  }

  /**
   * Gets the value of the 'score' field.
   */
  public java.lang.Float getScore() {
    return score;
  }

  /**
   * Sets the value of the 'score' field.
   * @param value the value to set.
   */
  public void setScore(java.lang.Float value) {
    this.score = value;
    setDirty(49);
  }
  
  /**
   * Checks the dirty status of the 'score' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isScoreDirty() {
    return isDirty(49);
  }

  /**
   * Gets the value of the 'sortScore' field.
   */
  public java.lang.CharSequence getSortScore() {
    return sortScore;
  }

  /**
   * Sets the value of the 'sortScore' field.
   * @param value the value to set.
   */
  public void setSortScore(java.lang.CharSequence value) {
    this.sortScore = value;
    setDirty(50);
  }
  
  /**
   * Checks the dirty status of the 'sortScore' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isSortScoreDirty() {
    return isDirty(50);
  }

  /**
   * Gets the value of the 'pageCounters' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.Integer> getPageCounters() {
    return pageCounters;
  }

  /**
   * Sets the value of the 'pageCounters' field.
   * @param value the value to set.
   */
  public void setPageCounters(java.util.Map<java.lang.CharSequence,java.lang.Integer> value) {
    this.pageCounters = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(51);
  }
  
  /**
   * Checks the dirty status of the 'pageCounters' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPageCountersDirty() {
    return isDirty(51);
  }

  /**
   * Gets the value of the 'headers' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getHeaders() {
    return headers;
  }

  /**
   * Sets the value of the 'headers' field.
   * @param value the value to set.
   */
  public void setHeaders(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.headers = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(52);
  }
  
  /**
   * Checks the dirty status of the 'headers' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isHeadersDirty() {
    return isDirty(52);
  }

  /**
   * Gets the value of the 'links' field.
   */
  public java.util.List<java.lang.CharSequence> getLinks() {
    return links;
  }

  /**
   * Sets the value of the 'links' field.
   * @param value the value to set.
   */
  public void setLinks(java.util.List<java.lang.CharSequence> value) {
    this.links = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(53);
  }
  
  /**
   * Checks the dirty status of the 'links' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isLinksDirty() {
    return isDirty(53);
  }

  /**
   * Gets the value of the 'deadLinks' field.
   */
  public java.util.List<java.lang.CharSequence> getDeadLinks() {
    return deadLinks;
  }

  /**
   * Sets the value of the 'deadLinks' field.
   * @param value the value to set.
   */
  public void setDeadLinks(java.util.List<java.lang.CharSequence> value) {
    this.deadLinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(54);
  }
  
  /**
   * Checks the dirty status of the 'deadLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDeadLinksDirty() {
    return isDirty(54);
  }

  /**
   * Gets the value of the 'liveLinks' field.
   */
  public java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> getLiveLinks() {
    return liveLinks;
  }

  /**
   * Sets the value of the 'liveLinks' field.
   * @param value the value to set.
   */
  public void setLiveLinks(java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> value) {
    this.liveLinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(55);
  }
  
  /**
   * Checks the dirty status of the 'liveLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isLiveLinksDirty() {
    return isDirty(55);
  }

  /**
   * Gets the value of the 'vividLinks' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getVividLinks() {
    return vividLinks;
  }

  /**
   * Sets the value of the 'vividLinks' field.
   * @param value the value to set.
   */
  public void setVividLinks(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.vividLinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(56);
  }
  
  /**
   * Checks the dirty status of the 'vividLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isVividLinksDirty() {
    return isDirty(56);
  }

  /**
   * Gets the value of the 'inlinks' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getInlinks() {
    return inlinks;
  }

  /**
   * Sets the value of the 'inlinks' field.
   * @param value the value to set.
   */
  public void setInlinks(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.inlinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(57);
  }
  
  /**
   * Checks the dirty status of the 'inlinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isInlinksDirty() {
    return isDirty(57);
  }

  /**
   * Gets the value of the 'markers' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMarkers() {
    return markers;
  }

  /**
   * Sets the value of the 'markers' field.
   * @param value the value to set.
   */
  public void setMarkers(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.markers = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(58);
  }
  
  /**
   * Checks the dirty status of the 'markers' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMarkersDirty() {
    return isDirty(58);
  }

  /**
   * Gets the value of the 'metadata' field.
   */
  public java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> getMetadata() {
    return metadata;
  }

  /**
   * Sets the value of the 'metadata' field.
   * @param value the value to set.
   */
  public void setMetadata(java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> value) {
    this.metadata = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(59);
  }
  
  /**
   * Checks the dirty status of the 'metadata' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMetadataDirty() {
    return isDirty(59);
  }

  /**
   * Gets the value of the 'activeDOMStatus' field.
   */
  public ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus getActiveDOMStatus() {
    return activeDOMStatus;
  }

  /**
   * Sets the value of the 'activeDOMStatus' field.
   * @param value the value to set.
   */
  public void setActiveDOMStatus(ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus value) {
    this.activeDOMStatus = value;
    setDirty(60);
  }
  
  /**
   * Checks the dirty status of the 'activeDOMStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isActiveDOMStatusDirty() {
    return isDirty(60);
  }

  /**
   * Gets the value of the 'activeDOMStatTrace' field.
   */
  public java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> getActiveDOMStatTrace() {
    return activeDOMStatTrace;
  }

  /**
   * Sets the value of the 'activeDOMStatTrace' field.
   * @param value the value to set.
   */
  public void setActiveDOMStatTrace(java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> value) {
    this.activeDOMStatTrace = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(61);
  }
  
  /**
   * Checks the dirty status of the 'activeDOMStatTrace' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isActiveDOMStatTraceDirty() {
    return isDirty(61);
  }

  /**
   * Gets the value of the 'pageModel' field.
   */
  public java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> getPageModel() {
    return pageModel;
  }

  /**
   * Sets the value of the 'pageModel' field.
   * @param value the value to set.
   */
  public void setPageModel(java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> value) {
    this.pageModel = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(62);
  }
  
  /**
   * Checks the dirty status of the 'pageModel' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPageModelDirty() {
    return isDirty(62);
  }

  /** Creates a new GWebPage RecordBuilder */
  public static ai.platon.pulsar.persist.gora.generated.GWebPage.Builder newBuilder() {
    return new ai.platon.pulsar.persist.gora.generated.GWebPage.Builder();
  }
  
  /** Creates a new GWebPage RecordBuilder by copying an existing Builder */
  public static ai.platon.pulsar.persist.gora.generated.GWebPage.Builder newBuilder(ai.platon.pulsar.persist.gora.generated.GWebPage.Builder other) {
    return new ai.platon.pulsar.persist.gora.generated.GWebPage.Builder(other);
  }
  
  /** Creates a new GWebPage RecordBuilder by copying an existing GWebPage instance */
  public static ai.platon.pulsar.persist.gora.generated.GWebPage.Builder newBuilder(ai.platon.pulsar.persist.gora.generated.GWebPage other) {
    return new ai.platon.pulsar.persist.gora.generated.GWebPage.Builder(other);
  }
  
  private static java.nio.ByteBuffer deepCopyToReadOnlyBuffer(
      java.nio.ByteBuffer input) {
    java.nio.ByteBuffer copy = java.nio.ByteBuffer.allocate(input.capacity());
    int position = input.position();
    input.reset();
    int mark = input.position();
    int limit = input.limit();
    input.rewind();
    input.limit(input.capacity());
    copy.put(input);
    input.rewind();
    copy.rewind();
    input.position(mark);
    input.mark();
    copy.position(mark);
    copy.mark();
    input.position(position);
    copy.position(position);
    input.limit(limit);
    copy.limit(limit);
    return copy.asReadOnlyBuffer();
  }
  
  /**
   * RecordBuilder for GWebPage instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<GWebPage>
    implements org.apache.avro.data.RecordBuilder<GWebPage> {

    private java.lang.CharSequence baseUrl;
    private long createTime;
    private int distance;
    private int fetchCount;
    private int fetchPriority;
    private int fetchInterval;
    private java.lang.CharSequence zoneId;
    private java.lang.CharSequence params;
    private java.lang.CharSequence batchId;
    private java.lang.Integer resource;
    private int crawlStatus;
    private java.lang.CharSequence browser;
    private java.lang.CharSequence proxy;
    private long prevFetchTime;
    private long prevCrawlTime1;
    private long fetchTime;
    private int fetchRetries;
    private java.lang.CharSequence reprUrl;
    private long prevModifiedTime;
    private long modifiedTime;
    private ai.platon.pulsar.persist.gora.generated.GProtocolStatus protocolStatus;
    private java.lang.CharSequence encoding;
    private java.lang.CharSequence contentType;
    private java.nio.ByteBuffer content;
    private long contentLength;
    private long lastContentLength;
    private long aveContentLength;
    private long persistedContentLength;
    private java.lang.CharSequence referrer;
    private java.lang.CharSequence htmlIntegrity;
    private java.lang.CharSequence anchor;
    private int anchorOrder;
    private ai.platon.pulsar.persist.gora.generated.GParseStatus parseStatus;
    private java.lang.CharSequence pageTitle;
    private java.lang.CharSequence pageText;
    private java.lang.CharSequence contentTitle;
    private java.lang.CharSequence contentText;
    private int contentTextLen;
    private java.lang.CharSequence pageCategory;
    private long contentModifiedTime;
    private long prevContentModifiedTime;
    private long contentPublishTime;
    private long prevContentPublishTime;
    private long refContentPublishTime;
    private long prevRefContentPublishTime;
    private long pageModelUpdateTime;
    private java.nio.ByteBuffer prevSignature;
    private java.nio.ByteBuffer signature;
    private float contentScore;
    private float score;
    private java.lang.CharSequence sortScore;
    private java.util.Map<java.lang.CharSequence,java.lang.Integer> pageCounters;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> headers;
    private java.util.List<java.lang.CharSequence> links;
    private java.util.List<java.lang.CharSequence> deadLinks;
    private java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> liveLinks;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> vividLinks;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> inlinks;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> markers;
    private java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> metadata;
    private ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus activeDOMStatus;
    private java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> activeDOMStatTrace;
    private java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> pageModel;

    /** Creates a new Builder */
    private Builder() {
      super(ai.platon.pulsar.persist.gora.generated.GWebPage.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(ai.platon.pulsar.persist.gora.generated.GWebPage.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing GWebPage instance */
    private Builder(ai.platon.pulsar.persist.gora.generated.GWebPage other) {
            super(ai.platon.pulsar.persist.gora.generated.GWebPage.SCHEMA$);
      if (isValidValue(fields()[0], other.baseUrl)) {
        this.baseUrl = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.baseUrl);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.createTime)) {
        this.createTime = (java.lang.Long) data().deepCopy(fields()[1].schema(), other.createTime);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.distance)) {
        this.distance = (java.lang.Integer) data().deepCopy(fields()[2].schema(), other.distance);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.fetchCount)) {
        this.fetchCount = (java.lang.Integer) data().deepCopy(fields()[3].schema(), other.fetchCount);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.fetchPriority)) {
        this.fetchPriority = (java.lang.Integer) data().deepCopy(fields()[4].schema(), other.fetchPriority);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.fetchInterval)) {
        this.fetchInterval = (java.lang.Integer) data().deepCopy(fields()[5].schema(), other.fetchInterval);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.zoneId)) {
        this.zoneId = (java.lang.CharSequence) data().deepCopy(fields()[6].schema(), other.zoneId);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.params)) {
        this.params = (java.lang.CharSequence) data().deepCopy(fields()[7].schema(), other.params);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.batchId)) {
        this.batchId = (java.lang.CharSequence) data().deepCopy(fields()[8].schema(), other.batchId);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.resource)) {
        this.resource = (java.lang.Integer) data().deepCopy(fields()[9].schema(), other.resource);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.crawlStatus)) {
        this.crawlStatus = (java.lang.Integer) data().deepCopy(fields()[10].schema(), other.crawlStatus);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.browser)) {
        this.browser = (java.lang.CharSequence) data().deepCopy(fields()[11].schema(), other.browser);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.proxy)) {
        this.proxy = (java.lang.CharSequence) data().deepCopy(fields()[12].schema(), other.proxy);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.prevFetchTime)) {
        this.prevFetchTime = (java.lang.Long) data().deepCopy(fields()[13].schema(), other.prevFetchTime);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.prevCrawlTime1)) {
        this.prevCrawlTime1 = (java.lang.Long) data().deepCopy(fields()[14].schema(), other.prevCrawlTime1);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.fetchTime)) {
        this.fetchTime = (java.lang.Long) data().deepCopy(fields()[15].schema(), other.fetchTime);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.fetchRetries)) {
        this.fetchRetries = (java.lang.Integer) data().deepCopy(fields()[16].schema(), other.fetchRetries);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.reprUrl)) {
        this.reprUrl = (java.lang.CharSequence) data().deepCopy(fields()[17].schema(), other.reprUrl);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.prevModifiedTime)) {
        this.prevModifiedTime = (java.lang.Long) data().deepCopy(fields()[18].schema(), other.prevModifiedTime);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.modifiedTime)) {
        this.modifiedTime = (java.lang.Long) data().deepCopy(fields()[19].schema(), other.modifiedTime);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.protocolStatus)) {
        this.protocolStatus = (ai.platon.pulsar.persist.gora.generated.GProtocolStatus) data().deepCopy(fields()[20].schema(), other.protocolStatus);
        fieldSetFlags()[20] = true;
      }
      if (isValidValue(fields()[21], other.encoding)) {
        this.encoding = (java.lang.CharSequence) data().deepCopy(fields()[21].schema(), other.encoding);
        fieldSetFlags()[21] = true;
      }
      if (isValidValue(fields()[22], other.contentType)) {
        this.contentType = (java.lang.CharSequence) data().deepCopy(fields()[22].schema(), other.contentType);
        fieldSetFlags()[22] = true;
      }
      if (isValidValue(fields()[23], other.content)) {
        this.content = (java.nio.ByteBuffer) data().deepCopy(fields()[23].schema(), other.content);
        fieldSetFlags()[23] = true;
      }
      if (isValidValue(fields()[24], other.contentLength)) {
        this.contentLength = (java.lang.Long) data().deepCopy(fields()[24].schema(), other.contentLength);
        fieldSetFlags()[24] = true;
      }
      if (isValidValue(fields()[25], other.lastContentLength)) {
        this.lastContentLength = (java.lang.Long) data().deepCopy(fields()[25].schema(), other.lastContentLength);
        fieldSetFlags()[25] = true;
      }
      if (isValidValue(fields()[26], other.aveContentLength)) {
        this.aveContentLength = (java.lang.Long) data().deepCopy(fields()[26].schema(), other.aveContentLength);
        fieldSetFlags()[26] = true;
      }
      if (isValidValue(fields()[27], other.persistedContentLength)) {
        this.persistedContentLength = (java.lang.Long) data().deepCopy(fields()[27].schema(), other.persistedContentLength);
        fieldSetFlags()[27] = true;
      }
      if (isValidValue(fields()[28], other.referrer)) {
        this.referrer = (java.lang.CharSequence) data().deepCopy(fields()[28].schema(), other.referrer);
        fieldSetFlags()[28] = true;
      }
      if (isValidValue(fields()[29], other.htmlIntegrity)) {
        this.htmlIntegrity = (java.lang.CharSequence) data().deepCopy(fields()[29].schema(), other.htmlIntegrity);
        fieldSetFlags()[29] = true;
      }
      if (isValidValue(fields()[30], other.anchor)) {
        this.anchor = (java.lang.CharSequence) data().deepCopy(fields()[30].schema(), other.anchor);
        fieldSetFlags()[30] = true;
      }
      if (isValidValue(fields()[31], other.anchorOrder)) {
        this.anchorOrder = (java.lang.Integer) data().deepCopy(fields()[31].schema(), other.anchorOrder);
        fieldSetFlags()[31] = true;
      }
      if (isValidValue(fields()[32], other.parseStatus)) {
        this.parseStatus = (ai.platon.pulsar.persist.gora.generated.GParseStatus) data().deepCopy(fields()[32].schema(), other.parseStatus);
        fieldSetFlags()[32] = true;
      }
      if (isValidValue(fields()[33], other.pageTitle)) {
        this.pageTitle = (java.lang.CharSequence) data().deepCopy(fields()[33].schema(), other.pageTitle);
        fieldSetFlags()[33] = true;
      }
      if (isValidValue(fields()[34], other.pageText)) {
        this.pageText = (java.lang.CharSequence) data().deepCopy(fields()[34].schema(), other.pageText);
        fieldSetFlags()[34] = true;
      }
      if (isValidValue(fields()[35], other.contentTitle)) {
        this.contentTitle = (java.lang.CharSequence) data().deepCopy(fields()[35].schema(), other.contentTitle);
        fieldSetFlags()[35] = true;
      }
      if (isValidValue(fields()[36], other.contentText)) {
        this.contentText = (java.lang.CharSequence) data().deepCopy(fields()[36].schema(), other.contentText);
        fieldSetFlags()[36] = true;
      }
      if (isValidValue(fields()[37], other.contentTextLen)) {
        this.contentTextLen = (java.lang.Integer) data().deepCopy(fields()[37].schema(), other.contentTextLen);
        fieldSetFlags()[37] = true;
      }
      if (isValidValue(fields()[38], other.pageCategory)) {
        this.pageCategory = (java.lang.CharSequence) data().deepCopy(fields()[38].schema(), other.pageCategory);
        fieldSetFlags()[38] = true;
      }
      if (isValidValue(fields()[39], other.contentModifiedTime)) {
        this.contentModifiedTime = (java.lang.Long) data().deepCopy(fields()[39].schema(), other.contentModifiedTime);
        fieldSetFlags()[39] = true;
      }
      if (isValidValue(fields()[40], other.prevContentModifiedTime)) {
        this.prevContentModifiedTime = (java.lang.Long) data().deepCopy(fields()[40].schema(), other.prevContentModifiedTime);
        fieldSetFlags()[40] = true;
      }
      if (isValidValue(fields()[41], other.contentPublishTime)) {
        this.contentPublishTime = (java.lang.Long) data().deepCopy(fields()[41].schema(), other.contentPublishTime);
        fieldSetFlags()[41] = true;
      }
      if (isValidValue(fields()[42], other.prevContentPublishTime)) {
        this.prevContentPublishTime = (java.lang.Long) data().deepCopy(fields()[42].schema(), other.prevContentPublishTime);
        fieldSetFlags()[42] = true;
      }
      if (isValidValue(fields()[43], other.refContentPublishTime)) {
        this.refContentPublishTime = (java.lang.Long) data().deepCopy(fields()[43].schema(), other.refContentPublishTime);
        fieldSetFlags()[43] = true;
      }
      if (isValidValue(fields()[44], other.prevRefContentPublishTime)) {
        this.prevRefContentPublishTime = (java.lang.Long) data().deepCopy(fields()[44].schema(), other.prevRefContentPublishTime);
        fieldSetFlags()[44] = true;
      }
      if (isValidValue(fields()[45], other.pageModelUpdateTime)) {
        this.pageModelUpdateTime = (java.lang.Long) data().deepCopy(fields()[45].schema(), other.pageModelUpdateTime);
        fieldSetFlags()[45] = true;
      }
      if (isValidValue(fields()[46], other.prevSignature)) {
        this.prevSignature = (java.nio.ByteBuffer) data().deepCopy(fields()[46].schema(), other.prevSignature);
        fieldSetFlags()[46] = true;
      }
      if (isValidValue(fields()[47], other.signature)) {
        this.signature = (java.nio.ByteBuffer) data().deepCopy(fields()[47].schema(), other.signature);
        fieldSetFlags()[47] = true;
      }
      if (isValidValue(fields()[48], other.contentScore)) {
        this.contentScore = (java.lang.Float) data().deepCopy(fields()[48].schema(), other.contentScore);
        fieldSetFlags()[48] = true;
      }
      if (isValidValue(fields()[49], other.score)) {
        this.score = (java.lang.Float) data().deepCopy(fields()[49].schema(), other.score);
        fieldSetFlags()[49] = true;
      }
      if (isValidValue(fields()[50], other.sortScore)) {
        this.sortScore = (java.lang.CharSequence) data().deepCopy(fields()[50].schema(), other.sortScore);
        fieldSetFlags()[50] = true;
      }
      if (isValidValue(fields()[51], other.pageCounters)) {
        this.pageCounters = (java.util.Map<java.lang.CharSequence,java.lang.Integer>) data().deepCopy(fields()[51].schema(), other.pageCounters);
        fieldSetFlags()[51] = true;
      }
      if (isValidValue(fields()[52], other.headers)) {
        this.headers = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) data().deepCopy(fields()[52].schema(), other.headers);
        fieldSetFlags()[52] = true;
      }
      if (isValidValue(fields()[53], other.links)) {
        this.links = (java.util.List<java.lang.CharSequence>) data().deepCopy(fields()[53].schema(), other.links);
        fieldSetFlags()[53] = true;
      }
      if (isValidValue(fields()[54], other.deadLinks)) {
        this.deadLinks = (java.util.List<java.lang.CharSequence>) data().deepCopy(fields()[54].schema(), other.deadLinks);
        fieldSetFlags()[54] = true;
      }
      if (isValidValue(fields()[55], other.liveLinks)) {
        this.liveLinks = (java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink>) data().deepCopy(fields()[55].schema(), other.liveLinks);
        fieldSetFlags()[55] = true;
      }
      if (isValidValue(fields()[56], other.vividLinks)) {
        this.vividLinks = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) data().deepCopy(fields()[56].schema(), other.vividLinks);
        fieldSetFlags()[56] = true;
      }
      if (isValidValue(fields()[57], other.inlinks)) {
        this.inlinks = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) data().deepCopy(fields()[57].schema(), other.inlinks);
        fieldSetFlags()[57] = true;
      }
      if (isValidValue(fields()[58], other.markers)) {
        this.markers = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) data().deepCopy(fields()[58].schema(), other.markers);
        fieldSetFlags()[58] = true;
      }
      if (isValidValue(fields()[59], other.metadata)) {
        this.metadata = (java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer>) data().deepCopy(fields()[59].schema(), other.metadata);
        fieldSetFlags()[59] = true;
      }
      if (isValidValue(fields()[60], other.activeDOMStatus)) {
        this.activeDOMStatus = (ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus) data().deepCopy(fields()[60].schema(), other.activeDOMStatus);
        fieldSetFlags()[60] = true;
      }
      if (isValidValue(fields()[61], other.activeDOMStatTrace)) {
        this.activeDOMStatTrace = (java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat>) data().deepCopy(fields()[61].schema(), other.activeDOMStatTrace);
        fieldSetFlags()[61] = true;
      }
      if (isValidValue(fields()[62], other.pageModel)) {
        this.pageModel = (java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup>) data().deepCopy(fields()[62].schema(), other.pageModel);
        fieldSetFlags()[62] = true;
      }
    }

    /** Gets the value of the 'baseUrl' field */
    public java.lang.CharSequence getBaseUrl() {
      return baseUrl;
    }
    
    /** Sets the value of the 'baseUrl' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setBaseUrl(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.baseUrl = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'baseUrl' field has been set */
    public boolean hasBaseUrl() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'baseUrl' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearBaseUrl() {
      baseUrl = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'createTime' field */
    public java.lang.Long getCreateTime() {
      return createTime;
    }
    
    /** Sets the value of the 'createTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setCreateTime(long value) {
      validate(fields()[1], value);
      this.createTime = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'createTime' field has been set */
    public boolean hasCreateTime() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'createTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearCreateTime() {
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'distance' field */
    public java.lang.Integer getDistance() {
      return distance;
    }
    
    /** Sets the value of the 'distance' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setDistance(int value) {
      validate(fields()[2], value);
      this.distance = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'distance' field has been set */
    public boolean hasDistance() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'distance' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearDistance() {
      fieldSetFlags()[2] = false;
      return this;
    }
    
    /** Gets the value of the 'fetchCount' field */
    public java.lang.Integer getFetchCount() {
      return fetchCount;
    }
    
    /** Sets the value of the 'fetchCount' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setFetchCount(int value) {
      validate(fields()[3], value);
      this.fetchCount = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'fetchCount' field has been set */
    public boolean hasFetchCount() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'fetchCount' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearFetchCount() {
      fieldSetFlags()[3] = false;
      return this;
    }
    
    /** Gets the value of the 'fetchPriority' field */
    public java.lang.Integer getFetchPriority() {
      return fetchPriority;
    }
    
    /** Sets the value of the 'fetchPriority' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setFetchPriority(int value) {
      validate(fields()[4], value);
      this.fetchPriority = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'fetchPriority' field has been set */
    public boolean hasFetchPriority() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'fetchPriority' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearFetchPriority() {
      fieldSetFlags()[4] = false;
      return this;
    }
    
    /** Gets the value of the 'fetchInterval' field */
    public java.lang.Integer getFetchInterval() {
      return fetchInterval;
    }
    
    /** Sets the value of the 'fetchInterval' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setFetchInterval(int value) {
      validate(fields()[5], value);
      this.fetchInterval = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'fetchInterval' field has been set */
    public boolean hasFetchInterval() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'fetchInterval' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearFetchInterval() {
      fieldSetFlags()[5] = false;
      return this;
    }
    
    /** Gets the value of the 'zoneId' field */
    public java.lang.CharSequence getZoneId() {
      return zoneId;
    }
    
    /** Sets the value of the 'zoneId' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setZoneId(java.lang.CharSequence value) {
      validate(fields()[6], value);
      this.zoneId = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'zoneId' field has been set */
    public boolean hasZoneId() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'zoneId' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearZoneId() {
      zoneId = null;
      fieldSetFlags()[6] = false;
      return this;
    }
    
    /** Gets the value of the 'params' field */
    public java.lang.CharSequence getParams() {
      return params;
    }
    
    /** Sets the value of the 'params' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setParams(java.lang.CharSequence value) {
      validate(fields()[7], value);
      this.params = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'params' field has been set */
    public boolean hasParams() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'params' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearParams() {
      params = null;
      fieldSetFlags()[7] = false;
      return this;
    }
    
    /** Gets the value of the 'batchId' field */
    public java.lang.CharSequence getBatchId() {
      return batchId;
    }
    
    /** Sets the value of the 'batchId' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setBatchId(java.lang.CharSequence value) {
      validate(fields()[8], value);
      this.batchId = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'batchId' field has been set */
    public boolean hasBatchId() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'batchId' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearBatchId() {
      batchId = null;
      fieldSetFlags()[8] = false;
      return this;
    }
    
    /** Gets the value of the 'resource' field */
    public java.lang.Integer getResource() {
      return resource;
    }
    
    /** Sets the value of the 'resource' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setResource(java.lang.Integer value) {
      validate(fields()[9], value);
      this.resource = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'resource' field has been set */
    public boolean hasResource() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'resource' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearResource() {
      resource = null;
      fieldSetFlags()[9] = false;
      return this;
    }
    
    /** Gets the value of the 'crawlStatus' field */
    public java.lang.Integer getCrawlStatus() {
      return crawlStatus;
    }
    
    /** Sets the value of the 'crawlStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setCrawlStatus(int value) {
      validate(fields()[10], value);
      this.crawlStatus = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'crawlStatus' field has been set */
    public boolean hasCrawlStatus() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'crawlStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearCrawlStatus() {
      fieldSetFlags()[10] = false;
      return this;
    }
    
    /** Gets the value of the 'browser' field */
    public java.lang.CharSequence getBrowser() {
      return browser;
    }
    
    /** Sets the value of the 'browser' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setBrowser(java.lang.CharSequence value) {
      validate(fields()[11], value);
      this.browser = value;
      fieldSetFlags()[11] = true;
      return this; 
    }
    
    /** Checks whether the 'browser' field has been set */
    public boolean hasBrowser() {
      return fieldSetFlags()[11];
    }
    
    /** Clears the value of the 'browser' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearBrowser() {
      browser = null;
      fieldSetFlags()[11] = false;
      return this;
    }
    
    /** Gets the value of the 'proxy' field */
    public java.lang.CharSequence getProxy() {
      return proxy;
    }
    
    /** Sets the value of the 'proxy' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setProxy(java.lang.CharSequence value) {
      validate(fields()[12], value);
      this.proxy = value;
      fieldSetFlags()[12] = true;
      return this; 
    }
    
    /** Checks whether the 'proxy' field has been set */
    public boolean hasProxy() {
      return fieldSetFlags()[12];
    }
    
    /** Clears the value of the 'proxy' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearProxy() {
      proxy = null;
      fieldSetFlags()[12] = false;
      return this;
    }
    
    /** Gets the value of the 'prevFetchTime' field */
    public java.lang.Long getPrevFetchTime() {
      return prevFetchTime;
    }
    
    /** Sets the value of the 'prevFetchTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevFetchTime(long value) {
      validate(fields()[13], value);
      this.prevFetchTime = value;
      fieldSetFlags()[13] = true;
      return this; 
    }
    
    /** Checks whether the 'prevFetchTime' field has been set */
    public boolean hasPrevFetchTime() {
      return fieldSetFlags()[13];
    }
    
    /** Clears the value of the 'prevFetchTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevFetchTime() {
      fieldSetFlags()[13] = false;
      return this;
    }
    
    /** Gets the value of the 'prevCrawlTime1' field */
    public java.lang.Long getPrevCrawlTime1() {
      return prevCrawlTime1;
    }
    
    /** Sets the value of the 'prevCrawlTime1' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevCrawlTime1(long value) {
      validate(fields()[14], value);
      this.prevCrawlTime1 = value;
      fieldSetFlags()[14] = true;
      return this; 
    }
    
    /** Checks whether the 'prevCrawlTime1' field has been set */
    public boolean hasPrevCrawlTime1() {
      return fieldSetFlags()[14];
    }
    
    /** Clears the value of the 'prevCrawlTime1' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevCrawlTime1() {
      fieldSetFlags()[14] = false;
      return this;
    }
    
    /** Gets the value of the 'fetchTime' field */
    public java.lang.Long getFetchTime() {
      return fetchTime;
    }
    
    /** Sets the value of the 'fetchTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setFetchTime(long value) {
      validate(fields()[15], value);
      this.fetchTime = value;
      fieldSetFlags()[15] = true;
      return this; 
    }
    
    /** Checks whether the 'fetchTime' field has been set */
    public boolean hasFetchTime() {
      return fieldSetFlags()[15];
    }
    
    /** Clears the value of the 'fetchTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearFetchTime() {
      fieldSetFlags()[15] = false;
      return this;
    }
    
    /** Gets the value of the 'fetchRetries' field */
    public java.lang.Integer getFetchRetries() {
      return fetchRetries;
    }
    
    /** Sets the value of the 'fetchRetries' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setFetchRetries(int value) {
      validate(fields()[16], value);
      this.fetchRetries = value;
      fieldSetFlags()[16] = true;
      return this; 
    }
    
    /** Checks whether the 'fetchRetries' field has been set */
    public boolean hasFetchRetries() {
      return fieldSetFlags()[16];
    }
    
    /** Clears the value of the 'fetchRetries' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearFetchRetries() {
      fieldSetFlags()[16] = false;
      return this;
    }
    
    /** Gets the value of the 'reprUrl' field */
    public java.lang.CharSequence getReprUrl() {
      return reprUrl;
    }
    
    /** Sets the value of the 'reprUrl' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setReprUrl(java.lang.CharSequence value) {
      validate(fields()[17], value);
      this.reprUrl = value;
      fieldSetFlags()[17] = true;
      return this; 
    }
    
    /** Checks whether the 'reprUrl' field has been set */
    public boolean hasReprUrl() {
      return fieldSetFlags()[17];
    }
    
    /** Clears the value of the 'reprUrl' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearReprUrl() {
      reprUrl = null;
      fieldSetFlags()[17] = false;
      return this;
    }
    
    /** Gets the value of the 'prevModifiedTime' field */
    public java.lang.Long getPrevModifiedTime() {
      return prevModifiedTime;
    }
    
    /** Sets the value of the 'prevModifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevModifiedTime(long value) {
      validate(fields()[18], value);
      this.prevModifiedTime = value;
      fieldSetFlags()[18] = true;
      return this; 
    }
    
    /** Checks whether the 'prevModifiedTime' field has been set */
    public boolean hasPrevModifiedTime() {
      return fieldSetFlags()[18];
    }
    
    /** Clears the value of the 'prevModifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevModifiedTime() {
      fieldSetFlags()[18] = false;
      return this;
    }
    
    /** Gets the value of the 'modifiedTime' field */
    public java.lang.Long getModifiedTime() {
      return modifiedTime;
    }
    
    /** Sets the value of the 'modifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setModifiedTime(long value) {
      validate(fields()[19], value);
      this.modifiedTime = value;
      fieldSetFlags()[19] = true;
      return this; 
    }
    
    /** Checks whether the 'modifiedTime' field has been set */
    public boolean hasModifiedTime() {
      return fieldSetFlags()[19];
    }
    
    /** Clears the value of the 'modifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearModifiedTime() {
      fieldSetFlags()[19] = false;
      return this;
    }
    
    /** Gets the value of the 'protocolStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GProtocolStatus getProtocolStatus() {
      return protocolStatus;
    }
    
    /** Sets the value of the 'protocolStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setProtocolStatus(ai.platon.pulsar.persist.gora.generated.GProtocolStatus value) {
      validate(fields()[20], value);
      this.protocolStatus = value;
      fieldSetFlags()[20] = true;
      return this; 
    }
    
    /** Checks whether the 'protocolStatus' field has been set */
    public boolean hasProtocolStatus() {
      return fieldSetFlags()[20];
    }
    
    /** Clears the value of the 'protocolStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearProtocolStatus() {
      protocolStatus = null;
      fieldSetFlags()[20] = false;
      return this;
    }
    
    /** Gets the value of the 'encoding' field */
    public java.lang.CharSequence getEncoding() {
      return encoding;
    }
    
    /** Sets the value of the 'encoding' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setEncoding(java.lang.CharSequence value) {
      validate(fields()[21], value);
      this.encoding = value;
      fieldSetFlags()[21] = true;
      return this; 
    }
    
    /** Checks whether the 'encoding' field has been set */
    public boolean hasEncoding() {
      return fieldSetFlags()[21];
    }
    
    /** Clears the value of the 'encoding' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearEncoding() {
      encoding = null;
      fieldSetFlags()[21] = false;
      return this;
    }
    
    /** Gets the value of the 'contentType' field */
    public java.lang.CharSequence getContentType() {
      return contentType;
    }
    
    /** Sets the value of the 'contentType' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentType(java.lang.CharSequence value) {
      validate(fields()[22], value);
      this.contentType = value;
      fieldSetFlags()[22] = true;
      return this; 
    }
    
    /** Checks whether the 'contentType' field has been set */
    public boolean hasContentType() {
      return fieldSetFlags()[22];
    }
    
    /** Clears the value of the 'contentType' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentType() {
      contentType = null;
      fieldSetFlags()[22] = false;
      return this;
    }
    
    /** Gets the value of the 'content' field */
    public java.nio.ByteBuffer getContent() {
      return content;
    }
    
    /** Sets the value of the 'content' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContent(java.nio.ByteBuffer value) {
      validate(fields()[23], value);
      this.content = value;
      fieldSetFlags()[23] = true;
      return this; 
    }
    
    /** Checks whether the 'content' field has been set */
    public boolean hasContent() {
      return fieldSetFlags()[23];
    }
    
    /** Clears the value of the 'content' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContent() {
      content = null;
      fieldSetFlags()[23] = false;
      return this;
    }
    
    /** Gets the value of the 'contentLength' field */
    public java.lang.Long getContentLength() {
      return contentLength;
    }
    
    /** Sets the value of the 'contentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentLength(long value) {
      validate(fields()[24], value);
      this.contentLength = value;
      fieldSetFlags()[24] = true;
      return this; 
    }
    
    /** Checks whether the 'contentLength' field has been set */
    public boolean hasContentLength() {
      return fieldSetFlags()[24];
    }
    
    /** Clears the value of the 'contentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentLength() {
      fieldSetFlags()[24] = false;
      return this;
    }
    
    /** Gets the value of the 'lastContentLength' field */
    public java.lang.Long getLastContentLength() {
      return lastContentLength;
    }
    
    /** Sets the value of the 'lastContentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setLastContentLength(long value) {
      validate(fields()[25], value);
      this.lastContentLength = value;
      fieldSetFlags()[25] = true;
      return this; 
    }
    
    /** Checks whether the 'lastContentLength' field has been set */
    public boolean hasLastContentLength() {
      return fieldSetFlags()[25];
    }
    
    /** Clears the value of the 'lastContentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearLastContentLength() {
      fieldSetFlags()[25] = false;
      return this;
    }
    
    /** Gets the value of the 'aveContentLength' field */
    public java.lang.Long getAveContentLength() {
      return aveContentLength;
    }
    
    /** Sets the value of the 'aveContentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setAveContentLength(long value) {
      validate(fields()[26], value);
      this.aveContentLength = value;
      fieldSetFlags()[26] = true;
      return this; 
    }
    
    /** Checks whether the 'aveContentLength' field has been set */
    public boolean hasAveContentLength() {
      return fieldSetFlags()[26];
    }
    
    /** Clears the value of the 'aveContentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearAveContentLength() {
      fieldSetFlags()[26] = false;
      return this;
    }
    
    /** Gets the value of the 'persistedContentLength' field */
    public java.lang.Long getPersistedContentLength() {
      return persistedContentLength;
    }
    
    /** Sets the value of the 'persistedContentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPersistedContentLength(long value) {
      validate(fields()[27], value);
      this.persistedContentLength = value;
      fieldSetFlags()[27] = true;
      return this; 
    }
    
    /** Checks whether the 'persistedContentLength' field has been set */
    public boolean hasPersistedContentLength() {
      return fieldSetFlags()[27];
    }
    
    /** Clears the value of the 'persistedContentLength' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPersistedContentLength() {
      fieldSetFlags()[27] = false;
      return this;
    }
    
    /** Gets the value of the 'referrer' field */
    public java.lang.CharSequence getReferrer() {
      return referrer;
    }
    
    /** Sets the value of the 'referrer' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setReferrer(java.lang.CharSequence value) {
      validate(fields()[28], value);
      this.referrer = value;
      fieldSetFlags()[28] = true;
      return this; 
    }
    
    /** Checks whether the 'referrer' field has been set */
    public boolean hasReferrer() {
      return fieldSetFlags()[28];
    }
    
    /** Clears the value of the 'referrer' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearReferrer() {
      referrer = null;
      fieldSetFlags()[28] = false;
      return this;
    }
    
    /** Gets the value of the 'htmlIntegrity' field */
    public java.lang.CharSequence getHtmlIntegrity() {
      return htmlIntegrity;
    }
    
    /** Sets the value of the 'htmlIntegrity' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setHtmlIntegrity(java.lang.CharSequence value) {
      validate(fields()[29], value);
      this.htmlIntegrity = value;
      fieldSetFlags()[29] = true;
      return this; 
    }
    
    /** Checks whether the 'htmlIntegrity' field has been set */
    public boolean hasHtmlIntegrity() {
      return fieldSetFlags()[29];
    }
    
    /** Clears the value of the 'htmlIntegrity' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearHtmlIntegrity() {
      htmlIntegrity = null;
      fieldSetFlags()[29] = false;
      return this;
    }
    
    /** Gets the value of the 'anchor' field */
    public java.lang.CharSequence getAnchor() {
      return anchor;
    }
    
    /** Sets the value of the 'anchor' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setAnchor(java.lang.CharSequence value) {
      validate(fields()[30], value);
      this.anchor = value;
      fieldSetFlags()[30] = true;
      return this; 
    }
    
    /** Checks whether the 'anchor' field has been set */
    public boolean hasAnchor() {
      return fieldSetFlags()[30];
    }
    
    /** Clears the value of the 'anchor' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearAnchor() {
      anchor = null;
      fieldSetFlags()[30] = false;
      return this;
    }
    
    /** Gets the value of the 'anchorOrder' field */
    public java.lang.Integer getAnchorOrder() {
      return anchorOrder;
    }
    
    /** Sets the value of the 'anchorOrder' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setAnchorOrder(int value) {
      validate(fields()[31], value);
      this.anchorOrder = value;
      fieldSetFlags()[31] = true;
      return this; 
    }
    
    /** Checks whether the 'anchorOrder' field has been set */
    public boolean hasAnchorOrder() {
      return fieldSetFlags()[31];
    }
    
    /** Clears the value of the 'anchorOrder' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearAnchorOrder() {
      fieldSetFlags()[31] = false;
      return this;
    }
    
    /** Gets the value of the 'parseStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GParseStatus getParseStatus() {
      return parseStatus;
    }
    
    /** Sets the value of the 'parseStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setParseStatus(ai.platon.pulsar.persist.gora.generated.GParseStatus value) {
      validate(fields()[32], value);
      this.parseStatus = value;
      fieldSetFlags()[32] = true;
      return this; 
    }
    
    /** Checks whether the 'parseStatus' field has been set */
    public boolean hasParseStatus() {
      return fieldSetFlags()[32];
    }
    
    /** Clears the value of the 'parseStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearParseStatus() {
      parseStatus = null;
      fieldSetFlags()[32] = false;
      return this;
    }
    
    /** Gets the value of the 'pageTitle' field */
    public java.lang.CharSequence getPageTitle() {
      return pageTitle;
    }
    
    /** Sets the value of the 'pageTitle' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPageTitle(java.lang.CharSequence value) {
      validate(fields()[33], value);
      this.pageTitle = value;
      fieldSetFlags()[33] = true;
      return this; 
    }
    
    /** Checks whether the 'pageTitle' field has been set */
    public boolean hasPageTitle() {
      return fieldSetFlags()[33];
    }
    
    /** Clears the value of the 'pageTitle' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPageTitle() {
      pageTitle = null;
      fieldSetFlags()[33] = false;
      return this;
    }
    
    /** Gets the value of the 'pageText' field */
    public java.lang.CharSequence getPageText() {
      return pageText;
    }
    
    /** Sets the value of the 'pageText' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPageText(java.lang.CharSequence value) {
      validate(fields()[34], value);
      this.pageText = value;
      fieldSetFlags()[34] = true;
      return this; 
    }
    
    /** Checks whether the 'pageText' field has been set */
    public boolean hasPageText() {
      return fieldSetFlags()[34];
    }
    
    /** Clears the value of the 'pageText' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPageText() {
      pageText = null;
      fieldSetFlags()[34] = false;
      return this;
    }
    
    /** Gets the value of the 'contentTitle' field */
    public java.lang.CharSequence getContentTitle() {
      return contentTitle;
    }
    
    /** Sets the value of the 'contentTitle' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentTitle(java.lang.CharSequence value) {
      validate(fields()[35], value);
      this.contentTitle = value;
      fieldSetFlags()[35] = true;
      return this; 
    }
    
    /** Checks whether the 'contentTitle' field has been set */
    public boolean hasContentTitle() {
      return fieldSetFlags()[35];
    }
    
    /** Clears the value of the 'contentTitle' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentTitle() {
      contentTitle = null;
      fieldSetFlags()[35] = false;
      return this;
    }
    
    /** Gets the value of the 'contentText' field */
    public java.lang.CharSequence getContentText() {
      return contentText;
    }
    
    /** Sets the value of the 'contentText' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentText(java.lang.CharSequence value) {
      validate(fields()[36], value);
      this.contentText = value;
      fieldSetFlags()[36] = true;
      return this; 
    }
    
    /** Checks whether the 'contentText' field has been set */
    public boolean hasContentText() {
      return fieldSetFlags()[36];
    }
    
    /** Clears the value of the 'contentText' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentText() {
      contentText = null;
      fieldSetFlags()[36] = false;
      return this;
    }
    
    /** Gets the value of the 'contentTextLen' field */
    public java.lang.Integer getContentTextLen() {
      return contentTextLen;
    }
    
    /** Sets the value of the 'contentTextLen' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentTextLen(int value) {
      validate(fields()[37], value);
      this.contentTextLen = value;
      fieldSetFlags()[37] = true;
      return this; 
    }
    
    /** Checks whether the 'contentTextLen' field has been set */
    public boolean hasContentTextLen() {
      return fieldSetFlags()[37];
    }
    
    /** Clears the value of the 'contentTextLen' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentTextLen() {
      fieldSetFlags()[37] = false;
      return this;
    }
    
    /** Gets the value of the 'pageCategory' field */
    public java.lang.CharSequence getPageCategory() {
      return pageCategory;
    }
    
    /** Sets the value of the 'pageCategory' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPageCategory(java.lang.CharSequence value) {
      validate(fields()[38], value);
      this.pageCategory = value;
      fieldSetFlags()[38] = true;
      return this; 
    }
    
    /** Checks whether the 'pageCategory' field has been set */
    public boolean hasPageCategory() {
      return fieldSetFlags()[38];
    }
    
    /** Clears the value of the 'pageCategory' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPageCategory() {
      pageCategory = null;
      fieldSetFlags()[38] = false;
      return this;
    }
    
    /** Gets the value of the 'contentModifiedTime' field */
    public java.lang.Long getContentModifiedTime() {
      return contentModifiedTime;
    }
    
    /** Sets the value of the 'contentModifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentModifiedTime(long value) {
      validate(fields()[39], value);
      this.contentModifiedTime = value;
      fieldSetFlags()[39] = true;
      return this; 
    }
    
    /** Checks whether the 'contentModifiedTime' field has been set */
    public boolean hasContentModifiedTime() {
      return fieldSetFlags()[39];
    }
    
    /** Clears the value of the 'contentModifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentModifiedTime() {
      fieldSetFlags()[39] = false;
      return this;
    }
    
    /** Gets the value of the 'prevContentModifiedTime' field */
    public java.lang.Long getPrevContentModifiedTime() {
      return prevContentModifiedTime;
    }
    
    /** Sets the value of the 'prevContentModifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevContentModifiedTime(long value) {
      validate(fields()[40], value);
      this.prevContentModifiedTime = value;
      fieldSetFlags()[40] = true;
      return this; 
    }
    
    /** Checks whether the 'prevContentModifiedTime' field has been set */
    public boolean hasPrevContentModifiedTime() {
      return fieldSetFlags()[40];
    }
    
    /** Clears the value of the 'prevContentModifiedTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevContentModifiedTime() {
      fieldSetFlags()[40] = false;
      return this;
    }
    
    /** Gets the value of the 'contentPublishTime' field */
    public java.lang.Long getContentPublishTime() {
      return contentPublishTime;
    }
    
    /** Sets the value of the 'contentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentPublishTime(long value) {
      validate(fields()[41], value);
      this.contentPublishTime = value;
      fieldSetFlags()[41] = true;
      return this; 
    }
    
    /** Checks whether the 'contentPublishTime' field has been set */
    public boolean hasContentPublishTime() {
      return fieldSetFlags()[41];
    }
    
    /** Clears the value of the 'contentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentPublishTime() {
      fieldSetFlags()[41] = false;
      return this;
    }
    
    /** Gets the value of the 'prevContentPublishTime' field */
    public java.lang.Long getPrevContentPublishTime() {
      return prevContentPublishTime;
    }
    
    /** Sets the value of the 'prevContentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevContentPublishTime(long value) {
      validate(fields()[42], value);
      this.prevContentPublishTime = value;
      fieldSetFlags()[42] = true;
      return this; 
    }
    
    /** Checks whether the 'prevContentPublishTime' field has been set */
    public boolean hasPrevContentPublishTime() {
      return fieldSetFlags()[42];
    }
    
    /** Clears the value of the 'prevContentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevContentPublishTime() {
      fieldSetFlags()[42] = false;
      return this;
    }
    
    /** Gets the value of the 'refContentPublishTime' field */
    public java.lang.Long getRefContentPublishTime() {
      return refContentPublishTime;
    }
    
    /** Sets the value of the 'refContentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setRefContentPublishTime(long value) {
      validate(fields()[43], value);
      this.refContentPublishTime = value;
      fieldSetFlags()[43] = true;
      return this; 
    }
    
    /** Checks whether the 'refContentPublishTime' field has been set */
    public boolean hasRefContentPublishTime() {
      return fieldSetFlags()[43];
    }
    
    /** Clears the value of the 'refContentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearRefContentPublishTime() {
      fieldSetFlags()[43] = false;
      return this;
    }
    
    /** Gets the value of the 'prevRefContentPublishTime' field */
    public java.lang.Long getPrevRefContentPublishTime() {
      return prevRefContentPublishTime;
    }
    
    /** Sets the value of the 'prevRefContentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevRefContentPublishTime(long value) {
      validate(fields()[44], value);
      this.prevRefContentPublishTime = value;
      fieldSetFlags()[44] = true;
      return this; 
    }
    
    /** Checks whether the 'prevRefContentPublishTime' field has been set */
    public boolean hasPrevRefContentPublishTime() {
      return fieldSetFlags()[44];
    }
    
    /** Clears the value of the 'prevRefContentPublishTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevRefContentPublishTime() {
      fieldSetFlags()[44] = false;
      return this;
    }
    
    /** Gets the value of the 'pageModelUpdateTime' field */
    public java.lang.Long getPageModelUpdateTime() {
      return pageModelUpdateTime;
    }
    
    /** Sets the value of the 'pageModelUpdateTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPageModelUpdateTime(long value) {
      validate(fields()[45], value);
      this.pageModelUpdateTime = value;
      fieldSetFlags()[45] = true;
      return this; 
    }
    
    /** Checks whether the 'pageModelUpdateTime' field has been set */
    public boolean hasPageModelUpdateTime() {
      return fieldSetFlags()[45];
    }
    
    /** Clears the value of the 'pageModelUpdateTime' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPageModelUpdateTime() {
      fieldSetFlags()[45] = false;
      return this;
    }
    
    /** Gets the value of the 'prevSignature' field */
    public java.nio.ByteBuffer getPrevSignature() {
      return prevSignature;
    }
    
    /** Sets the value of the 'prevSignature' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPrevSignature(java.nio.ByteBuffer value) {
      validate(fields()[46], value);
      this.prevSignature = value;
      fieldSetFlags()[46] = true;
      return this; 
    }
    
    /** Checks whether the 'prevSignature' field has been set */
    public boolean hasPrevSignature() {
      return fieldSetFlags()[46];
    }
    
    /** Clears the value of the 'prevSignature' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPrevSignature() {
      prevSignature = null;
      fieldSetFlags()[46] = false;
      return this;
    }
    
    /** Gets the value of the 'signature' field */
    public java.nio.ByteBuffer getSignature() {
      return signature;
    }
    
    /** Sets the value of the 'signature' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setSignature(java.nio.ByteBuffer value) {
      validate(fields()[47], value);
      this.signature = value;
      fieldSetFlags()[47] = true;
      return this; 
    }
    
    /** Checks whether the 'signature' field has been set */
    public boolean hasSignature() {
      return fieldSetFlags()[47];
    }
    
    /** Clears the value of the 'signature' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearSignature() {
      signature = null;
      fieldSetFlags()[47] = false;
      return this;
    }
    
    /** Gets the value of the 'contentScore' field */
    public java.lang.Float getContentScore() {
      return contentScore;
    }
    
    /** Sets the value of the 'contentScore' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setContentScore(float value) {
      validate(fields()[48], value);
      this.contentScore = value;
      fieldSetFlags()[48] = true;
      return this; 
    }
    
    /** Checks whether the 'contentScore' field has been set */
    public boolean hasContentScore() {
      return fieldSetFlags()[48];
    }
    
    /** Clears the value of the 'contentScore' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearContentScore() {
      fieldSetFlags()[48] = false;
      return this;
    }
    
    /** Gets the value of the 'score' field */
    public java.lang.Float getScore() {
      return score;
    }
    
    /** Sets the value of the 'score' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setScore(float value) {
      validate(fields()[49], value);
      this.score = value;
      fieldSetFlags()[49] = true;
      return this; 
    }
    
    /** Checks whether the 'score' field has been set */
    public boolean hasScore() {
      return fieldSetFlags()[49];
    }
    
    /** Clears the value of the 'score' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearScore() {
      fieldSetFlags()[49] = false;
      return this;
    }
    
    /** Gets the value of the 'sortScore' field */
    public java.lang.CharSequence getSortScore() {
      return sortScore;
    }
    
    /** Sets the value of the 'sortScore' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setSortScore(java.lang.CharSequence value) {
      validate(fields()[50], value);
      this.sortScore = value;
      fieldSetFlags()[50] = true;
      return this; 
    }
    
    /** Checks whether the 'sortScore' field has been set */
    public boolean hasSortScore() {
      return fieldSetFlags()[50];
    }
    
    /** Clears the value of the 'sortScore' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearSortScore() {
      sortScore = null;
      fieldSetFlags()[50] = false;
      return this;
    }
    
    /** Gets the value of the 'pageCounters' field */
    public java.util.Map<java.lang.CharSequence,java.lang.Integer> getPageCounters() {
      return pageCounters;
    }
    
    /** Sets the value of the 'pageCounters' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPageCounters(java.util.Map<java.lang.CharSequence,java.lang.Integer> value) {
      validate(fields()[51], value);
      this.pageCounters = value;
      fieldSetFlags()[51] = true;
      return this; 
    }
    
    /** Checks whether the 'pageCounters' field has been set */
    public boolean hasPageCounters() {
      return fieldSetFlags()[51];
    }
    
    /** Clears the value of the 'pageCounters' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPageCounters() {
      pageCounters = null;
      fieldSetFlags()[51] = false;
      return this;
    }
    
    /** Gets the value of the 'headers' field */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getHeaders() {
      return headers;
    }
    
    /** Sets the value of the 'headers' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setHeaders(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[52], value);
      this.headers = value;
      fieldSetFlags()[52] = true;
      return this; 
    }
    
    /** Checks whether the 'headers' field has been set */
    public boolean hasHeaders() {
      return fieldSetFlags()[52];
    }
    
    /** Clears the value of the 'headers' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearHeaders() {
      headers = null;
      fieldSetFlags()[52] = false;
      return this;
    }
    
    /** Gets the value of the 'links' field */
    public java.util.List<java.lang.CharSequence> getLinks() {
      return links;
    }
    
    /** Sets the value of the 'links' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setLinks(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[53], value);
      this.links = value;
      fieldSetFlags()[53] = true;
      return this; 
    }
    
    /** Checks whether the 'links' field has been set */
    public boolean hasLinks() {
      return fieldSetFlags()[53];
    }
    
    /** Clears the value of the 'links' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearLinks() {
      links = null;
      fieldSetFlags()[53] = false;
      return this;
    }
    
    /** Gets the value of the 'deadLinks' field */
    public java.util.List<java.lang.CharSequence> getDeadLinks() {
      return deadLinks;
    }
    
    /** Sets the value of the 'deadLinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setDeadLinks(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[54], value);
      this.deadLinks = value;
      fieldSetFlags()[54] = true;
      return this; 
    }
    
    /** Checks whether the 'deadLinks' field has been set */
    public boolean hasDeadLinks() {
      return fieldSetFlags()[54];
    }
    
    /** Clears the value of the 'deadLinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearDeadLinks() {
      deadLinks = null;
      fieldSetFlags()[54] = false;
      return this;
    }
    
    /** Gets the value of the 'liveLinks' field */
    public java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> getLiveLinks() {
      return liveLinks;
    }
    
    /** Sets the value of the 'liveLinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setLiveLinks(java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> value) {
      validate(fields()[55], value);
      this.liveLinks = value;
      fieldSetFlags()[55] = true;
      return this; 
    }
    
    /** Checks whether the 'liveLinks' field has been set */
    public boolean hasLiveLinks() {
      return fieldSetFlags()[55];
    }
    
    /** Clears the value of the 'liveLinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearLiveLinks() {
      liveLinks = null;
      fieldSetFlags()[55] = false;
      return this;
    }
    
    /** Gets the value of the 'vividLinks' field */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getVividLinks() {
      return vividLinks;
    }
    
    /** Sets the value of the 'vividLinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setVividLinks(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[56], value);
      this.vividLinks = value;
      fieldSetFlags()[56] = true;
      return this; 
    }
    
    /** Checks whether the 'vividLinks' field has been set */
    public boolean hasVividLinks() {
      return fieldSetFlags()[56];
    }
    
    /** Clears the value of the 'vividLinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearVividLinks() {
      vividLinks = null;
      fieldSetFlags()[56] = false;
      return this;
    }
    
    /** Gets the value of the 'inlinks' field */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getInlinks() {
      return inlinks;
    }
    
    /** Sets the value of the 'inlinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setInlinks(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[57], value);
      this.inlinks = value;
      fieldSetFlags()[57] = true;
      return this; 
    }
    
    /** Checks whether the 'inlinks' field has been set */
    public boolean hasInlinks() {
      return fieldSetFlags()[57];
    }
    
    /** Clears the value of the 'inlinks' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearInlinks() {
      inlinks = null;
      fieldSetFlags()[57] = false;
      return this;
    }
    
    /** Gets the value of the 'markers' field */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMarkers() {
      return markers;
    }
    
    /** Sets the value of the 'markers' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setMarkers(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[58], value);
      this.markers = value;
      fieldSetFlags()[58] = true;
      return this; 
    }
    
    /** Checks whether the 'markers' field has been set */
    public boolean hasMarkers() {
      return fieldSetFlags()[58];
    }
    
    /** Clears the value of the 'markers' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearMarkers() {
      markers = null;
      fieldSetFlags()[58] = false;
      return this;
    }
    
    /** Gets the value of the 'metadata' field */
    public java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> getMetadata() {
      return metadata;
    }
    
    /** Sets the value of the 'metadata' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setMetadata(java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> value) {
      validate(fields()[59], value);
      this.metadata = value;
      fieldSetFlags()[59] = true;
      return this; 
    }
    
    /** Checks whether the 'metadata' field has been set */
    public boolean hasMetadata() {
      return fieldSetFlags()[59];
    }
    
    /** Clears the value of the 'metadata' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearMetadata() {
      metadata = null;
      fieldSetFlags()[59] = false;
      return this;
    }
    
    /** Gets the value of the 'activeDOMStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus getActiveDOMStatus() {
      return activeDOMStatus;
    }
    
    /** Sets the value of the 'activeDOMStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setActiveDOMStatus(ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus value) {
      validate(fields()[60], value);
      this.activeDOMStatus = value;
      fieldSetFlags()[60] = true;
      return this; 
    }
    
    /** Checks whether the 'activeDOMStatus' field has been set */
    public boolean hasActiveDOMStatus() {
      return fieldSetFlags()[60];
    }
    
    /** Clears the value of the 'activeDOMStatus' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearActiveDOMStatus() {
      activeDOMStatus = null;
      fieldSetFlags()[60] = false;
      return this;
    }
    
    /** Gets the value of the 'activeDOMStatTrace' field */
    public java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> getActiveDOMStatTrace() {
      return activeDOMStatTrace;
    }
    
    /** Sets the value of the 'activeDOMStatTrace' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setActiveDOMStatTrace(java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> value) {
      validate(fields()[61], value);
      this.activeDOMStatTrace = value;
      fieldSetFlags()[61] = true;
      return this; 
    }
    
    /** Checks whether the 'activeDOMStatTrace' field has been set */
    public boolean hasActiveDOMStatTrace() {
      return fieldSetFlags()[61];
    }
    
    /** Clears the value of the 'activeDOMStatTrace' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearActiveDOMStatTrace() {
      activeDOMStatTrace = null;
      fieldSetFlags()[61] = false;
      return this;
    }
    
    /** Gets the value of the 'pageModel' field */
    public java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> getPageModel() {
      return pageModel;
    }
    
    /** Sets the value of the 'pageModel' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder setPageModel(java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> value) {
      validate(fields()[62], value);
      this.pageModel = value;
      fieldSetFlags()[62] = true;
      return this; 
    }
    
    /** Checks whether the 'pageModel' field has been set */
    public boolean hasPageModel() {
      return fieldSetFlags()[62];
    }
    
    /** Clears the value of the 'pageModel' field */
    public ai.platon.pulsar.persist.gora.generated.GWebPage.Builder clearPageModel() {
      pageModel = null;
      fieldSetFlags()[62] = false;
      return this;
    }
    
    @Override
    public GWebPage build() {
      try {
        GWebPage record = new GWebPage();
        record.baseUrl = fieldSetFlags()[0] ? this.baseUrl : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.createTime = fieldSetFlags()[1] ? this.createTime : (java.lang.Long) defaultValue(fields()[1]);
        record.distance = fieldSetFlags()[2] ? this.distance : (java.lang.Integer) defaultValue(fields()[2]);
        record.fetchCount = fieldSetFlags()[3] ? this.fetchCount : (java.lang.Integer) defaultValue(fields()[3]);
        record.fetchPriority = fieldSetFlags()[4] ? this.fetchPriority : (java.lang.Integer) defaultValue(fields()[4]);
        record.fetchInterval = fieldSetFlags()[5] ? this.fetchInterval : (java.lang.Integer) defaultValue(fields()[5]);
        record.zoneId = fieldSetFlags()[6] ? this.zoneId : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.params = fieldSetFlags()[7] ? this.params : (java.lang.CharSequence) defaultValue(fields()[7]);
        record.batchId = fieldSetFlags()[8] ? this.batchId : (java.lang.CharSequence) defaultValue(fields()[8]);
        record.resource = fieldSetFlags()[9] ? this.resource : (java.lang.Integer) defaultValue(fields()[9]);
        record.crawlStatus = fieldSetFlags()[10] ? this.crawlStatus : (java.lang.Integer) defaultValue(fields()[10]);
        record.browser = fieldSetFlags()[11] ? this.browser : (java.lang.CharSequence) defaultValue(fields()[11]);
        record.proxy = fieldSetFlags()[12] ? this.proxy : (java.lang.CharSequence) defaultValue(fields()[12]);
        record.prevFetchTime = fieldSetFlags()[13] ? this.prevFetchTime : (java.lang.Long) defaultValue(fields()[13]);
        record.prevCrawlTime1 = fieldSetFlags()[14] ? this.prevCrawlTime1 : (java.lang.Long) defaultValue(fields()[14]);
        record.fetchTime = fieldSetFlags()[15] ? this.fetchTime : (java.lang.Long) defaultValue(fields()[15]);
        record.fetchRetries = fieldSetFlags()[16] ? this.fetchRetries : (java.lang.Integer) defaultValue(fields()[16]);
        record.reprUrl = fieldSetFlags()[17] ? this.reprUrl : (java.lang.CharSequence) defaultValue(fields()[17]);
        record.prevModifiedTime = fieldSetFlags()[18] ? this.prevModifiedTime : (java.lang.Long) defaultValue(fields()[18]);
        record.modifiedTime = fieldSetFlags()[19] ? this.modifiedTime : (java.lang.Long) defaultValue(fields()[19]);
        record.protocolStatus = fieldSetFlags()[20] ? this.protocolStatus : (ai.platon.pulsar.persist.gora.generated.GProtocolStatus) defaultValue(fields()[20]);
        record.encoding = fieldSetFlags()[21] ? this.encoding : (java.lang.CharSequence) defaultValue(fields()[21]);
        record.contentType = fieldSetFlags()[22] ? this.contentType : (java.lang.CharSequence) defaultValue(fields()[22]);
        record.content = fieldSetFlags()[23] ? this.content : (java.nio.ByteBuffer) defaultValue(fields()[23]);
        record.contentLength = fieldSetFlags()[24] ? this.contentLength : (java.lang.Long) defaultValue(fields()[24]);
        record.lastContentLength = fieldSetFlags()[25] ? this.lastContentLength : (java.lang.Long) defaultValue(fields()[25]);
        record.aveContentLength = fieldSetFlags()[26] ? this.aveContentLength : (java.lang.Long) defaultValue(fields()[26]);
        record.persistedContentLength = fieldSetFlags()[27] ? this.persistedContentLength : (java.lang.Long) defaultValue(fields()[27]);
        record.referrer = fieldSetFlags()[28] ? this.referrer : (java.lang.CharSequence) defaultValue(fields()[28]);
        record.htmlIntegrity = fieldSetFlags()[29] ? this.htmlIntegrity : (java.lang.CharSequence) defaultValue(fields()[29]);
        record.anchor = fieldSetFlags()[30] ? this.anchor : (java.lang.CharSequence) defaultValue(fields()[30]);
        record.anchorOrder = fieldSetFlags()[31] ? this.anchorOrder : (java.lang.Integer) defaultValue(fields()[31]);
        record.parseStatus = fieldSetFlags()[32] ? this.parseStatus : (ai.platon.pulsar.persist.gora.generated.GParseStatus) defaultValue(fields()[32]);
        record.pageTitle = fieldSetFlags()[33] ? this.pageTitle : (java.lang.CharSequence) defaultValue(fields()[33]);
        record.pageText = fieldSetFlags()[34] ? this.pageText : (java.lang.CharSequence) defaultValue(fields()[34]);
        record.contentTitle = fieldSetFlags()[35] ? this.contentTitle : (java.lang.CharSequence) defaultValue(fields()[35]);
        record.contentText = fieldSetFlags()[36] ? this.contentText : (java.lang.CharSequence) defaultValue(fields()[36]);
        record.contentTextLen = fieldSetFlags()[37] ? this.contentTextLen : (java.lang.Integer) defaultValue(fields()[37]);
        record.pageCategory = fieldSetFlags()[38] ? this.pageCategory : (java.lang.CharSequence) defaultValue(fields()[38]);
        record.contentModifiedTime = fieldSetFlags()[39] ? this.contentModifiedTime : (java.lang.Long) defaultValue(fields()[39]);
        record.prevContentModifiedTime = fieldSetFlags()[40] ? this.prevContentModifiedTime : (java.lang.Long) defaultValue(fields()[40]);
        record.contentPublishTime = fieldSetFlags()[41] ? this.contentPublishTime : (java.lang.Long) defaultValue(fields()[41]);
        record.prevContentPublishTime = fieldSetFlags()[42] ? this.prevContentPublishTime : (java.lang.Long) defaultValue(fields()[42]);
        record.refContentPublishTime = fieldSetFlags()[43] ? this.refContentPublishTime : (java.lang.Long) defaultValue(fields()[43]);
        record.prevRefContentPublishTime = fieldSetFlags()[44] ? this.prevRefContentPublishTime : (java.lang.Long) defaultValue(fields()[44]);
        record.pageModelUpdateTime = fieldSetFlags()[45] ? this.pageModelUpdateTime : (java.lang.Long) defaultValue(fields()[45]);
        record.prevSignature = fieldSetFlags()[46] ? this.prevSignature : (java.nio.ByteBuffer) defaultValue(fields()[46]);
        record.signature = fieldSetFlags()[47] ? this.signature : (java.nio.ByteBuffer) defaultValue(fields()[47]);
        record.contentScore = fieldSetFlags()[48] ? this.contentScore : (java.lang.Float) defaultValue(fields()[48]);
        record.score = fieldSetFlags()[49] ? this.score : (java.lang.Float) defaultValue(fields()[49]);
        record.sortScore = fieldSetFlags()[50] ? this.sortScore : (java.lang.CharSequence) defaultValue(fields()[50]);
        record.pageCounters = fieldSetFlags()[51] ? this.pageCounters : (java.util.Map<java.lang.CharSequence,java.lang.Integer>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[51]));
        record.headers = fieldSetFlags()[52] ? this.headers : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[52]));
        record.links = fieldSetFlags()[53] ? this.links : (java.util.List<java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[53]));
        record.deadLinks = fieldSetFlags()[54] ? this.deadLinks : (java.util.List<java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[54]));
        record.liveLinks = fieldSetFlags()[55] ? this.liveLinks : (java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[55]));
        record.vividLinks = fieldSetFlags()[56] ? this.vividLinks : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[56]));
        record.inlinks = fieldSetFlags()[57] ? this.inlinks : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[57]));
        record.markers = fieldSetFlags()[58] ? this.markers : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[58]));
        record.metadata = fieldSetFlags()[59] ? this.metadata : (java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[59]));
        record.activeDOMStatus = fieldSetFlags()[60] ? this.activeDOMStatus : (ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus) defaultValue(fields()[60]);
        record.activeDOMStatTrace = fieldSetFlags()[61] ? this.activeDOMStatTrace : (java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[61]));
        record.pageModel = fieldSetFlags()[62] ? this.pageModel : (java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[62]));
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public GWebPage.Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  public GWebPage newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends GWebPage implements org.apache.gora.persistency.Tombstone {
  
      private Tombstone() { }
  
	  		  /**
	   * Gets the value of the 'baseUrl' field.
		   */
	  public java.lang.CharSequence getBaseUrl() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'baseUrl' field.
		   * @param value the value to set.
	   */
	  public void setBaseUrl(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'baseUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isBaseUrlDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'createTime' field.
		   */
	  public java.lang.Long getCreateTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'createTime' field.
		   * @param value the value to set.
	   */
	  public void setCreateTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'createTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCreateTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'distance' field.
		   */
	  public java.lang.Integer getDistance() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'distance' field.
		   * @param value the value to set.
	   */
	  public void setDistance(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'distance' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDistanceDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'fetchCount' field.
		   */
	  public java.lang.Integer getFetchCount() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'fetchCount' field.
		   * @param value the value to set.
	   */
	  public void setFetchCount(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'fetchCount' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchCountDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'fetchPriority' field.
		   */
	  public java.lang.Integer getFetchPriority() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'fetchPriority' field.
		   * @param value the value to set.
	   */
	  public void setFetchPriority(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'fetchPriority' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchPriorityDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'fetchInterval' field.
		   */
	  public java.lang.Integer getFetchInterval() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'fetchInterval' field.
		   * @param value the value to set.
	   */
	  public void setFetchInterval(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'fetchInterval' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchIntervalDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'zoneId' field.
		   */
	  public java.lang.CharSequence getZoneId() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'zoneId' field.
		   * @param value the value to set.
	   */
	  public void setZoneId(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'zoneId' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isZoneIdDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'params' field.
		   */
	  public java.lang.CharSequence getParams() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'params' field.
		   * @param value the value to set.
	   */
	  public void setParams(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'params' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isParamsDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'batchId' field.
		   */
	  public java.lang.CharSequence getBatchId() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'batchId' field.
		   * @param value the value to set.
	   */
	  public void setBatchId(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'batchId' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isBatchIdDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'resource' field.
		   */
	  public java.lang.Integer getResource() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'resource' field.
		   * @param value the value to set.
	   */
	  public void setResource(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'resource' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isResourceDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'crawlStatus' field.
		   */
	  public java.lang.Integer getCrawlStatus() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'crawlStatus' field.
		   * @param value the value to set.
	   */
	  public void setCrawlStatus(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'crawlStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCrawlStatusDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'browser' field.
		   */
	  public java.lang.CharSequence getBrowser() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'browser' field.
		   * @param value the value to set.
	   */
	  public void setBrowser(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'browser' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isBrowserDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'proxy' field.
		   */
	  public java.lang.CharSequence getProxy() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'proxy' field.
		   * @param value the value to set.
	   */
	  public void setProxy(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'proxy' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isProxyDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevFetchTime' field.
		   */
	  public java.lang.Long getPrevFetchTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevFetchTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevFetchTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevFetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevFetchTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevCrawlTime1' field.
		   */
	  public java.lang.Long getPrevCrawlTime1() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevCrawlTime1' field.
		   * @param value the value to set.
	   */
	  public void setPrevCrawlTime1(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevCrawlTime1' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevCrawlTime1Dirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'fetchTime' field.
		   */
	  public java.lang.Long getFetchTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'fetchTime' field.
		   * @param value the value to set.
	   */
	  public void setFetchTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'fetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'fetchRetries' field.
		   */
	  public java.lang.Integer getFetchRetries() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'fetchRetries' field.
		   * @param value the value to set.
	   */
	  public void setFetchRetries(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'fetchRetries' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchRetriesDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'reprUrl' field.
		   */
	  public java.lang.CharSequence getReprUrl() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'reprUrl' field.
		   * @param value the value to set.
	   */
	  public void setReprUrl(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'reprUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isReprUrlDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevModifiedTime' field.
		   */
	  public java.lang.Long getPrevModifiedTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevModifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevModifiedTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevModifiedTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'modifiedTime' field.
		   */
	  public java.lang.Long getModifiedTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'modifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setModifiedTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'modifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isModifiedTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'protocolStatus' field.
		   */
	  public ai.platon.pulsar.persist.gora.generated.GProtocolStatus getProtocolStatus() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'protocolStatus' field.
		   * @param value the value to set.
	   */
	  public void setProtocolStatus(ai.platon.pulsar.persist.gora.generated.GProtocolStatus value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'protocolStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isProtocolStatusDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'encoding' field.
		   */
	  public java.lang.CharSequence getEncoding() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'encoding' field.
		   * @param value the value to set.
	   */
	  public void setEncoding(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'encoding' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isEncodingDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentType' field.
		   */
	  public java.lang.CharSequence getContentType() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentType' field.
		   * @param value the value to set.
	   */
	  public void setContentType(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentType' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTypeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'content' field.
	   * The entire raw document content e.g. raw XHTML	   */
	  public java.nio.ByteBuffer getContent() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'content' field.
	   * The entire raw document content e.g. raw XHTML	   * @param value the value to set.
	   */
	  public void setContent(java.nio.ByteBuffer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'content' field. A field is dirty if it represents a change that has not yet been written to the database.
	   * The entire raw document content e.g. raw XHTML	   * @param value the value to set.
	   */
	  public boolean isContentDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentLength' field.
		   */
	  public java.lang.Long getContentLength() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentLength' field.
		   * @param value the value to set.
	   */
	  public void setContentLength(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentLengthDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'lastContentLength' field.
		   */
	  public java.lang.Long getLastContentLength() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'lastContentLength' field.
		   * @param value the value to set.
	   */
	  public void setLastContentLength(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'lastContentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isLastContentLengthDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'aveContentLength' field.
		   */
	  public java.lang.Long getAveContentLength() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'aveContentLength' field.
		   * @param value the value to set.
	   */
	  public void setAveContentLength(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'aveContentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAveContentLengthDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'persistedContentLength' field.
		   */
	  public java.lang.Long getPersistedContentLength() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'persistedContentLength' field.
		   * @param value the value to set.
	   */
	  public void setPersistedContentLength(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'persistedContentLength' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPersistedContentLengthDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'referrer' field.
		   */
	  public java.lang.CharSequence getReferrer() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'referrer' field.
		   * @param value the value to set.
	   */
	  public void setReferrer(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'referrer' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isReferrerDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'htmlIntegrity' field.
		   */
	  public java.lang.CharSequence getHtmlIntegrity() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'htmlIntegrity' field.
		   * @param value the value to set.
	   */
	  public void setHtmlIntegrity(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'htmlIntegrity' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isHtmlIntegrityDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'anchor' field.
		   */
	  public java.lang.CharSequence getAnchor() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'anchor' field.
		   * @param value the value to set.
	   */
	  public void setAnchor(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'anchor' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAnchorDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'anchorOrder' field.
		   */
	  public java.lang.Integer getAnchorOrder() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'anchorOrder' field.
		   * @param value the value to set.
	   */
	  public void setAnchorOrder(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'anchorOrder' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAnchorOrderDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'parseStatus' field.
		   */
	  public ai.platon.pulsar.persist.gora.generated.GParseStatus getParseStatus() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'parseStatus' field.
		   * @param value the value to set.
	   */
	  public void setParseStatus(ai.platon.pulsar.persist.gora.generated.GParseStatus value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'parseStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isParseStatusDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'pageTitle' field.
		   */
	  public java.lang.CharSequence getPageTitle() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'pageTitle' field.
		   * @param value the value to set.
	   */
	  public void setPageTitle(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'pageTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageTitleDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'pageText' field.
		   */
	  public java.lang.CharSequence getPageText() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'pageText' field.
		   * @param value the value to set.
	   */
	  public void setPageText(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'pageText' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageTextDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentTitle' field.
		   */
	  public java.lang.CharSequence getContentTitle() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentTitle' field.
		   * @param value the value to set.
	   */
	  public void setContentTitle(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTitleDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentText' field.
		   */
	  public java.lang.CharSequence getContentText() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentText' field.
		   * @param value the value to set.
	   */
	  public void setContentText(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentText' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTextDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentTextLen' field.
		   */
	  public java.lang.Integer getContentTextLen() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentTextLen' field.
		   * @param value the value to set.
	   */
	  public void setContentTextLen(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentTextLen' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTextLenDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'pageCategory' field.
		   */
	  public java.lang.CharSequence getPageCategory() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'pageCategory' field.
		   * @param value the value to set.
	   */
	  public void setPageCategory(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'pageCategory' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageCategoryDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentModifiedTime' field.
		   */
	  public java.lang.Long getContentModifiedTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentModifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setContentModifiedTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentModifiedTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevContentModifiedTime' field.
		   */
	  public java.lang.Long getPrevContentModifiedTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevContentModifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevContentModifiedTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevContentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevContentModifiedTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentPublishTime' field.
		   */
	  public java.lang.Long getContentPublishTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setContentPublishTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentPublishTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevContentPublishTime' field.
		   */
	  public java.lang.Long getPrevContentPublishTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevContentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevContentPublishTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevContentPublishTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'refContentPublishTime' field.
		   */
	  public java.lang.Long getRefContentPublishTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'refContentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setRefContentPublishTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'refContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isRefContentPublishTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevRefContentPublishTime' field.
		   */
	  public java.lang.Long getPrevRefContentPublishTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevRefContentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevRefContentPublishTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevRefContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevRefContentPublishTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'pageModelUpdateTime' field.
		   */
	  public java.lang.Long getPageModelUpdateTime() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'pageModelUpdateTime' field.
		   * @param value the value to set.
	   */
	  public void setPageModelUpdateTime(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'pageModelUpdateTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageModelUpdateTimeDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'prevSignature' field.
		   */
	  public java.nio.ByteBuffer getPrevSignature() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prevSignature' field.
		   * @param value the value to set.
	   */
	  public void setPrevSignature(java.nio.ByteBuffer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prevSignature' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevSignatureDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'signature' field.
		   */
	  public java.nio.ByteBuffer getSignature() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'signature' field.
		   * @param value the value to set.
	   */
	  public void setSignature(java.nio.ByteBuffer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'signature' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isSignatureDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'contentScore' field.
		   */
	  public java.lang.Float getContentScore() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'contentScore' field.
		   * @param value the value to set.
	   */
	  public void setContentScore(java.lang.Float value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'contentScore' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentScoreDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'score' field.
		   */
	  public java.lang.Float getScore() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'score' field.
		   * @param value the value to set.
	   */
	  public void setScore(java.lang.Float value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'score' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isScoreDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'sortScore' field.
		   */
	  public java.lang.CharSequence getSortScore() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'sortScore' field.
		   * @param value the value to set.
	   */
	  public void setSortScore(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'sortScore' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isSortScoreDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'pageCounters' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.Integer> getPageCounters() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'pageCounters' field.
		   * @param value the value to set.
	   */
	  public void setPageCounters(java.util.Map<java.lang.CharSequence,java.lang.Integer> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'pageCounters' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageCountersDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'headers' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getHeaders() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'headers' field.
		   * @param value the value to set.
	   */
	  public void setHeaders(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'headers' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isHeadersDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'links' field.
		   */
	  public java.util.List<java.lang.CharSequence> getLinks() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'links' field.
		   * @param value the value to set.
	   */
	  public void setLinks(java.util.List<java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'links' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isLinksDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'deadLinks' field.
		   */
	  public java.util.List<java.lang.CharSequence> getDeadLinks() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'deadLinks' field.
		   * @param value the value to set.
	   */
	  public void setDeadLinks(java.util.List<java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'deadLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDeadLinksDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'liveLinks' field.
		   */
	  public java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> getLiveLinks() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'liveLinks' field.
		   * @param value the value to set.
	   */
	  public void setLiveLinks(java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GHypeLink> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'liveLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isLiveLinksDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'vividLinks' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getVividLinks() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'vividLinks' field.
		   * @param value the value to set.
	   */
	  public void setVividLinks(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'vividLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isVividLinksDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'inlinks' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getInlinks() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'inlinks' field.
		   * @param value the value to set.
	   */
	  public void setInlinks(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'inlinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isInlinksDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'markers' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMarkers() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'markers' field.
		   * @param value the value to set.
	   */
	  public void setMarkers(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'markers' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMarkersDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'metadata' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> getMetadata() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'metadata' field.
		   * @param value the value to set.
	   */
	  public void setMetadata(java.util.Map<java.lang.CharSequence,java.nio.ByteBuffer> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'metadata' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMetadataDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'activeDOMStatus' field.
		   */
	  public ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus getActiveDOMStatus() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'activeDOMStatus' field.
		   * @param value the value to set.
	   */
	  public void setActiveDOMStatus(ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'activeDOMStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isActiveDOMStatusDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'activeDOMStatTrace' field.
		   */
	  public java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> getActiveDOMStatTrace() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'activeDOMStatTrace' field.
		   * @param value the value to set.
	   */
	  public void setActiveDOMStatTrace(java.util.Map<java.lang.CharSequence,ai.platon.pulsar.persist.gora.generated.GActiveDOMStat> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'activeDOMStatTrace' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isActiveDOMStatTraceDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'pageModel' field.
		   */
	  public java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> getPageModel() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'pageModel' field.
		   * @param value the value to set.
	   */
	  public void setPageModel(java.util.List<ai.platon.pulsar.persist.gora.generated.GFieldGroup> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'pageModel' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageModelDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
		  
  }

  private static final org.apache.avro.io.DatumWriter
            DATUM_WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);
  private static final org.apache.avro.io.DatumReader
            DATUM_READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  /**
   * Writes AVRO data bean to output stream in the form of AVRO Binary encoding format. This will transform
   * AVRO data bean from its Java object form to it s serializable form.
   *
   * @param out java.io.ObjectOutput output stream to write data bean in serializable form
   */
  @Override
  public void writeExternal(java.io.ObjectOutput out)
          throws java.io.IOException {
    out.write(super.getDirtyBytes().array());
    DATUM_WRITER$.write(this, org.apache.avro.io.EncoderFactory.get()
            .directBinaryEncoder((java.io.OutputStream) out,
                    null));
  }

  /**
   * Reads AVRO data bean from input stream in it s AVRO Binary encoding format to Java object format.
   * This will transform AVRO data bean from it s serializable form to deserialized Java object form.
   *
   * @param in java.io.ObjectOutput input stream to read data bean in serializable form
   */
  @Override
  public void readExternal(java.io.ObjectInput in)
          throws java.io.IOException {
    byte[] __g__dirty = new byte[getFieldsCount()];
    in.read(__g__dirty);
    super.setDirtyBytes(java.nio.ByteBuffer.wrap(__g__dirty));
    DATUM_READER$.read(this, org.apache.avro.io.DecoderFactory.get()
            .directBinaryDecoder((java.io.InputStream) in,
                    null));
  }
  
}

