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

/**
 * <p>GWebPage class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class GWebPage extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  /** Constant <code>SCHEMA$</code> */
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"GWebPage\",\"namespace\":\"ai.platon.pulsar.persist.gora.generated\",\"fields\":[{\"name\":\"createTime\",\"type\":\"long\",\"default\":0},{\"name\":\"distance\",\"type\":\"int\",\"default\":-1},{\"name\":\"fetchCount\",\"type\":\"int\",\"default\":0},{\"name\":\"fetchPriority\",\"type\":\"int\",\"default\":0},{\"name\":\"fetchInterval\",\"type\":\"int\",\"default\":0},{\"name\":\"zoneId\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"options\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"batchId\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"crawlStatus\",\"type\":\"int\",\"default\":0},{\"name\":\"prevFetchTime\",\"type\":\"long\",\"default\":0},{\"name\":\"fetchTime\",\"type\":\"long\",\"default\":0},{\"name\":\"fetchRetries\",\"type\":\"int\",\"default\":0},{\"name\":\"reprUrl\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"prevModifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"modifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"protocolStatus\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"GProtocolStatus\",\"fields\":[{\"name\":\"majorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"minorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"args\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}}]}],\"default\":null},{\"name\":\"encoding\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentType\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"content\",\"type\":[\"null\",\"bytes\"],\"doc\":\"The entire raw document content e.g. raw XHTML\",\"default\":null},{\"name\":\"baseUrl\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"referrer\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"anchor\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"anchorOrder\",\"type\":\"int\",\"default\":-1},{\"name\":\"parseStatus\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"GParseStatus\",\"fields\":[{\"name\":\"majorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"minorCode\",\"type\":\"int\",\"default\":0},{\"name\":\"args\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}}]}],\"default\":null},{\"name\":\"pageTitle\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"pageText\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentTitle\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentText\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentTextLen\",\"type\":\"int\",\"default\":0},{\"name\":\"pageCategory\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"contentModifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevContentModifiedTime\",\"type\":\"long\",\"default\":0},{\"name\":\"contentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevContentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"refContentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevRefContentPublishTime\",\"type\":\"long\",\"default\":0},{\"name\":\"prevSignature\",\"type\":[\"null\",\"bytes\"],\"default\":null},{\"name\":\"signature\",\"type\":[\"null\",\"bytes\"],\"default\":null},{\"name\":\"contentScore\",\"type\":\"float\",\"default\":0},{\"name\":\"score\",\"type\":\"float\",\"default\":0},{\"name\":\"sortScore\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"pageCounters\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"int\"]},\"default\":{}},{\"name\":\"headers\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"links\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":[]},{\"name\":\"liveLinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",{\"type\":\"record\",\"name\":\"GHypeLink\",\"fields\":[{\"name\":\"url\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"anchor\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"order\",\"type\":\"int\",\"default\":0}]}]},\"default\":[]},{\"name\":\"vividLinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"deadLinks\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":[]},{\"name\":\"inlinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"markers\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"metadata\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"bytes\"]},\"default\":{}},{\"name\":\"pageModel\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"GFieldGroup\",\"fields\":[{\"name\":\"id\",\"type\":\"long\",\"default\":0},{\"name\":\"parentId\",\"type\":\"long\",\"default\":0},{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"fields\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}}]}},\"default\":[]}]}");
  private static final long serialVersionUID = 106768163334616612L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    CREATE_TIME(0, "createTime"),
    DISTANCE(1, "distance"),
    FETCH_COUNT(2, "fetchCount"),
    FETCH_PRIORITY(3, "fetchPriority"),
    FETCH_INTERVAL(4, "fetchInterval"),
    ZONE_ID(5, "zoneId"),
    OPTIONS(6, "options"),
    BATCH_ID(7, "batchId"),
    CRAWL_STATUS(8, "crawlStatus"),
    PREV_FETCH_TIME(9, "prevFetchTime"),
    FETCH_TIME(10, "fetchTime"),
    FETCH_RETRIES(11, "fetchRetries"),
    REPR_URL(12, "reprUrl"),
    PREV_MODIFIED_TIME(13, "prevModifiedTime"),
    MODIFIED_TIME(14, "modifiedTime"),
    PROTOCOL_STATUS(15, "protocolStatus"),
    ENCODING(16, "encoding"),
    CONTENT_TYPE(17, "contentType"),
    CONTENT(18, "content"),
    BASE_URL(19, "baseUrl"),
    REFERRER(20, "referrer"),
    ANCHOR(21, "anchor"),
    ANCHOR_ORDER(22, "anchorOrder"),
    PARSE_STATUS(23, "parseStatus"),
    PAGE_TITLE(24, "pageTitle"),
    PAGE_TEXT(25, "pageText"),
    CONTENT_TITLE(26, "contentTitle"),
    CONTENT_TEXT(27, "contentText"),
    CONTENT_TEXT_LEN(28, "contentTextLen"),
    PAGE_CATEGORY(29, "pageCategory"),
    CONTENT_MODIFIED_TIME(30, "contentModifiedTime"),
    PREV_CONTENT_MODIFIED_TIME(31, "prevContentModifiedTime"),
    CONTENT_PUBLISH_TIME(32, "contentPublishTime"),
    PREV_CONTENT_PUBLISH_TIME(33, "prevContentPublishTime"),
    REF_CONTENT_PUBLISH_TIME(34, "refContentPublishTime"),
    PREV_REF_CONTENT_PUBLISH_TIME(35, "prevRefContentPublishTime"),
    PREV_SIGNATURE(36, "prevSignature"),
    SIGNATURE(37, "signature"),
    CONTENT_SCORE(38, "contentScore"),
    SCORE(39, "score"),
    SORT_SCORE(40, "sortScore"),
    PAGE_COUNTERS(41, "pageCounters"),
    HEADERS(42, "headers"),
    LINKS(43, "links"),
    LIVE_LINKS(44, "liveLinks"),
    VIVID_LINKS(45, "vividLinks"),
    DEAD_LINKS(46, "deadLinks"),
    INLINKS(47, "inlinks"),
    MARKERS(48, "markers"),
    METADATA(49, "metadata"),
    PAGE_MODEL(50, "pageModel"),
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

  /** Constant <code>_ALL_FIELDS</code> */
  public static final String[] _ALL_FIELDS = {
  "createTime",
  "distance",
  "fetchCount",
  "fetchPriority",
  "fetchInterval",
  "zoneId",
  "options",
  "batchId",
  "crawlStatus",
  "prevFetchTime",
  "fetchTime",
  "fetchRetries",
  "reprUrl",
  "prevModifiedTime",
  "modifiedTime",
  "protocolStatus",
  "encoding",
  "contentType",
  "content",
  "baseUrl",
  "referrer",
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
  "prevSignature",
  "signature",
  "contentScore",
  "score",
  "sortScore",
  "pageCounters",
  "headers",
  "links",
  "liveLinks",
  "vividLinks",
  "deadLinks",
  "inlinks",
  "markers",
  "metadata",
  "pageModel",
  };

  /**
   * Gets the total field count.
   *
   * @return int field count
   */
  public int getFieldsCount() {
    return GWebPage._ALL_FIELDS.length;
  }

  private long createTime;
  private int distance;
  private int fetchCount;
  private int fetchPriority;
  private int fetchInterval;
  private CharSequence zoneId;
  private CharSequence options;
  private CharSequence batchId;
  private int crawlStatus;
  private long prevFetchTime;
  private long fetchTime;
  private int fetchRetries;
  private CharSequence reprUrl;
  private long prevModifiedTime;
  private long modifiedTime;
  private GProtocolStatus protocolStatus;
  private CharSequence encoding;
  private CharSequence contentType;
  /** The entire raw document content e.g. raw XHTML */
  private java.nio.ByteBuffer content;
  private CharSequence baseUrl;
  private CharSequence referrer;
  private CharSequence anchor;
  private int anchorOrder;
  private GParseStatus parseStatus;
  private CharSequence pageTitle;
  private CharSequence pageText;
  private CharSequence contentTitle;
  private CharSequence contentText;
  private int contentTextLen;
  private CharSequence pageCategory;
  private long contentModifiedTime;
  private long prevContentModifiedTime;
  private long contentPublishTime;
  private long prevContentPublishTime;
  private long refContentPublishTime;
  private long prevRefContentPublishTime;
  private java.nio.ByteBuffer prevSignature;
  private java.nio.ByteBuffer signature;
  private float contentScore;
  private float score;
  private CharSequence sortScore;
  private java.util.Map<CharSequence, Integer> pageCounters;
  private java.util.Map<CharSequence, CharSequence> headers;
  private java.util.List<CharSequence> links;
  private java.util.Map<CharSequence, GHypeLink> liveLinks;
  private java.util.Map<CharSequence, CharSequence> vividLinks;
  private java.util.List<CharSequence> deadLinks;
  private java.util.Map<CharSequence, CharSequence> inlinks;
  private java.util.Map<CharSequence, CharSequence> markers;
  private java.util.Map<CharSequence,java.nio.ByteBuffer> metadata;
  private java.util.List<GFieldGroup> pageModel;
  /**
   * <p>getSchema.</p>
   *
   * @return a {@link org.apache.avro.Schema} object.
   */
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  /** {@inheritDoc} */
  public Object get(int field$) {
    switch (field$) {
    case 0: return createTime;
    case 1: return distance;
    case 2: return fetchCount;
    case 3: return fetchPriority;
    case 4: return fetchInterval;
    case 5: return zoneId;
    case 6: return options;
    case 7: return batchId;
    case 8: return crawlStatus;
    case 9: return prevFetchTime;
    case 10: return fetchTime;
    case 11: return fetchRetries;
    case 12: return reprUrl;
    case 13: return prevModifiedTime;
    case 14: return modifiedTime;
    case 15: return protocolStatus;
    case 16: return encoding;
    case 17: return contentType;
    case 18: return content;
    case 19: return baseUrl;
    case 20: return referrer;
    case 21: return anchor;
    case 22: return anchorOrder;
    case 23: return parseStatus;
    case 24: return pageTitle;
    case 25: return pageText;
    case 26: return contentTitle;
    case 27: return contentText;
    case 28: return contentTextLen;
    case 29: return pageCategory;
    case 30: return contentModifiedTime;
    case 31: return prevContentModifiedTime;
    case 32: return contentPublishTime;
    case 33: return prevContentPublishTime;
    case 34: return refContentPublishTime;
    case 35: return prevRefContentPublishTime;
    case 36: return prevSignature;
    case 37: return signature;
    case 38: return contentScore;
    case 39: return score;
    case 40: return sortScore;
    case 41: return pageCounters;
    case 42: return headers;
    case 43: return links;
    case 44: return liveLinks;
    case 45: return vividLinks;
    case 46: return deadLinks;
    case 47: return inlinks;
    case 48: return markers;
    case 49: return metadata;
    case 50: return pageModel;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  /** {@inheritDoc} */
  @SuppressWarnings(value="unchecked")
  public void put(int field$, Object value) {
    switch (field$) {
    case 0: createTime = (Long)(value); break;
    case 1: distance = (Integer)(value); break;
    case 2: fetchCount = (Integer)(value); break;
    case 3: fetchPriority = (Integer)(value); break;
    case 4: fetchInterval = (Integer)(value); break;
    case 5: zoneId = (CharSequence)(value); break;
    case 6: options = (CharSequence)(value); break;
    case 7: batchId = (CharSequence)(value); break;
    case 8: crawlStatus = (Integer)(value); break;
    case 9: prevFetchTime = (Long)(value); break;
    case 10: fetchTime = (Long)(value); break;
    case 11: fetchRetries = (Integer)(value); break;
    case 12: reprUrl = (CharSequence)(value); break;
    case 13: prevModifiedTime = (Long)(value); break;
    case 14: modifiedTime = (Long)(value); break;
    case 15: protocolStatus = (GProtocolStatus)(value); break;
    case 16: encoding = (CharSequence)(value); break;
    case 17: contentType = (CharSequence)(value); break;
    case 18: content = (java.nio.ByteBuffer)(value); break;
    case 19: baseUrl = (CharSequence)(value); break;
    case 20: referrer = (CharSequence)(value); break;
    case 21: anchor = (CharSequence)(value); break;
    case 22: anchorOrder = (Integer)(value); break;
    case 23: parseStatus = (GParseStatus)(value); break;
    case 24: pageTitle = (CharSequence)(value); break;
    case 25: pageText = (CharSequence)(value); break;
    case 26: contentTitle = (CharSequence)(value); break;
    case 27: contentText = (CharSequence)(value); break;
    case 28: contentTextLen = (Integer)(value); break;
    case 29: pageCategory = (CharSequence)(value); break;
    case 30: contentModifiedTime = (Long)(value); break;
    case 31: prevContentModifiedTime = (Long)(value); break;
    case 32: contentPublishTime = (Long)(value); break;
    case 33: prevContentPublishTime = (Long)(value); break;
    case 34: refContentPublishTime = (Long)(value); break;
    case 35: prevRefContentPublishTime = (Long)(value); break;
    case 36: prevSignature = (java.nio.ByteBuffer)(value); break;
    case 37: signature = (java.nio.ByteBuffer)(value); break;
    case 38: contentScore = (Float)(value); break;
    case 39: score = (Float)(value); break;
    case 40: sortScore = (CharSequence)(value); break;
    case 41: pageCounters = (java.util.Map<CharSequence, Integer>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 42: headers = (java.util.Map<CharSequence, CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 43: links = (java.util.List<CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 44: liveLinks = (java.util.Map<CharSequence, GHypeLink>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 45: vividLinks = (java.util.Map<CharSequence, CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 46: deadLinks = (java.util.List<CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 47: inlinks = (java.util.Map<CharSequence, CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 48: markers = (java.util.Map<CharSequence, CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 49: metadata = (java.util.Map<CharSequence,java.nio.ByteBuffer>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 50: pageModel = (java.util.List<GFieldGroup>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'createTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getCreateTime() {
    return createTime;
  }

  /**
   * Sets the value of the 'createTime' field.
   *
   * @param value the value to set.
   */
  public void setCreateTime(Long value) {
    this.createTime = value;
    setDirty(0);
  }

  /**
   * Checks the dirty status of the 'createTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isCreateTimeDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'distance' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getDistance() {
    return distance;
  }

  /**
   * Sets the value of the 'distance' field.
   *
   * @param value the value to set.
   */
  public void setDistance(Integer value) {
    this.distance = value;
    setDirty(1);
  }

  /**
   * Checks the dirty status of the 'distance' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isDistanceDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'fetchCount' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getFetchCount() {
    return fetchCount;
  }

  /**
   * Sets the value of the 'fetchCount' field.
   *
   * @param value the value to set.
   */
  public void setFetchCount(Integer value) {
    this.fetchCount = value;
    setDirty(2);
  }

  /**
   * Checks the dirty status of the 'fetchCount' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isFetchCountDirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'fetchPriority' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getFetchPriority() {
    return fetchPriority;
  }

  /**
   * Sets the value of the 'fetchPriority' field.
   *
   * @param value the value to set.
   */
  public void setFetchPriority(Integer value) {
    this.fetchPriority = value;
    setDirty(3);
  }

  /**
   * Checks the dirty status of the 'fetchPriority' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isFetchPriorityDirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'fetchInterval' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getFetchInterval() {
    return fetchInterval;
  }

  /**
   * Sets the value of the 'fetchInterval' field.
   *
   * @param value the value to set.
   */
  public void setFetchInterval(Integer value) {
    this.fetchInterval = value;
    setDirty(4);
  }

  /**
   * Checks the dirty status of the 'fetchInterval' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isFetchIntervalDirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'zoneId' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getZoneId() {
    return zoneId;
  }

  /**
   * Sets the value of the 'zoneId' field.
   *
   * @param value the value to set.
   */
  public void setZoneId(CharSequence value) {
    this.zoneId = value;
    setDirty(5);
  }

  /**
   * Checks the dirty status of the 'zoneId' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isZoneIdDirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'options' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getOptions() {
    return options;
  }

  /**
   * Sets the value of the 'options' field.
   *
   * @param value the value to set.
   */
  public void setOptions(CharSequence value) {
    this.options = value;
    setDirty(6);
  }

  /**
   * Checks the dirty status of the 'options' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isOptionsDirty() {
    return isDirty(6);
  }

  /**
   * Gets the value of the 'batchId' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getBatchId() {
    return batchId;
  }

  /**
   * Sets the value of the 'batchId' field.
   *
   * @param value the value to set.
   */
  public void setBatchId(CharSequence value) {
    this.batchId = value;
    setDirty(7);
  }

  /**
   * Checks the dirty status of the 'batchId' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isBatchIdDirty() {
    return isDirty(7);
  }

  /**
   * Gets the value of the 'crawlStatus' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getCrawlStatus() {
    return crawlStatus;
  }

  /**
   * Sets the value of the 'crawlStatus' field.
   *
   * @param value the value to set.
   */
  public void setCrawlStatus(Integer value) {
    this.crawlStatus = value;
    setDirty(8);
  }

  /**
   * Checks the dirty status of the 'crawlStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isCrawlStatusDirty() {
    return isDirty(8);
  }

  /**
   * Gets the value of the 'prevFetchTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getPrevFetchTime() {
    return prevFetchTime;
  }

  /**
   * Sets the value of the 'prevFetchTime' field.
   *
   * @param value the value to set.
   */
  public void setPrevFetchTime(Long value) {
    this.prevFetchTime = value;
    setDirty(9);
  }

  /**
   * Checks the dirty status of the 'prevFetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPrevFetchTimeDirty() {
    return isDirty(9);
  }

  /**
   * Gets the value of the 'fetchTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getFetchTime() {
    return fetchTime;
  }

  /**
   * Sets the value of the 'fetchTime' field.
   *
   * @param value the value to set.
   */
  public void setFetchTime(Long value) {
    this.fetchTime = value;
    setDirty(10);
  }

  /**
   * Checks the dirty status of the 'fetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isFetchTimeDirty() {
    return isDirty(10);
  }

  /**
   * Gets the value of the 'fetchRetries' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getFetchRetries() {
    return fetchRetries;
  }

  /**
   * Sets the value of the 'fetchRetries' field.
   *
   * @param value the value to set.
   */
  public void setFetchRetries(Integer value) {
    this.fetchRetries = value;
    setDirty(11);
  }

  /**
   * Checks the dirty status of the 'fetchRetries' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isFetchRetriesDirty() {
    return isDirty(11);
  }

  /**
   * Gets the value of the 'reprUrl' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getReprUrl() {
    return reprUrl;
  }

  /**
   * Sets the value of the 'reprUrl' field.
   *
   * @param value the value to set.
   */
  public void setReprUrl(CharSequence value) {
    this.reprUrl = value;
    setDirty(12);
  }

  /**
   * Checks the dirty status of the 'reprUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isReprUrlDirty() {
    return isDirty(12);
  }

  /**
   * Gets the value of the 'prevModifiedTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getPrevModifiedTime() {
    return prevModifiedTime;
  }

  /**
   * Sets the value of the 'prevModifiedTime' field.
   *
   * @param value the value to set.
   */
  public void setPrevModifiedTime(Long value) {
    this.prevModifiedTime = value;
    setDirty(13);
  }

  /**
   * Checks the dirty status of the 'prevModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPrevModifiedTimeDirty() {
    return isDirty(13);
  }

  /**
   * Gets the value of the 'modifiedTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getModifiedTime() {
    return modifiedTime;
  }

  /**
   * Sets the value of the 'modifiedTime' field.
   *
   * @param value the value to set.
   */
  public void setModifiedTime(Long value) {
    this.modifiedTime = value;
    setDirty(14);
  }

  /**
   * Checks the dirty status of the 'modifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isModifiedTimeDirty() {
    return isDirty(14);
  }

  /**
   * Gets the value of the 'protocolStatus' field.
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GProtocolStatus} object.
   */
  public GProtocolStatus getProtocolStatus() {
    return protocolStatus;
  }

  /**
   * Sets the value of the 'protocolStatus' field.
   *
   * @param value the value to set.
   */
  public void setProtocolStatus(GProtocolStatus value) {
    this.protocolStatus = value;
    setDirty(15);
  }

  /**
   * Checks the dirty status of the 'protocolStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isProtocolStatusDirty() {
    return isDirty(15);
  }

  /**
   * Gets the value of the 'encoding' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getEncoding() {
    return encoding;
  }

  /**
   * Sets the value of the 'encoding' field.
   *
   * @param value the value to set.
   */
  public void setEncoding(CharSequence value) {
    this.encoding = value;
    setDirty(16);
  }

  /**
   * Checks the dirty status of the 'encoding' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isEncodingDirty() {
    return isDirty(16);
  }

  /**
   * Gets the value of the 'contentType' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getContentType() {
    return contentType;
  }

  /**
   * Sets the value of the 'contentType' field.
   *
   * @param value the value to set.
   */
  public void setContentType(CharSequence value) {
    this.contentType = value;
    setDirty(17);
  }

  /**
   * Checks the dirty status of the 'contentType' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentTypeDirty() {
    return isDirty(17);
  }

  /**
   * Gets the value of the 'content' field.
   * The entire raw document content e.g. raw XHTML
   *
   * @return a {@link java.nio.ByteBuffer} object.
   */
  public java.nio.ByteBuffer getContent() {
    return content;
  }

  /**
   * Sets the value of the 'content' field.
   * The entire raw document content e.g. raw XHTML   * @param value the value to set.
   *
   * @param value a {@link java.nio.ByteBuffer} object.
   */
  public void setContent(java.nio.ByteBuffer value) {
    this.content = value;
    setDirty(18);
  }

  /**
   * Checks the dirty status of the 'content' field. A field is dirty if it represents a change that has not yet been written to the database.
   * The entire raw document content e.g. raw XHTML   * @param value the value to set.
   *
   * @return a boolean.
   */
  public boolean isContentDirty() {
    return isDirty(18);
  }

  /**
   * Gets the value of the 'baseUrl' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getBaseUrl() {
    return baseUrl;
  }

  /**
   * Sets the value of the 'baseUrl' field.
   *
   * @param value the value to set.
   */
  public void setBaseUrl(CharSequence value) {
    this.baseUrl = value;
    setDirty(19);
  }

  /**
   * Checks the dirty status of the 'baseUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isBaseUrlDirty() {
    return isDirty(19);
  }

  /**
   * Gets the value of the 'referrer' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getReferrer() {
    return referrer;
  }

  /**
   * Sets the value of the 'referrer' field.
   *
   * @param value the value to set.
   */
  public void setReferrer(CharSequence value) {
    this.referrer = value;
    setDirty(20);
  }

  /**
   * Checks the dirty status of the 'referrer' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isReferrerDirty() {
    return isDirty(20);
  }

  /**
   * Gets the value of the 'anchor' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getAnchor() {
    return anchor;
  }

  /**
   * Sets the value of the 'anchor' field.
   *
   * @param value the value to set.
   */
  public void setAnchor(CharSequence value) {
    this.anchor = value;
    setDirty(21);
  }

  /**
   * Checks the dirty status of the 'anchor' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isAnchorDirty() {
    return isDirty(21);
  }

  /**
   * Gets the value of the 'anchorOrder' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getAnchorOrder() {
    return anchorOrder;
  }

  /**
   * Sets the value of the 'anchorOrder' field.
   *
   * @param value the value to set.
   */
  public void setAnchorOrder(Integer value) {
    this.anchorOrder = value;
    setDirty(22);
  }

  /**
   * Checks the dirty status of the 'anchorOrder' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isAnchorOrderDirty() {
    return isDirty(22);
  }

  /**
   * Gets the value of the 'parseStatus' field.
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GParseStatus} object.
   */
  public GParseStatus getParseStatus() {
    return parseStatus;
  }

  /**
   * Sets the value of the 'parseStatus' field.
   *
   * @param value the value to set.
   */
  public void setParseStatus(GParseStatus value) {
    this.parseStatus = value;
    setDirty(23);
  }

  /**
   * Checks the dirty status of the 'parseStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isParseStatusDirty() {
    return isDirty(23);
  }

  /**
   * Gets the value of the 'pageTitle' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getPageTitle() {
    return pageTitle;
  }

  /**
   * Sets the value of the 'pageTitle' field.
   *
   * @param value the value to set.
   */
  public void setPageTitle(CharSequence value) {
    this.pageTitle = value;
    setDirty(24);
  }

  /**
   * Checks the dirty status of the 'pageTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPageTitleDirty() {
    return isDirty(24);
  }

  /**
   * Gets the value of the 'pageText' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getPageText() {
    return pageText;
  }

  /**
   * Sets the value of the 'pageText' field.
   *
   * @param value the value to set.
   */
  public void setPageText(CharSequence value) {
    this.pageText = value;
    setDirty(25);
  }

  /**
   * Checks the dirty status of the 'pageText' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPageTextDirty() {
    return isDirty(25);
  }

  /**
   * Gets the value of the 'contentTitle' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getContentTitle() {
    return contentTitle;
  }

  /**
   * Sets the value of the 'contentTitle' field.
   *
   * @param value the value to set.
   */
  public void setContentTitle(CharSequence value) {
    this.contentTitle = value;
    setDirty(26);
  }

  /**
   * Checks the dirty status of the 'contentTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentTitleDirty() {
    return isDirty(26);
  }

  /**
   * Gets the value of the 'contentText' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getContentText() {
    return contentText;
  }

  /**
   * Sets the value of the 'contentText' field.
   *
   * @param value the value to set.
   */
  public void setContentText(CharSequence value) {
    this.contentText = value;
    setDirty(27);
  }

  /**
   * Checks the dirty status of the 'contentText' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentTextDirty() {
    return isDirty(27);
  }

  /**
   * Gets the value of the 'contentTextLen' field.
   *
   * @return a {@link java.lang.Integer} object.
   */
  public Integer getContentTextLen() {
    return contentTextLen;
  }

  /**
   * Sets the value of the 'contentTextLen' field.
   *
   * @param value the value to set.
   */
  public void setContentTextLen(Integer value) {
    this.contentTextLen = value;
    setDirty(28);
  }

  /**
   * Checks the dirty status of the 'contentTextLen' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentTextLenDirty() {
    return isDirty(28);
  }

  /**
   * Gets the value of the 'pageCategory' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getPageCategory() {
    return pageCategory;
  }

  /**
   * Sets the value of the 'pageCategory' field.
   *
   * @param value the value to set.
   */
  public void setPageCategory(CharSequence value) {
    this.pageCategory = value;
    setDirty(29);
  }

  /**
   * Checks the dirty status of the 'pageCategory' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPageCategoryDirty() {
    return isDirty(29);
  }

  /**
   * Gets the value of the 'contentModifiedTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getContentModifiedTime() {
    return contentModifiedTime;
  }

  /**
   * Sets the value of the 'contentModifiedTime' field.
   *
   * @param value the value to set.
   */
  public void setContentModifiedTime(Long value) {
    this.contentModifiedTime = value;
    setDirty(30);
  }

  /**
   * Checks the dirty status of the 'contentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentModifiedTimeDirty() {
    return isDirty(30);
  }

  /**
   * Gets the value of the 'prevContentModifiedTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getPrevContentModifiedTime() {
    return prevContentModifiedTime;
  }

  /**
   * Sets the value of the 'prevContentModifiedTime' field.
   *
   * @param value the value to set.
   */
  public void setPrevContentModifiedTime(Long value) {
    this.prevContentModifiedTime = value;
    setDirty(31);
  }

  /**
   * Checks the dirty status of the 'prevContentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPrevContentModifiedTimeDirty() {
    return isDirty(31);
  }

  /**
   * Gets the value of the 'contentPublishTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getContentPublishTime() {
    return contentPublishTime;
  }

  /**
   * Sets the value of the 'contentPublishTime' field.
   *
   * @param value the value to set.
   */
  public void setContentPublishTime(Long value) {
    this.contentPublishTime = value;
    setDirty(32);
  }

  /**
   * Checks the dirty status of the 'contentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentPublishTimeDirty() {
    return isDirty(32);
  }

  /**
   * Gets the value of the 'prevContentPublishTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getPrevContentPublishTime() {
    return prevContentPublishTime;
  }

  /**
   * Sets the value of the 'prevContentPublishTime' field.
   *
   * @param value the value to set.
   */
  public void setPrevContentPublishTime(Long value) {
    this.prevContentPublishTime = value;
    setDirty(33);
  }

  /**
   * Checks the dirty status of the 'prevContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPrevContentPublishTimeDirty() {
    return isDirty(33);
  }

  /**
   * Gets the value of the 'refContentPublishTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getRefContentPublishTime() {
    return refContentPublishTime;
  }

  /**
   * Sets the value of the 'refContentPublishTime' field.
   *
   * @param value the value to set.
   */
  public void setRefContentPublishTime(Long value) {
    this.refContentPublishTime = value;
    setDirty(34);
  }

  /**
   * Checks the dirty status of the 'refContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isRefContentPublishTimeDirty() {
    return isDirty(34);
  }

  /**
   * Gets the value of the 'prevRefContentPublishTime' field.
   *
   * @return a {@link java.lang.Long} object.
   */
  public Long getPrevRefContentPublishTime() {
    return prevRefContentPublishTime;
  }

  /**
   * Sets the value of the 'prevRefContentPublishTime' field.
   *
   * @param value the value to set.
   */
  public void setPrevRefContentPublishTime(Long value) {
    this.prevRefContentPublishTime = value;
    setDirty(35);
  }

  /**
   * Checks the dirty status of the 'prevRefContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPrevRefContentPublishTimeDirty() {
    return isDirty(35);
  }

  /**
   * Gets the value of the 'prevSignature' field.
   *
   * @return a {@link java.nio.ByteBuffer} object.
   */
  public java.nio.ByteBuffer getPrevSignature() {
    return prevSignature;
  }

  /**
   * Sets the value of the 'prevSignature' field.
   *
   * @param value the value to set.
   */
  public void setPrevSignature(java.nio.ByteBuffer value) {
    this.prevSignature = value;
    setDirty(36);
  }

  /**
   * Checks the dirty status of the 'prevSignature' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPrevSignatureDirty() {
    return isDirty(36);
  }

  /**
   * Gets the value of the 'signature' field.
   *
   * @return a {@link java.nio.ByteBuffer} object.
   */
  public java.nio.ByteBuffer getSignature() {
    return signature;
  }

  /**
   * Sets the value of the 'signature' field.
   *
   * @param value the value to set.
   */
  public void setSignature(java.nio.ByteBuffer value) {
    this.signature = value;
    setDirty(37);
  }

  /**
   * Checks the dirty status of the 'signature' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isSignatureDirty() {
    return isDirty(37);
  }

  /**
   * Gets the value of the 'contentScore' field.
   *
   * @return a {@link java.lang.Float} object.
   */
  public Float getContentScore() {
    return contentScore;
  }

  /**
   * Sets the value of the 'contentScore' field.
   *
   * @param value the value to set.
   */
  public void setContentScore(Float value) {
    this.contentScore = value;
    setDirty(38);
  }

  /**
   * Checks the dirty status of the 'contentScore' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isContentScoreDirty() {
    return isDirty(38);
  }

  /**
   * Gets the value of the 'score' field.
   *
   * @return a {@link java.lang.Float} object.
   */
  public Float getScore() {
    return score;
  }

  /**
   * Sets the value of the 'score' field.
   *
   * @param value the value to set.
   */
  public void setScore(Float value) {
    this.score = value;
    setDirty(39);
  }

  /**
   * Checks the dirty status of the 'score' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isScoreDirty() {
    return isDirty(39);
  }

  /**
   * Gets the value of the 'sortScore' field.
   *
   * @return a {@link java.lang.CharSequence} object.
   */
  public CharSequence getSortScore() {
    return sortScore;
  }

  /**
   * Sets the value of the 'sortScore' field.
   *
   * @param value the value to set.
   */
  public void setSortScore(CharSequence value) {
    this.sortScore = value;
    setDirty(40);
  }

  /**
   * Checks the dirty status of the 'sortScore' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isSortScoreDirty() {
    return isDirty(40);
  }

  /**
   * Gets the value of the 'pageCounters' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence, Integer> getPageCounters() {
    return pageCounters;
  }

  /**
   * Sets the value of the 'pageCounters' field.
   *
   * @param value the value to set.
   */
  public void setPageCounters(java.util.Map<CharSequence, Integer> value) {
    this.pageCounters = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(41);
  }

  /**
   * Checks the dirty status of the 'pageCounters' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPageCountersDirty() {
    return isDirty(41);
  }

  /**
   * Gets the value of the 'headers' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence, CharSequence> getHeaders() {
    return headers;
  }

  /**
   * Sets the value of the 'headers' field.
   *
   * @param value the value to set.
   */
  public void setHeaders(java.util.Map<CharSequence, CharSequence> value) {
    this.headers = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(42);
  }

  /**
   * Checks the dirty status of the 'headers' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isHeadersDirty() {
    return isDirty(42);
  }

  /**
   * Gets the value of the 'links' field.
   *
   * @return a {@link java.util.List} object.
   */
  public java.util.List<CharSequence> getLinks() {
    return links;
  }

  /**
   * Sets the value of the 'links' field.
   *
   * @param value the value to set.
   */
  public void setLinks(java.util.List<CharSequence> value) {
    this.links = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(43);
  }

  /**
   * Checks the dirty status of the 'links' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isLinksDirty() {
    return isDirty(43);
  }

  /**
   * Gets the value of the 'liveLinks' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence, GHypeLink> getLiveLinks() {
    return liveLinks;
  }

  /**
   * Sets the value of the 'liveLinks' field.
   *
   * @param value the value to set.
   */
  public void setLiveLinks(java.util.Map<CharSequence, GHypeLink> value) {
    this.liveLinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(44);
  }

  /**
   * Checks the dirty status of the 'liveLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isLiveLinksDirty() {
    return isDirty(44);
  }

  /**
   * Gets the value of the 'vividLinks' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence, CharSequence> getVividLinks() {
    return vividLinks;
  }

  /**
   * Sets the value of the 'vividLinks' field.
   *
   * @param value the value to set.
   */
  public void setVividLinks(java.util.Map<CharSequence, CharSequence> value) {
    this.vividLinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(45);
  }

  /**
   * Checks the dirty status of the 'vividLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isVividLinksDirty() {
    return isDirty(45);
  }

  /**
   * Gets the value of the 'deadLinks' field.
   *
   * @return a {@link java.util.List} object.
   */
  public java.util.List<CharSequence> getDeadLinks() {
    return deadLinks;
  }

  /**
   * Sets the value of the 'deadLinks' field.
   *
   * @param value the value to set.
   */
  public void setDeadLinks(java.util.List<CharSequence> value) {
    this.deadLinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(46);
  }

  /**
   * Checks the dirty status of the 'deadLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isDeadLinksDirty() {
    return isDirty(46);
  }

  /**
   * Gets the value of the 'inlinks' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence, CharSequence> getInlinks() {
    return inlinks;
  }

  /**
   * Sets the value of the 'inlinks' field.
   *
   * @param value the value to set.
   */
  public void setInlinks(java.util.Map<CharSequence, CharSequence> value) {
    this.inlinks = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(47);
  }

  /**
   * Checks the dirty status of the 'inlinks' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isInlinksDirty() {
    return isDirty(47);
  }

  /**
   * Gets the value of the 'markers' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence, CharSequence> getMarkers() {
    return markers;
  }

  /**
   * Sets the value of the 'markers' field.
   *
   * @param value the value to set.
   */
  public void setMarkers(java.util.Map<CharSequence, CharSequence> value) {
    this.markers = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(48);
  }

  /**
   * Checks the dirty status of the 'markers' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isMarkersDirty() {
    return isDirty(48);
  }

  /**
   * Gets the value of the 'metadata' field.
   *
   * @return a {@link java.util.Map} object.
   */
  public java.util.Map<CharSequence,java.nio.ByteBuffer> getMetadata() {
    return metadata;
  }

  /**
   * Sets the value of the 'metadata' field.
   *
   * @param value the value to set.
   */
  public void setMetadata(java.util.Map<CharSequence,java.nio.ByteBuffer> value) {
    this.metadata = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(49);
  }

  /**
   * Checks the dirty status of the 'metadata' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isMetadataDirty() {
    return isDirty(49);
  }

  /**
   * Gets the value of the 'pageModel' field.
   *
   * @return a {@link java.util.List} object.
   */
  public java.util.List<GFieldGroup> getPageModel() {
    return pageModel;
  }

  /**
   * Sets the value of the 'pageModel' field.
   *
   * @param value the value to set.
   */
  public void setPageModel(java.util.List<GFieldGroup> value) {
    this.pageModel = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(50);
  }

  /**
   * Checks the dirty status of the 'pageModel' field. A field is dirty if it represents a change that has not yet been written to the database.
   *
   * @return a boolean.
   */
  public boolean isPageModelDirty() {
    return isDirty(50);
  }

  /**
   * Creates a new GWebPage RecordBuilder
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GWebPage.Builder} object.
   */
  public static GWebPage.Builder newBuilder() {
    return new GWebPage.Builder();
  }

  /**
   * Creates a new GWebPage RecordBuilder by copying an existing Builder
   *
   * @param other a {@link ai.platon.pulsar.persist.gora.generated.GWebPage.Builder} object.
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GWebPage.Builder} object.
   */
  public static GWebPage.Builder newBuilder(GWebPage.Builder other) {
    return new GWebPage.Builder(other);
  }

  /**
   * Creates a new GWebPage RecordBuilder by copying an existing GWebPage instance
   *
   * @param other a {@link ai.platon.pulsar.persist.gora.generated.GWebPage} object.
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GWebPage.Builder} object.
   */
  public static GWebPage.Builder newBuilder(GWebPage other) {
    return new GWebPage.Builder(other);
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

    private long createTime;
    private int distance;
    private int fetchCount;
    private int fetchPriority;
    private int fetchInterval;
    private CharSequence zoneId;
    private CharSequence options;
    private CharSequence batchId;
    private int crawlStatus;
    private long prevFetchTime;
    private long fetchTime;
    private int fetchRetries;
    private CharSequence reprUrl;
    private long prevModifiedTime;
    private long modifiedTime;
    private GProtocolStatus protocolStatus;
    private CharSequence encoding;
    private CharSequence contentType;
    private java.nio.ByteBuffer content;
    private CharSequence baseUrl;
    private CharSequence referrer;
    private CharSequence anchor;
    private int anchorOrder;
    private GParseStatus parseStatus;
    private CharSequence pageTitle;
    private CharSequence pageText;
    private CharSequence contentTitle;
    private CharSequence contentText;
    private int contentTextLen;
    private CharSequence pageCategory;
    private long contentModifiedTime;
    private long prevContentModifiedTime;
    private long contentPublishTime;
    private long prevContentPublishTime;
    private long refContentPublishTime;
    private long prevRefContentPublishTime;
    private java.nio.ByteBuffer prevSignature;
    private java.nio.ByteBuffer signature;
    private float contentScore;
    private float score;
    private CharSequence sortScore;
    private java.util.Map<CharSequence, Integer> pageCounters;
    private java.util.Map<CharSequence, CharSequence> headers;
    private java.util.List<CharSequence> links;
    private java.util.Map<CharSequence, GHypeLink> liveLinks;
    private java.util.Map<CharSequence, CharSequence> vividLinks;
    private java.util.List<CharSequence> deadLinks;
    private java.util.Map<CharSequence, CharSequence> inlinks;
    private java.util.Map<CharSequence, CharSequence> markers;
    private java.util.Map<CharSequence,java.nio.ByteBuffer> metadata;
    private java.util.List<GFieldGroup> pageModel;

    /** Creates a new Builder */
    private Builder() {
      super(GWebPage.SCHEMA$);
    }

    /** Creates a Builder by copying an existing Builder */
    private Builder(GWebPage.Builder other) {
      super(other);
    }

    /** Creates a Builder by copying an existing GWebPage instance */
    private Builder(GWebPage other) {
            super(GWebPage.SCHEMA$);
      if (isValidValue(fields()[0], other.createTime)) {
        this.createTime = (Long) data().deepCopy(fields()[0].schema(), other.createTime);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.distance)) {
        this.distance = (Integer) data().deepCopy(fields()[1].schema(), other.distance);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.fetchCount)) {
        this.fetchCount = (Integer) data().deepCopy(fields()[2].schema(), other.fetchCount);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.fetchPriority)) {
        this.fetchPriority = (Integer) data().deepCopy(fields()[3].schema(), other.fetchPriority);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.fetchInterval)) {
        this.fetchInterval = (Integer) data().deepCopy(fields()[4].schema(), other.fetchInterval);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.zoneId)) {
        this.zoneId = (CharSequence) data().deepCopy(fields()[5].schema(), other.zoneId);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.options)) {
        this.options = (CharSequence) data().deepCopy(fields()[6].schema(), other.options);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.batchId)) {
        this.batchId = (CharSequence) data().deepCopy(fields()[7].schema(), other.batchId);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.crawlStatus)) {
        this.crawlStatus = (Integer) data().deepCopy(fields()[8].schema(), other.crawlStatus);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.prevFetchTime)) {
        this.prevFetchTime = (Long) data().deepCopy(fields()[9].schema(), other.prevFetchTime);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.fetchTime)) {
        this.fetchTime = (Long) data().deepCopy(fields()[10].schema(), other.fetchTime);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.fetchRetries)) {
        this.fetchRetries = (Integer) data().deepCopy(fields()[11].schema(), other.fetchRetries);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.reprUrl)) {
        this.reprUrl = (CharSequence) data().deepCopy(fields()[12].schema(), other.reprUrl);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.prevModifiedTime)) {
        this.prevModifiedTime = (Long) data().deepCopy(fields()[13].schema(), other.prevModifiedTime);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.modifiedTime)) {
        this.modifiedTime = (Long) data().deepCopy(fields()[14].schema(), other.modifiedTime);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.protocolStatus)) {
        this.protocolStatus = (GProtocolStatus) data().deepCopy(fields()[15].schema(), other.protocolStatus);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.encoding)) {
        this.encoding = (CharSequence) data().deepCopy(fields()[16].schema(), other.encoding);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.contentType)) {
        this.contentType = (CharSequence) data().deepCopy(fields()[17].schema(), other.contentType);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.content)) {
        this.content = (java.nio.ByteBuffer) data().deepCopy(fields()[18].schema(), other.content);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.baseUrl)) {
        this.baseUrl = (CharSequence) data().deepCopy(fields()[19].schema(), other.baseUrl);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.referrer)) {
        this.referrer = (CharSequence) data().deepCopy(fields()[20].schema(), other.referrer);
        fieldSetFlags()[20] = true;
      }
      if (isValidValue(fields()[21], other.anchor)) {
        this.anchor = (CharSequence) data().deepCopy(fields()[21].schema(), other.anchor);
        fieldSetFlags()[21] = true;
      }
      if (isValidValue(fields()[22], other.anchorOrder)) {
        this.anchorOrder = (Integer) data().deepCopy(fields()[22].schema(), other.anchorOrder);
        fieldSetFlags()[22] = true;
      }
      if (isValidValue(fields()[23], other.parseStatus)) {
        this.parseStatus = (GParseStatus) data().deepCopy(fields()[23].schema(), other.parseStatus);
        fieldSetFlags()[23] = true;
      }
      if (isValidValue(fields()[24], other.pageTitle)) {
        this.pageTitle = (CharSequence) data().deepCopy(fields()[24].schema(), other.pageTitle);
        fieldSetFlags()[24] = true;
      }
      if (isValidValue(fields()[25], other.pageText)) {
        this.pageText = (CharSequence) data().deepCopy(fields()[25].schema(), other.pageText);
        fieldSetFlags()[25] = true;
      }
      if (isValidValue(fields()[26], other.contentTitle)) {
        this.contentTitle = (CharSequence) data().deepCopy(fields()[26].schema(), other.contentTitle);
        fieldSetFlags()[26] = true;
      }
      if (isValidValue(fields()[27], other.contentText)) {
        this.contentText = (CharSequence) data().deepCopy(fields()[27].schema(), other.contentText);
        fieldSetFlags()[27] = true;
      }
      if (isValidValue(fields()[28], other.contentTextLen)) {
        this.contentTextLen = (Integer) data().deepCopy(fields()[28].schema(), other.contentTextLen);
        fieldSetFlags()[28] = true;
      }
      if (isValidValue(fields()[29], other.pageCategory)) {
        this.pageCategory = (CharSequence) data().deepCopy(fields()[29].schema(), other.pageCategory);
        fieldSetFlags()[29] = true;
      }
      if (isValidValue(fields()[30], other.contentModifiedTime)) {
        this.contentModifiedTime = (Long) data().deepCopy(fields()[30].schema(), other.contentModifiedTime);
        fieldSetFlags()[30] = true;
      }
      if (isValidValue(fields()[31], other.prevContentModifiedTime)) {
        this.prevContentModifiedTime = (Long) data().deepCopy(fields()[31].schema(), other.prevContentModifiedTime);
        fieldSetFlags()[31] = true;
      }
      if (isValidValue(fields()[32], other.contentPublishTime)) {
        this.contentPublishTime = (Long) data().deepCopy(fields()[32].schema(), other.contentPublishTime);
        fieldSetFlags()[32] = true;
      }
      if (isValidValue(fields()[33], other.prevContentPublishTime)) {
        this.prevContentPublishTime = (Long) data().deepCopy(fields()[33].schema(), other.prevContentPublishTime);
        fieldSetFlags()[33] = true;
      }
      if (isValidValue(fields()[34], other.refContentPublishTime)) {
        this.refContentPublishTime = (Long) data().deepCopy(fields()[34].schema(), other.refContentPublishTime);
        fieldSetFlags()[34] = true;
      }
      if (isValidValue(fields()[35], other.prevRefContentPublishTime)) {
        this.prevRefContentPublishTime = (Long) data().deepCopy(fields()[35].schema(), other.prevRefContentPublishTime);
        fieldSetFlags()[35] = true;
      }
      if (isValidValue(fields()[36], other.prevSignature)) {
        this.prevSignature = (java.nio.ByteBuffer) data().deepCopy(fields()[36].schema(), other.prevSignature);
        fieldSetFlags()[36] = true;
      }
      if (isValidValue(fields()[37], other.signature)) {
        this.signature = (java.nio.ByteBuffer) data().deepCopy(fields()[37].schema(), other.signature);
        fieldSetFlags()[37] = true;
      }
      if (isValidValue(fields()[38], other.contentScore)) {
        this.contentScore = (Float) data().deepCopy(fields()[38].schema(), other.contentScore);
        fieldSetFlags()[38] = true;
      }
      if (isValidValue(fields()[39], other.score)) {
        this.score = (Float) data().deepCopy(fields()[39].schema(), other.score);
        fieldSetFlags()[39] = true;
      }
      if (isValidValue(fields()[40], other.sortScore)) {
        this.sortScore = (CharSequence) data().deepCopy(fields()[40].schema(), other.sortScore);
        fieldSetFlags()[40] = true;
      }
      if (isValidValue(fields()[41], other.pageCounters)) {
        this.pageCounters = (java.util.Map<CharSequence, Integer>) data().deepCopy(fields()[41].schema(), other.pageCounters);
        fieldSetFlags()[41] = true;
      }
      if (isValidValue(fields()[42], other.headers)) {
        this.headers = (java.util.Map<CharSequence, CharSequence>) data().deepCopy(fields()[42].schema(), other.headers);
        fieldSetFlags()[42] = true;
      }
      if (isValidValue(fields()[43], other.links)) {
        this.links = (java.util.List<CharSequence>) data().deepCopy(fields()[43].schema(), other.links);
        fieldSetFlags()[43] = true;
      }
      if (isValidValue(fields()[44], other.liveLinks)) {
        this.liveLinks = (java.util.Map<CharSequence, GHypeLink>) data().deepCopy(fields()[44].schema(), other.liveLinks);
        fieldSetFlags()[44] = true;
      }
      if (isValidValue(fields()[45], other.vividLinks)) {
        this.vividLinks = (java.util.Map<CharSequence, CharSequence>) data().deepCopy(fields()[45].schema(), other.vividLinks);
        fieldSetFlags()[45] = true;
      }
      if (isValidValue(fields()[46], other.deadLinks)) {
        this.deadLinks = (java.util.List<CharSequence>) data().deepCopy(fields()[46].schema(), other.deadLinks);
        fieldSetFlags()[46] = true;
      }
      if (isValidValue(fields()[47], other.inlinks)) {
        this.inlinks = (java.util.Map<CharSequence, CharSequence>) data().deepCopy(fields()[47].schema(), other.inlinks);
        fieldSetFlags()[47] = true;
      }
      if (isValidValue(fields()[48], other.markers)) {
        this.markers = (java.util.Map<CharSequence, CharSequence>) data().deepCopy(fields()[48].schema(), other.markers);
        fieldSetFlags()[48] = true;
      }
      if (isValidValue(fields()[49], other.metadata)) {
        this.metadata = (java.util.Map<CharSequence,java.nio.ByteBuffer>) data().deepCopy(fields()[49].schema(), other.metadata);
        fieldSetFlags()[49] = true;
      }
      if (isValidValue(fields()[50], other.pageModel)) {
        this.pageModel = (java.util.List<GFieldGroup>) data().deepCopy(fields()[50].schema(), other.pageModel);
        fieldSetFlags()[50] = true;
      }
    }

    /** Gets the value of the 'createTime' field */
    public Long getCreateTime() {
      return createTime;
    }

    /** Sets the value of the 'createTime' field */
    public GWebPage.Builder setCreateTime(long value) {
      validate(fields()[0], value);
      this.createTime = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /** Checks whether the 'createTime' field has been set */
    public boolean hasCreateTime() {
      return fieldSetFlags()[0];
    }

    /** Clears the value of the 'createTime' field */
    public GWebPage.Builder clearCreateTime() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'distance' field */
    public Integer getDistance() {
      return distance;
    }

    /** Sets the value of the 'distance' field */
    public GWebPage.Builder setDistance(int value) {
      validate(fields()[1], value);
      this.distance = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /** Checks whether the 'distance' field has been set */
    public boolean hasDistance() {
      return fieldSetFlags()[1];
    }

    /** Clears the value of the 'distance' field */
    public GWebPage.Builder clearDistance() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'fetchCount' field */
    public Integer getFetchCount() {
      return fetchCount;
    }

    /** Sets the value of the 'fetchCount' field */
    public GWebPage.Builder setFetchCount(int value) {
      validate(fields()[2], value);
      this.fetchCount = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /** Checks whether the 'fetchCount' field has been set */
    public boolean hasFetchCount() {
      return fieldSetFlags()[2];
    }

    /** Clears the value of the 'fetchCount' field */
    public GWebPage.Builder clearFetchCount() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'fetchPriority' field */
    public Integer getFetchPriority() {
      return fetchPriority;
    }

    /** Sets the value of the 'fetchPriority' field */
    public GWebPage.Builder setFetchPriority(int value) {
      validate(fields()[3], value);
      this.fetchPriority = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /** Checks whether the 'fetchPriority' field has been set */
    public boolean hasFetchPriority() {
      return fieldSetFlags()[3];
    }

    /** Clears the value of the 'fetchPriority' field */
    public GWebPage.Builder clearFetchPriority() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'fetchInterval' field */
    public Integer getFetchInterval() {
      return fetchInterval;
    }

    /** Sets the value of the 'fetchInterval' field */
    public GWebPage.Builder setFetchInterval(int value) {
      validate(fields()[4], value);
      this.fetchInterval = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /** Checks whether the 'fetchInterval' field has been set */
    public boolean hasFetchInterval() {
      return fieldSetFlags()[4];
    }

    /** Clears the value of the 'fetchInterval' field */
    public GWebPage.Builder clearFetchInterval() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'zoneId' field */
    public CharSequence getZoneId() {
      return zoneId;
    }

    /** Sets the value of the 'zoneId' field */
    public GWebPage.Builder setZoneId(CharSequence value) {
      validate(fields()[5], value);
      this.zoneId = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /** Checks whether the 'zoneId' field has been set */
    public boolean hasZoneId() {
      return fieldSetFlags()[5];
    }

    /** Clears the value of the 'zoneId' field */
    public GWebPage.Builder clearZoneId() {
      zoneId = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'options' field */
    public CharSequence getOptions() {
      return options;
    }

    /** Sets the value of the 'options' field */
    public GWebPage.Builder setOptions(CharSequence value) {
      validate(fields()[6], value);
      this.options = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /** Checks whether the 'options' field has been set */
    public boolean hasOptions() {
      return fieldSetFlags()[6];
    }

    /** Clears the value of the 'options' field */
    public GWebPage.Builder clearOptions() {
      options = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'batchId' field */
    public CharSequence getBatchId() {
      return batchId;
    }

    /** Sets the value of the 'batchId' field */
    public GWebPage.Builder setBatchId(CharSequence value) {
      validate(fields()[7], value);
      this.batchId = value;
      fieldSetFlags()[7] = true;
      return this;
    }

    /** Checks whether the 'batchId' field has been set */
    public boolean hasBatchId() {
      return fieldSetFlags()[7];
    }

    /** Clears the value of the 'batchId' field */
    public GWebPage.Builder clearBatchId() {
      batchId = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'crawlStatus' field */
    public Integer getCrawlStatus() {
      return crawlStatus;
    }

    /** Sets the value of the 'crawlStatus' field */
    public GWebPage.Builder setCrawlStatus(int value) {
      validate(fields()[8], value);
      this.crawlStatus = value;
      fieldSetFlags()[8] = true;
      return this;
    }

    /** Checks whether the 'crawlStatus' field has been set */
    public boolean hasCrawlStatus() {
      return fieldSetFlags()[8];
    }

    /** Clears the value of the 'crawlStatus' field */
    public GWebPage.Builder clearCrawlStatus() {
      fieldSetFlags()[8] = false;
      return this;
    }

    /** Gets the value of the 'prevFetchTime' field */
    public Long getPrevFetchTime() {
      return prevFetchTime;
    }

    /** Sets the value of the 'prevFetchTime' field */
    public GWebPage.Builder setPrevFetchTime(long value) {
      validate(fields()[9], value);
      this.prevFetchTime = value;
      fieldSetFlags()[9] = true;
      return this;
    }

    /** Checks whether the 'prevFetchTime' field has been set */
    public boolean hasPrevFetchTime() {
      return fieldSetFlags()[9];
    }

    /** Clears the value of the 'prevFetchTime' field */
    public GWebPage.Builder clearPrevFetchTime() {
      fieldSetFlags()[9] = false;
      return this;
    }

    /** Gets the value of the 'fetchTime' field */
    public Long getFetchTime() {
      return fetchTime;
    }

    /** Sets the value of the 'fetchTime' field */
    public GWebPage.Builder setFetchTime(long value) {
      validate(fields()[10], value);
      this.fetchTime = value;
      fieldSetFlags()[10] = true;
      return this;
    }

    /** Checks whether the 'fetchTime' field has been set */
    public boolean hasFetchTime() {
      return fieldSetFlags()[10];
    }

    /** Clears the value of the 'fetchTime' field */
    public GWebPage.Builder clearFetchTime() {
      fieldSetFlags()[10] = false;
      return this;
    }

    /** Gets the value of the 'fetchRetries' field */
    public Integer getFetchRetries() {
      return fetchRetries;
    }

    /** Sets the value of the 'fetchRetries' field */
    public GWebPage.Builder setFetchRetries(int value) {
      validate(fields()[11], value);
      this.fetchRetries = value;
      fieldSetFlags()[11] = true;
      return this;
    }

    /** Checks whether the 'fetchRetries' field has been set */
    public boolean hasFetchRetries() {
      return fieldSetFlags()[11];
    }

    /** Clears the value of the 'fetchRetries' field */
    public GWebPage.Builder clearFetchRetries() {
      fieldSetFlags()[11] = false;
      return this;
    }

    /** Gets the value of the 'reprUrl' field */
    public CharSequence getReprUrl() {
      return reprUrl;
    }

    /** Sets the value of the 'reprUrl' field */
    public GWebPage.Builder setReprUrl(CharSequence value) {
      validate(fields()[12], value);
      this.reprUrl = value;
      fieldSetFlags()[12] = true;
      return this;
    }

    /** Checks whether the 'reprUrl' field has been set */
    public boolean hasReprUrl() {
      return fieldSetFlags()[12];
    }

    /** Clears the value of the 'reprUrl' field */
    public GWebPage.Builder clearReprUrl() {
      reprUrl = null;
      fieldSetFlags()[12] = false;
      return this;
    }

    /** Gets the value of the 'prevModifiedTime' field */
    public Long getPrevModifiedTime() {
      return prevModifiedTime;
    }

    /** Sets the value of the 'prevModifiedTime' field */
    public GWebPage.Builder setPrevModifiedTime(long value) {
      validate(fields()[13], value);
      this.prevModifiedTime = value;
      fieldSetFlags()[13] = true;
      return this;
    }

    /** Checks whether the 'prevModifiedTime' field has been set */
    public boolean hasPrevModifiedTime() {
      return fieldSetFlags()[13];
    }

    /** Clears the value of the 'prevModifiedTime' field */
    public GWebPage.Builder clearPrevModifiedTime() {
      fieldSetFlags()[13] = false;
      return this;
    }

    /** Gets the value of the 'modifiedTime' field */
    public Long getModifiedTime() {
      return modifiedTime;
    }

    /** Sets the value of the 'modifiedTime' field */
    public GWebPage.Builder setModifiedTime(long value) {
      validate(fields()[14], value);
      this.modifiedTime = value;
      fieldSetFlags()[14] = true;
      return this;
    }

    /** Checks whether the 'modifiedTime' field has been set */
    public boolean hasModifiedTime() {
      return fieldSetFlags()[14];
    }

    /** Clears the value of the 'modifiedTime' field */
    public GWebPage.Builder clearModifiedTime() {
      fieldSetFlags()[14] = false;
      return this;
    }

    /** Gets the value of the 'protocolStatus' field */
    public GProtocolStatus getProtocolStatus() {
      return protocolStatus;
    }

    /** Sets the value of the 'protocolStatus' field */
    public GWebPage.Builder setProtocolStatus(GProtocolStatus value) {
      validate(fields()[15], value);
      this.protocolStatus = value;
      fieldSetFlags()[15] = true;
      return this;
    }

    /** Checks whether the 'protocolStatus' field has been set */
    public boolean hasProtocolStatus() {
      return fieldSetFlags()[15];
    }

    /** Clears the value of the 'protocolStatus' field */
    public GWebPage.Builder clearProtocolStatus() {
      protocolStatus = null;
      fieldSetFlags()[15] = false;
      return this;
    }

    /** Gets the value of the 'encoding' field */
    public CharSequence getEncoding() {
      return encoding;
    }

    /** Sets the value of the 'encoding' field */
    public GWebPage.Builder setEncoding(CharSequence value) {
      validate(fields()[16], value);
      this.encoding = value;
      fieldSetFlags()[16] = true;
      return this;
    }

    /** Checks whether the 'encoding' field has been set */
    public boolean hasEncoding() {
      return fieldSetFlags()[16];
    }

    /** Clears the value of the 'encoding' field */
    public GWebPage.Builder clearEncoding() {
      encoding = null;
      fieldSetFlags()[16] = false;
      return this;
    }

    /** Gets the value of the 'contentType' field */
    public CharSequence getContentType() {
      return contentType;
    }

    /** Sets the value of the 'contentType' field */
    public GWebPage.Builder setContentType(CharSequence value) {
      validate(fields()[17], value);
      this.contentType = value;
      fieldSetFlags()[17] = true;
      return this;
    }

    /** Checks whether the 'contentType' field has been set */
    public boolean hasContentType() {
      return fieldSetFlags()[17];
    }

    /** Clears the value of the 'contentType' field */
    public GWebPage.Builder clearContentType() {
      contentType = null;
      fieldSetFlags()[17] = false;
      return this;
    }

    /** Gets the value of the 'content' field */
    public java.nio.ByteBuffer getContent() {
      return content;
    }

    /** Sets the value of the 'content' field */
    public GWebPage.Builder setContent(java.nio.ByteBuffer value) {
      validate(fields()[18], value);
      this.content = value;
      fieldSetFlags()[18] = true;
      return this;
    }

    /** Checks whether the 'content' field has been set */
    public boolean hasContent() {
      return fieldSetFlags()[18];
    }

    /** Clears the value of the 'content' field */
    public GWebPage.Builder clearContent() {
      content = null;
      fieldSetFlags()[18] = false;
      return this;
    }

    /** Gets the value of the 'baseUrl' field */
    public CharSequence getBaseUrl() {
      return baseUrl;
    }

    /** Sets the value of the 'baseUrl' field */
    public GWebPage.Builder setBaseUrl(CharSequence value) {
      validate(fields()[19], value);
      this.baseUrl = value;
      fieldSetFlags()[19] = true;
      return this;
    }

    /** Checks whether the 'baseUrl' field has been set */
    public boolean hasBaseUrl() {
      return fieldSetFlags()[19];
    }

    /** Clears the value of the 'baseUrl' field */
    public GWebPage.Builder clearBaseUrl() {
      baseUrl = null;
      fieldSetFlags()[19] = false;
      return this;
    }

    /** Gets the value of the 'referrer' field */
    public CharSequence getReferrer() {
      return referrer;
    }

    /** Sets the value of the 'referrer' field */
    public GWebPage.Builder setReferrer(CharSequence value) {
      validate(fields()[20], value);
      this.referrer = value;
      fieldSetFlags()[20] = true;
      return this;
    }

    /** Checks whether the 'referrer' field has been set */
    public boolean hasReferrer() {
      return fieldSetFlags()[20];
    }

    /** Clears the value of the 'referrer' field */
    public GWebPage.Builder clearReferrer() {
      referrer = null;
      fieldSetFlags()[20] = false;
      return this;
    }

    /** Gets the value of the 'anchor' field */
    public CharSequence getAnchor() {
      return anchor;
    }

    /** Sets the value of the 'anchor' field */
    public GWebPage.Builder setAnchor(CharSequence value) {
      validate(fields()[21], value);
      this.anchor = value;
      fieldSetFlags()[21] = true;
      return this;
    }

    /** Checks whether the 'anchor' field has been set */
    public boolean hasAnchor() {
      return fieldSetFlags()[21];
    }

    /** Clears the value of the 'anchor' field */
    public GWebPage.Builder clearAnchor() {
      anchor = null;
      fieldSetFlags()[21] = false;
      return this;
    }

    /** Gets the value of the 'anchorOrder' field */
    public Integer getAnchorOrder() {
      return anchorOrder;
    }

    /** Sets the value of the 'anchorOrder' field */
    public GWebPage.Builder setAnchorOrder(int value) {
      validate(fields()[22], value);
      this.anchorOrder = value;
      fieldSetFlags()[22] = true;
      return this;
    }

    /** Checks whether the 'anchorOrder' field has been set */
    public boolean hasAnchorOrder() {
      return fieldSetFlags()[22];
    }

    /** Clears the value of the 'anchorOrder' field */
    public GWebPage.Builder clearAnchorOrder() {
      fieldSetFlags()[22] = false;
      return this;
    }

    /** Gets the value of the 'parseStatus' field */
    public GParseStatus getParseStatus() {
      return parseStatus;
    }

    /** Sets the value of the 'parseStatus' field */
    public GWebPage.Builder setParseStatus(GParseStatus value) {
      validate(fields()[23], value);
      this.parseStatus = value;
      fieldSetFlags()[23] = true;
      return this;
    }

    /** Checks whether the 'parseStatus' field has been set */
    public boolean hasParseStatus() {
      return fieldSetFlags()[23];
    }

    /** Clears the value of the 'parseStatus' field */
    public GWebPage.Builder clearParseStatus() {
      parseStatus = null;
      fieldSetFlags()[23] = false;
      return this;
    }

    /** Gets the value of the 'pageTitle' field */
    public CharSequence getPageTitle() {
      return pageTitle;
    }

    /** Sets the value of the 'pageTitle' field */
    public GWebPage.Builder setPageTitle(CharSequence value) {
      validate(fields()[24], value);
      this.pageTitle = value;
      fieldSetFlags()[24] = true;
      return this;
    }

    /** Checks whether the 'pageTitle' field has been set */
    public boolean hasPageTitle() {
      return fieldSetFlags()[24];
    }

    /** Clears the value of the 'pageTitle' field */
    public GWebPage.Builder clearPageTitle() {
      pageTitle = null;
      fieldSetFlags()[24] = false;
      return this;
    }

    /** Gets the value of the 'pageText' field */
    public CharSequence getPageText() {
      return pageText;
    }

    /** Sets the value of the 'pageText' field */
    public GWebPage.Builder setPageText(CharSequence value) {
      validate(fields()[25], value);
      this.pageText = value;
      fieldSetFlags()[25] = true;
      return this;
    }

    /** Checks whether the 'pageText' field has been set */
    public boolean hasPageText() {
      return fieldSetFlags()[25];
    }

    /** Clears the value of the 'pageText' field */
    public GWebPage.Builder clearPageText() {
      pageText = null;
      fieldSetFlags()[25] = false;
      return this;
    }

    /** Gets the value of the 'contentTitle' field */
    public CharSequence getContentTitle() {
      return contentTitle;
    }

    /** Sets the value of the 'contentTitle' field */
    public GWebPage.Builder setContentTitle(CharSequence value) {
      validate(fields()[26], value);
      this.contentTitle = value;
      fieldSetFlags()[26] = true;
      return this;
    }

    /** Checks whether the 'contentTitle' field has been set */
    public boolean hasContentTitle() {
      return fieldSetFlags()[26];
    }

    /** Clears the value of the 'contentTitle' field */
    public GWebPage.Builder clearContentTitle() {
      contentTitle = null;
      fieldSetFlags()[26] = false;
      return this;
    }

    /** Gets the value of the 'contentText' field */
    public CharSequence getContentText() {
      return contentText;
    }

    /** Sets the value of the 'contentText' field */
    public GWebPage.Builder setContentText(CharSequence value) {
      validate(fields()[27], value);
      this.contentText = value;
      fieldSetFlags()[27] = true;
      return this;
    }

    /** Checks whether the 'contentText' field has been set */
    public boolean hasContentText() {
      return fieldSetFlags()[27];
    }

    /** Clears the value of the 'contentText' field */
    public GWebPage.Builder clearContentText() {
      contentText = null;
      fieldSetFlags()[27] = false;
      return this;
    }

    /** Gets the value of the 'contentTextLen' field */
    public Integer getContentTextLen() {
      return contentTextLen;
    }

    /** Sets the value of the 'contentTextLen' field */
    public GWebPage.Builder setContentTextLen(int value) {
      validate(fields()[28], value);
      this.contentTextLen = value;
      fieldSetFlags()[28] = true;
      return this;
    }

    /** Checks whether the 'contentTextLen' field has been set */
    public boolean hasContentTextLen() {
      return fieldSetFlags()[28];
    }

    /** Clears the value of the 'contentTextLen' field */
    public GWebPage.Builder clearContentTextLen() {
      fieldSetFlags()[28] = false;
      return this;
    }

    /** Gets the value of the 'pageCategory' field */
    public CharSequence getPageCategory() {
      return pageCategory;
    }

    /** Sets the value of the 'pageCategory' field */
    public GWebPage.Builder setPageCategory(CharSequence value) {
      validate(fields()[29], value);
      this.pageCategory = value;
      fieldSetFlags()[29] = true;
      return this;
    }

    /** Checks whether the 'pageCategory' field has been set */
    public boolean hasPageCategory() {
      return fieldSetFlags()[29];
    }

    /** Clears the value of the 'pageCategory' field */
    public GWebPage.Builder clearPageCategory() {
      pageCategory = null;
      fieldSetFlags()[29] = false;
      return this;
    }

    /** Gets the value of the 'contentModifiedTime' field */
    public Long getContentModifiedTime() {
      return contentModifiedTime;
    }

    /** Sets the value of the 'contentModifiedTime' field */
    public GWebPage.Builder setContentModifiedTime(long value) {
      validate(fields()[30], value);
      this.contentModifiedTime = value;
      fieldSetFlags()[30] = true;
      return this;
    }

    /** Checks whether the 'contentModifiedTime' field has been set */
    public boolean hasContentModifiedTime() {
      return fieldSetFlags()[30];
    }

    /** Clears the value of the 'contentModifiedTime' field */
    public GWebPage.Builder clearContentModifiedTime() {
      fieldSetFlags()[30] = false;
      return this;
    }

    /** Gets the value of the 'prevContentModifiedTime' field */
    public Long getPrevContentModifiedTime() {
      return prevContentModifiedTime;
    }

    /** Sets the value of the 'prevContentModifiedTime' field */
    public GWebPage.Builder setPrevContentModifiedTime(long value) {
      validate(fields()[31], value);
      this.prevContentModifiedTime = value;
      fieldSetFlags()[31] = true;
      return this;
    }

    /** Checks whether the 'prevContentModifiedTime' field has been set */
    public boolean hasPrevContentModifiedTime() {
      return fieldSetFlags()[31];
    }

    /** Clears the value of the 'prevContentModifiedTime' field */
    public GWebPage.Builder clearPrevContentModifiedTime() {
      fieldSetFlags()[31] = false;
      return this;
    }

    /** Gets the value of the 'contentPublishTime' field */
    public Long getContentPublishTime() {
      return contentPublishTime;
    }

    /** Sets the value of the 'contentPublishTime' field */
    public GWebPage.Builder setContentPublishTime(long value) {
      validate(fields()[32], value);
      this.contentPublishTime = value;
      fieldSetFlags()[32] = true;
      return this;
    }

    /** Checks whether the 'contentPublishTime' field has been set */
    public boolean hasContentPublishTime() {
      return fieldSetFlags()[32];
    }

    /** Clears the value of the 'contentPublishTime' field */
    public GWebPage.Builder clearContentPublishTime() {
      fieldSetFlags()[32] = false;
      return this;
    }

    /** Gets the value of the 'prevContentPublishTime' field */
    public Long getPrevContentPublishTime() {
      return prevContentPublishTime;
    }

    /** Sets the value of the 'prevContentPublishTime' field */
    public GWebPage.Builder setPrevContentPublishTime(long value) {
      validate(fields()[33], value);
      this.prevContentPublishTime = value;
      fieldSetFlags()[33] = true;
      return this;
    }

    /** Checks whether the 'prevContentPublishTime' field has been set */
    public boolean hasPrevContentPublishTime() {
      return fieldSetFlags()[33];
    }

    /** Clears the value of the 'prevContentPublishTime' field */
    public GWebPage.Builder clearPrevContentPublishTime() {
      fieldSetFlags()[33] = false;
      return this;
    }

    /** Gets the value of the 'refContentPublishTime' field */
    public Long getRefContentPublishTime() {
      return refContentPublishTime;
    }

    /** Sets the value of the 'refContentPublishTime' field */
    public GWebPage.Builder setRefContentPublishTime(long value) {
      validate(fields()[34], value);
      this.refContentPublishTime = value;
      fieldSetFlags()[34] = true;
      return this;
    }

    /** Checks whether the 'refContentPublishTime' field has been set */
    public boolean hasRefContentPublishTime() {
      return fieldSetFlags()[34];
    }

    /** Clears the value of the 'refContentPublishTime' field */
    public GWebPage.Builder clearRefContentPublishTime() {
      fieldSetFlags()[34] = false;
      return this;
    }

    /** Gets the value of the 'prevRefContentPublishTime' field */
    public Long getPrevRefContentPublishTime() {
      return prevRefContentPublishTime;
    }

    /** Sets the value of the 'prevRefContentPublishTime' field */
    public GWebPage.Builder setPrevRefContentPublishTime(long value) {
      validate(fields()[35], value);
      this.prevRefContentPublishTime = value;
      fieldSetFlags()[35] = true;
      return this;
    }

    /** Checks whether the 'prevRefContentPublishTime' field has been set */
    public boolean hasPrevRefContentPublishTime() {
      return fieldSetFlags()[35];
    }

    /** Clears the value of the 'prevRefContentPublishTime' field */
    public GWebPage.Builder clearPrevRefContentPublishTime() {
      fieldSetFlags()[35] = false;
      return this;
    }

    /** Gets the value of the 'prevSignature' field */
    public java.nio.ByteBuffer getPrevSignature() {
      return prevSignature;
    }

    /** Sets the value of the 'prevSignature' field */
    public GWebPage.Builder setPrevSignature(java.nio.ByteBuffer value) {
      validate(fields()[36], value);
      this.prevSignature = value;
      fieldSetFlags()[36] = true;
      return this;
    }

    /** Checks whether the 'prevSignature' field has been set */
    public boolean hasPrevSignature() {
      return fieldSetFlags()[36];
    }

    /** Clears the value of the 'prevSignature' field */
    public GWebPage.Builder clearPrevSignature() {
      prevSignature = null;
      fieldSetFlags()[36] = false;
      return this;
    }

    /** Gets the value of the 'signature' field */
    public java.nio.ByteBuffer getSignature() {
      return signature;
    }

    /** Sets the value of the 'signature' field */
    public GWebPage.Builder setSignature(java.nio.ByteBuffer value) {
      validate(fields()[37], value);
      this.signature = value;
      fieldSetFlags()[37] = true;
      return this;
    }

    /** Checks whether the 'signature' field has been set */
    public boolean hasSignature() {
      return fieldSetFlags()[37];
    }

    /** Clears the value of the 'signature' field */
    public GWebPage.Builder clearSignature() {
      signature = null;
      fieldSetFlags()[37] = false;
      return this;
    }

    /** Gets the value of the 'contentScore' field */
    public Float getContentScore() {
      return contentScore;
    }

    /** Sets the value of the 'contentScore' field */
    public GWebPage.Builder setContentScore(float value) {
      validate(fields()[38], value);
      this.contentScore = value;
      fieldSetFlags()[38] = true;
      return this;
    }

    /** Checks whether the 'contentScore' field has been set */
    public boolean hasContentScore() {
      return fieldSetFlags()[38];
    }

    /** Clears the value of the 'contentScore' field */
    public GWebPage.Builder clearContentScore() {
      fieldSetFlags()[38] = false;
      return this;
    }

    /** Gets the value of the 'score' field */
    public Float getScore() {
      return score;
    }

    /** Sets the value of the 'score' field */
    public GWebPage.Builder setScore(float value) {
      validate(fields()[39], value);
      this.score = value;
      fieldSetFlags()[39] = true;
      return this;
    }

    /** Checks whether the 'score' field has been set */
    public boolean hasScore() {
      return fieldSetFlags()[39];
    }

    /** Clears the value of the 'score' field */
    public GWebPage.Builder clearScore() {
      fieldSetFlags()[39] = false;
      return this;
    }

    /** Gets the value of the 'sortScore' field */
    public CharSequence getSortScore() {
      return sortScore;
    }

    /** Sets the value of the 'sortScore' field */
    public GWebPage.Builder setSortScore(CharSequence value) {
      validate(fields()[40], value);
      this.sortScore = value;
      fieldSetFlags()[40] = true;
      return this;
    }

    /** Checks whether the 'sortScore' field has been set */
    public boolean hasSortScore() {
      return fieldSetFlags()[40];
    }

    /** Clears the value of the 'sortScore' field */
    public GWebPage.Builder clearSortScore() {
      sortScore = null;
      fieldSetFlags()[40] = false;
      return this;
    }

    /** Gets the value of the 'pageCounters' field */
    public java.util.Map<CharSequence, Integer> getPageCounters() {
      return pageCounters;
    }

    /** Sets the value of the 'pageCounters' field */
    public GWebPage.Builder setPageCounters(java.util.Map<CharSequence, Integer> value) {
      validate(fields()[41], value);
      this.pageCounters = value;
      fieldSetFlags()[41] = true;
      return this;
    }

    /** Checks whether the 'pageCounters' field has been set */
    public boolean hasPageCounters() {
      return fieldSetFlags()[41];
    }

    /** Clears the value of the 'pageCounters' field */
    public GWebPage.Builder clearPageCounters() {
      pageCounters = null;
      fieldSetFlags()[41] = false;
      return this;
    }

    /** Gets the value of the 'headers' field */
    public java.util.Map<CharSequence, CharSequence> getHeaders() {
      return headers;
    }

    /** Sets the value of the 'headers' field */
    public GWebPage.Builder setHeaders(java.util.Map<CharSequence, CharSequence> value) {
      validate(fields()[42], value);
      this.headers = value;
      fieldSetFlags()[42] = true;
      return this;
    }

    /** Checks whether the 'headers' field has been set */
    public boolean hasHeaders() {
      return fieldSetFlags()[42];
    }

    /** Clears the value of the 'headers' field */
    public GWebPage.Builder clearHeaders() {
      headers = null;
      fieldSetFlags()[42] = false;
      return this;
    }

    /** Gets the value of the 'links' field */
    public java.util.List<CharSequence> getLinks() {
      return links;
    }

    /** Sets the value of the 'links' field */
    public GWebPage.Builder setLinks(java.util.List<CharSequence> value) {
      validate(fields()[43], value);
      this.links = value;
      fieldSetFlags()[43] = true;
      return this;
    }

    /** Checks whether the 'links' field has been set */
    public boolean hasLinks() {
      return fieldSetFlags()[43];
    }

    /** Clears the value of the 'links' field */
    public GWebPage.Builder clearLinks() {
      links = null;
      fieldSetFlags()[43] = false;
      return this;
    }

    /** Gets the value of the 'liveLinks' field */
    public java.util.Map<CharSequence, GHypeLink> getLiveLinks() {
      return liveLinks;
    }

    /** Sets the value of the 'liveLinks' field */
    public GWebPage.Builder setLiveLinks(java.util.Map<CharSequence, GHypeLink> value) {
      validate(fields()[44], value);
      this.liveLinks = value;
      fieldSetFlags()[44] = true;
      return this;
    }

    /** Checks whether the 'liveLinks' field has been set */
    public boolean hasLiveLinks() {
      return fieldSetFlags()[44];
    }

    /** Clears the value of the 'liveLinks' field */
    public GWebPage.Builder clearLiveLinks() {
      liveLinks = null;
      fieldSetFlags()[44] = false;
      return this;
    }

    /** Gets the value of the 'vividLinks' field */
    public java.util.Map<CharSequence, CharSequence> getVividLinks() {
      return vividLinks;
    }

    /** Sets the value of the 'vividLinks' field */
    public GWebPage.Builder setVividLinks(java.util.Map<CharSequence, CharSequence> value) {
      validate(fields()[45], value);
      this.vividLinks = value;
      fieldSetFlags()[45] = true;
      return this;
    }

    /** Checks whether the 'vividLinks' field has been set */
    public boolean hasVividLinks() {
      return fieldSetFlags()[45];
    }

    /** Clears the value of the 'vividLinks' field */
    public GWebPage.Builder clearVividLinks() {
      vividLinks = null;
      fieldSetFlags()[45] = false;
      return this;
    }

    /** Gets the value of the 'deadLinks' field */
    public java.util.List<CharSequence> getDeadLinks() {
      return deadLinks;
    }

    /** Sets the value of the 'deadLinks' field */
    public GWebPage.Builder setDeadLinks(java.util.List<CharSequence> value) {
      validate(fields()[46], value);
      this.deadLinks = value;
      fieldSetFlags()[46] = true;
      return this;
    }

    /** Checks whether the 'deadLinks' field has been set */
    public boolean hasDeadLinks() {
      return fieldSetFlags()[46];
    }

    /** Clears the value of the 'deadLinks' field */
    public GWebPage.Builder clearDeadLinks() {
      deadLinks = null;
      fieldSetFlags()[46] = false;
      return this;
    }

    /** Gets the value of the 'inlinks' field */
    public java.util.Map<CharSequence, CharSequence> getInlinks() {
      return inlinks;
    }

    /** Sets the value of the 'inlinks' field */
    public GWebPage.Builder setInlinks(java.util.Map<CharSequence, CharSequence> value) {
      validate(fields()[47], value);
      this.inlinks = value;
      fieldSetFlags()[47] = true;
      return this;
    }

    /** Checks whether the 'inlinks' field has been set */
    public boolean hasInlinks() {
      return fieldSetFlags()[47];
    }

    /** Clears the value of the 'inlinks' field */
    public GWebPage.Builder clearInlinks() {
      inlinks = null;
      fieldSetFlags()[47] = false;
      return this;
    }

    /** Gets the value of the 'markers' field */
    public java.util.Map<CharSequence, CharSequence> getMarkers() {
      return markers;
    }

    /** Sets the value of the 'markers' field */
    public GWebPage.Builder setMarkers(java.util.Map<CharSequence, CharSequence> value) {
      validate(fields()[48], value);
      this.markers = value;
      fieldSetFlags()[48] = true;
      return this;
    }

    /** Checks whether the 'markers' field has been set */
    public boolean hasMarkers() {
      return fieldSetFlags()[48];
    }

    /** Clears the value of the 'markers' field */
    public GWebPage.Builder clearMarkers() {
      markers = null;
      fieldSetFlags()[48] = false;
      return this;
    }

    /** Gets the value of the 'metadata' field */
    public java.util.Map<CharSequence,java.nio.ByteBuffer> getMetadata() {
      return metadata;
    }

    /** Sets the value of the 'metadata' field */
    public GWebPage.Builder setMetadata(java.util.Map<CharSequence,java.nio.ByteBuffer> value) {
      validate(fields()[49], value);
      this.metadata = value;
      fieldSetFlags()[49] = true;
      return this;
    }

    /** Checks whether the 'metadata' field has been set */
    public boolean hasMetadata() {
      return fieldSetFlags()[49];
    }

    /** Clears the value of the 'metadata' field */
    public GWebPage.Builder clearMetadata() {
      metadata = null;
      fieldSetFlags()[49] = false;
      return this;
    }

    /** Gets the value of the 'pageModel' field */
    public java.util.List<GFieldGroup> getPageModel() {
      return pageModel;
    }

    /** Sets the value of the 'pageModel' field */
    public GWebPage.Builder setPageModel(java.util.List<GFieldGroup> value) {
      validate(fields()[50], value);
      this.pageModel = value;
      fieldSetFlags()[50] = true;
      return this;
    }

    /** Checks whether the 'pageModel' field has been set */
    public boolean hasPageModel() {
      return fieldSetFlags()[50];
    }

    /** Clears the value of the 'pageModel' field */
    public GWebPage.Builder clearPageModel() {
      pageModel = null;
      fieldSetFlags()[50] = false;
      return this;
    }

    @Override
    public GWebPage build() {
      try {
        GWebPage record = new GWebPage();
        record.createTime = fieldSetFlags()[0] ? this.createTime : (Long) defaultValue(fields()[0]);
        record.distance = fieldSetFlags()[1] ? this.distance : (Integer) defaultValue(fields()[1]);
        record.fetchCount = fieldSetFlags()[2] ? this.fetchCount : (Integer) defaultValue(fields()[2]);
        record.fetchPriority = fieldSetFlags()[3] ? this.fetchPriority : (Integer) defaultValue(fields()[3]);
        record.fetchInterval = fieldSetFlags()[4] ? this.fetchInterval : (Integer) defaultValue(fields()[4]);
        record.zoneId = fieldSetFlags()[5] ? this.zoneId : (CharSequence) defaultValue(fields()[5]);
        record.options = fieldSetFlags()[6] ? this.options : (CharSequence) defaultValue(fields()[6]);
        record.batchId = fieldSetFlags()[7] ? this.batchId : (CharSequence) defaultValue(fields()[7]);
        record.crawlStatus = fieldSetFlags()[8] ? this.crawlStatus : (Integer) defaultValue(fields()[8]);
        record.prevFetchTime = fieldSetFlags()[9] ? this.prevFetchTime : (Long) defaultValue(fields()[9]);
        record.fetchTime = fieldSetFlags()[10] ? this.fetchTime : (Long) defaultValue(fields()[10]);
        record.fetchRetries = fieldSetFlags()[11] ? this.fetchRetries : (Integer) defaultValue(fields()[11]);
        record.reprUrl = fieldSetFlags()[12] ? this.reprUrl : (CharSequence) defaultValue(fields()[12]);
        record.prevModifiedTime = fieldSetFlags()[13] ? this.prevModifiedTime : (Long) defaultValue(fields()[13]);
        record.modifiedTime = fieldSetFlags()[14] ? this.modifiedTime : (Long) defaultValue(fields()[14]);
        record.protocolStatus = fieldSetFlags()[15] ? this.protocolStatus : (GProtocolStatus) defaultValue(fields()[15]);
        record.encoding = fieldSetFlags()[16] ? this.encoding : (CharSequence) defaultValue(fields()[16]);
        record.contentType = fieldSetFlags()[17] ? this.contentType : (CharSequence) defaultValue(fields()[17]);
        record.content = fieldSetFlags()[18] ? this.content : (java.nio.ByteBuffer) defaultValue(fields()[18]);
        record.baseUrl = fieldSetFlags()[19] ? this.baseUrl : (CharSequence) defaultValue(fields()[19]);
        record.referrer = fieldSetFlags()[20] ? this.referrer : (CharSequence) defaultValue(fields()[20]);
        record.anchor = fieldSetFlags()[21] ? this.anchor : (CharSequence) defaultValue(fields()[21]);
        record.anchorOrder = fieldSetFlags()[22] ? this.anchorOrder : (Integer) defaultValue(fields()[22]);
        record.parseStatus = fieldSetFlags()[23] ? this.parseStatus : (GParseStatus) defaultValue(fields()[23]);
        record.pageTitle = fieldSetFlags()[24] ? this.pageTitle : (CharSequence) defaultValue(fields()[24]);
        record.pageText = fieldSetFlags()[25] ? this.pageText : (CharSequence) defaultValue(fields()[25]);
        record.contentTitle = fieldSetFlags()[26] ? this.contentTitle : (CharSequence) defaultValue(fields()[26]);
        record.contentText = fieldSetFlags()[27] ? this.contentText : (CharSequence) defaultValue(fields()[27]);
        record.contentTextLen = fieldSetFlags()[28] ? this.contentTextLen : (Integer) defaultValue(fields()[28]);
        record.pageCategory = fieldSetFlags()[29] ? this.pageCategory : (CharSequence) defaultValue(fields()[29]);
        record.contentModifiedTime = fieldSetFlags()[30] ? this.contentModifiedTime : (Long) defaultValue(fields()[30]);
        record.prevContentModifiedTime = fieldSetFlags()[31] ? this.prevContentModifiedTime : (Long) defaultValue(fields()[31]);
        record.contentPublishTime = fieldSetFlags()[32] ? this.contentPublishTime : (Long) defaultValue(fields()[32]);
        record.prevContentPublishTime = fieldSetFlags()[33] ? this.prevContentPublishTime : (Long) defaultValue(fields()[33]);
        record.refContentPublishTime = fieldSetFlags()[34] ? this.refContentPublishTime : (Long) defaultValue(fields()[34]);
        record.prevRefContentPublishTime = fieldSetFlags()[35] ? this.prevRefContentPublishTime : (Long) defaultValue(fields()[35]);
        record.prevSignature = fieldSetFlags()[36] ? this.prevSignature : (java.nio.ByteBuffer) defaultValue(fields()[36]);
        record.signature = fieldSetFlags()[37] ? this.signature : (java.nio.ByteBuffer) defaultValue(fields()[37]);
        record.contentScore = fieldSetFlags()[38] ? this.contentScore : (Float) defaultValue(fields()[38]);
        record.score = fieldSetFlags()[39] ? this.score : (Float) defaultValue(fields()[39]);
        record.sortScore = fieldSetFlags()[40] ? this.sortScore : (CharSequence) defaultValue(fields()[40]);
        record.pageCounters = fieldSetFlags()[41] ? this.pageCounters : (java.util.Map<CharSequence, Integer>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[41]));
        record.headers = fieldSetFlags()[42] ? this.headers : (java.util.Map<CharSequence, CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[42]));
        record.links = fieldSetFlags()[43] ? this.links : (java.util.List<CharSequence>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[43]));
        record.liveLinks = fieldSetFlags()[44] ? this.liveLinks : (java.util.Map<CharSequence, GHypeLink>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[44]));
        record.vividLinks = fieldSetFlags()[45] ? this.vividLinks : (java.util.Map<CharSequence, CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[45]));
        record.deadLinks = fieldSetFlags()[46] ? this.deadLinks : (java.util.List<CharSequence>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[46]));
        record.inlinks = fieldSetFlags()[47] ? this.inlinks : (java.util.Map<CharSequence, CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[47]));
        record.markers = fieldSetFlags()[48] ? this.markers : (java.util.Map<CharSequence, CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[48]));
        record.metadata = fieldSetFlags()[49] ? this.metadata : (java.util.Map<CharSequence,java.nio.ByteBuffer>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[49]));
        record.pageModel = fieldSetFlags()[50] ? this.pageModel : (java.util.List<GFieldGroup>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[50]));
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  /**
   * <p>getTombstone.</p>
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GWebPage.Tombstone} object.
   */
  public Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  /**
   * <p>newInstance.</p>
   *
   * @return a {@link ai.platon.pulsar.persist.gora.generated.GWebPage} object.
   */
  public GWebPage newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();

  public static final class Tombstone extends GWebPage implements org.apache.gora.persistency.Tombstone {

      private Tombstone() { }

	  		  /**
	   * Gets the value of the 'createTime' field.
		   */
	  public Long getCreateTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'createTime' field.
		   * @param value the value to set.
	   */
	  public void setCreateTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'createTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCreateTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'distance' field.
		   */
	  public Integer getDistance() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'distance' field.
		   * @param value the value to set.
	   */
	  public void setDistance(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'distance' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDistanceDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'fetchCount' field.
		   */
	  public Integer getFetchCount() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'fetchCount' field.
		   * @param value the value to set.
	   */
	  public void setFetchCount(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'fetchCount' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchCountDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'fetchPriority' field.
		   */
	  public Integer getFetchPriority() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'fetchPriority' field.
		   * @param value the value to set.
	   */
	  public void setFetchPriority(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'fetchPriority' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchPriorityDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'fetchInterval' field.
		   */
	  public Integer getFetchInterval() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'fetchInterval' field.
		   * @param value the value to set.
	   */
	  public void setFetchInterval(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'fetchInterval' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchIntervalDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'zoneId' field.
		   */
	  public CharSequence getZoneId() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'zoneId' field.
		   * @param value the value to set.
	   */
	  public void setZoneId(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'zoneId' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isZoneIdDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'options' field.
		   */
	  public CharSequence getOptions() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'options' field.
		   * @param value the value to set.
	   */
	  public void setOptions(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'options' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isOptionsDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'batchId' field.
		   */
	  public CharSequence getBatchId() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'batchId' field.
		   * @param value the value to set.
	   */
	  public void setBatchId(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'batchId' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isBatchIdDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'crawlStatus' field.
		   */
	  public Integer getCrawlStatus() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'crawlStatus' field.
		   * @param value the value to set.
	   */
	  public void setCrawlStatus(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'crawlStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCrawlStatusDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'prevFetchTime' field.
		   */
	  public Long getPrevFetchTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'prevFetchTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevFetchTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'prevFetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevFetchTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'fetchTime' field.
		   */
	  public Long getFetchTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'fetchTime' field.
		   * @param value the value to set.
	   */
	  public void setFetchTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'fetchTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'fetchRetries' field.
		   */
	  public Integer getFetchRetries() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'fetchRetries' field.
		   * @param value the value to set.
	   */
	  public void setFetchRetries(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'fetchRetries' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFetchRetriesDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'reprUrl' field.
		   */
	  public CharSequence getReprUrl() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'reprUrl' field.
		   * @param value the value to set.
	   */
	  public void setReprUrl(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'reprUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isReprUrlDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'prevModifiedTime' field.
		   */
	  public Long getPrevModifiedTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'prevModifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevModifiedTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'prevModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevModifiedTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'modifiedTime' field.
		   */
	  public Long getModifiedTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'modifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setModifiedTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'modifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isModifiedTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'protocolStatus' field.
		   */
	  public GProtocolStatus getProtocolStatus() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'protocolStatus' field.
		   * @param value the value to set.
	   */
	  public void setProtocolStatus(GProtocolStatus value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'protocolStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isProtocolStatusDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'encoding' field.
		   */
	  public CharSequence getEncoding() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'encoding' field.
		   * @param value the value to set.
	   */
	  public void setEncoding(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'encoding' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isEncodingDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentType' field.
		   */
	  public CharSequence getContentType() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentType' field.
		   * @param value the value to set.
	   */
	  public void setContentType(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentType' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTypeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'content' field.
	   * The entire raw document content e.g. raw XHTML	   */
	  public java.nio.ByteBuffer getContent() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'content' field.
	   * The entire raw document content e.g. raw XHTML	   * @param value the value to set.
	   */
	  public void setContent(java.nio.ByteBuffer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'content' field. A field is dirty if it represents a change that has not yet been written to the database.
	   * The entire raw document content e.g. raw XHTML	   * @param value the value to set.
	   */
	  public boolean isContentDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'baseUrl' field.
		   */
	  public CharSequence getBaseUrl() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'baseUrl' field.
		   * @param value the value to set.
	   */
	  public void setBaseUrl(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'baseUrl' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isBaseUrlDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'referrer' field.
		   */
	  public CharSequence getReferrer() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'referrer' field.
		   * @param value the value to set.
	   */
	  public void setReferrer(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'referrer' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isReferrerDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'anchor' field.
		   */
	  public CharSequence getAnchor() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'anchor' field.
		   * @param value the value to set.
	   */
	  public void setAnchor(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'anchor' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAnchorDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'anchorOrder' field.
		   */
	  public Integer getAnchorOrder() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'anchorOrder' field.
		   * @param value the value to set.
	   */
	  public void setAnchorOrder(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'anchorOrder' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAnchorOrderDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'parseStatus' field.
		   */
	  public GParseStatus getParseStatus() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'parseStatus' field.
		   * @param value the value to set.
	   */
	  public void setParseStatus(GParseStatus value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'parseStatus' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isParseStatusDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'pageTitle' field.
		   */
	  public CharSequence getPageTitle() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'pageTitle' field.
		   * @param value the value to set.
	   */
	  public void setPageTitle(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'pageTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageTitleDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'pageText' field.
		   */
	  public CharSequence getPageText() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'pageText' field.
		   * @param value the value to set.
	   */
	  public void setPageText(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'pageText' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageTextDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentTitle' field.
		   */
	  public CharSequence getContentTitle() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentTitle' field.
		   * @param value the value to set.
	   */
	  public void setContentTitle(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentTitle' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTitleDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentText' field.
		   */
	  public CharSequence getContentText() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentText' field.
		   * @param value the value to set.
	   */
	  public void setContentText(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentText' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTextDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentTextLen' field.
		   */
	  public Integer getContentTextLen() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentTextLen' field.
		   * @param value the value to set.
	   */
	  public void setContentTextLen(Integer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentTextLen' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentTextLenDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'pageCategory' field.
		   */
	  public CharSequence getPageCategory() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'pageCategory' field.
		   * @param value the value to set.
	   */
	  public void setPageCategory(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'pageCategory' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageCategoryDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentModifiedTime' field.
		   */
	  public Long getContentModifiedTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentModifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setContentModifiedTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentModifiedTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'prevContentModifiedTime' field.
		   */
	  public Long getPrevContentModifiedTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'prevContentModifiedTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevContentModifiedTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'prevContentModifiedTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevContentModifiedTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentPublishTime' field.
		   */
	  public Long getContentPublishTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setContentPublishTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentPublishTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'prevContentPublishTime' field.
		   */
	  public Long getPrevContentPublishTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'prevContentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevContentPublishTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'prevContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevContentPublishTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'refContentPublishTime' field.
		   */
	  public Long getRefContentPublishTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'refContentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setRefContentPublishTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'refContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isRefContentPublishTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'prevRefContentPublishTime' field.
		   */
	  public Long getPrevRefContentPublishTime() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'prevRefContentPublishTime' field.
		   * @param value the value to set.
	   */
	  public void setPrevRefContentPublishTime(Long value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'prevRefContentPublishTime' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevRefContentPublishTimeDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'prevSignature' field.
		   */
	  public java.nio.ByteBuffer getPrevSignature() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'prevSignature' field.
		   * @param value the value to set.
	   */
	  public void setPrevSignature(java.nio.ByteBuffer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'prevSignature' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevSignatureDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'signature' field.
		   */
	  public java.nio.ByteBuffer getSignature() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'signature' field.
		   * @param value the value to set.
	   */
	  public void setSignature(java.nio.ByteBuffer value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'signature' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isSignatureDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'contentScore' field.
		   */
	  public Float getContentScore() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'contentScore' field.
		   * @param value the value to set.
	   */
	  public void setContentScore(Float value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'contentScore' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isContentScoreDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'score' field.
		   */
	  public Float getScore() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'score' field.
		   * @param value the value to set.
	   */
	  public void setScore(Float value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'score' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isScoreDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'sortScore' field.
		   */
	  public CharSequence getSortScore() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'sortScore' field.
		   * @param value the value to set.
	   */
	  public void setSortScore(CharSequence value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'sortScore' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isSortScoreDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'pageCounters' field.
		   */
	  public java.util.Map<CharSequence, Integer> getPageCounters() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'pageCounters' field.
		   * @param value the value to set.
	   */
	  public void setPageCounters(java.util.Map<CharSequence, Integer> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'pageCounters' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageCountersDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'headers' field.
		   */
	  public java.util.Map<CharSequence, CharSequence> getHeaders() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'headers' field.
		   * @param value the value to set.
	   */
	  public void setHeaders(java.util.Map<CharSequence, CharSequence> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'headers' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isHeadersDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'links' field.
		   */
	  public java.util.List<CharSequence> getLinks() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'links' field.
		   * @param value the value to set.
	   */
	  public void setLinks(java.util.List<CharSequence> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'links' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isLinksDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'liveLinks' field.
		   */
	  public java.util.Map<CharSequence, GHypeLink> getLiveLinks() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'liveLinks' field.
		   * @param value the value to set.
	   */
	  public void setLiveLinks(java.util.Map<CharSequence, GHypeLink> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'liveLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isLiveLinksDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'vividLinks' field.
		   */
	  public java.util.Map<CharSequence, CharSequence> getVividLinks() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'vividLinks' field.
		   * @param value the value to set.
	   */
	  public void setVividLinks(java.util.Map<CharSequence, CharSequence> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'vividLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isVividLinksDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'deadLinks' field.
		   */
	  public java.util.List<CharSequence> getDeadLinks() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'deadLinks' field.
		   * @param value the value to set.
	   */
	  public void setDeadLinks(java.util.List<CharSequence> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'deadLinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDeadLinksDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'inlinks' field.
		   */
	  public java.util.Map<CharSequence, CharSequence> getInlinks() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'inlinks' field.
		   * @param value the value to set.
	   */
	  public void setInlinks(java.util.Map<CharSequence, CharSequence> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'inlinks' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isInlinksDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'markers' field.
		   */
	  public java.util.Map<CharSequence, CharSequence> getMarkers() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'markers' field.
		   * @param value the value to set.
	   */
	  public void setMarkers(java.util.Map<CharSequence, CharSequence> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'markers' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMarkersDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'metadata' field.
		   */
	  public java.util.Map<CharSequence,java.nio.ByteBuffer> getMetadata() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'metadata' field.
		   * @param value the value to set.
	   */
	  public void setMetadata(java.util.Map<CharSequence,java.nio.ByteBuffer> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'metadata' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMetadataDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }

				  /**
	   * Gets the value of the 'pageModel' field.
		   */
	  public java.util.List<GFieldGroup> getPageModel() {
	    throw new UnsupportedOperationException("Get is not supported on tombstones");
	  }

	  /**
	   * Sets the value of the 'pageModel' field.
		   * @param value the value to set.
	   */
	  public void setPageModel(java.util.List<GFieldGroup> value) {
	    throw new UnsupportedOperationException("Set is not supported on tombstones");
	  }

	  /**
	   * Checks the dirty status of the 'pageModel' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPageModelDirty() {
	    throw new UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
		  
  }

  private static final org.apache.avro.io.DatumWriter
            DATUM_WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);
  private static final org.apache.avro.io.DatumReader
            DATUM_READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  /**
   * {@inheritDoc}
   *
   * Writes AVRO data bean to output stream in the form of AVRO Binary encoding format. This will transform
   * AVRO data bean from its Java object form to it s serializable form.
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
   * {@inheritDoc}
   *
   * Reads AVRO data bean from input stream in it s AVRO Binary encoding format to Java object format.
   * This will transform AVRO data bean from it s serializable form to deserialized Java object form.
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

