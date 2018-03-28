/**
 * Created by vincent on 16-11-9.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
package fun.platonic.pulsar.boilerpipe.document;

import fun.platonic.pulsar.boilerpipe.utils.PageCategory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TextDocument implements Cloneable {

  /** Scent Stat */
  public class Stat {
    public int _char = 0;
    public int _a = 0;
    public int _img = 0;
  }

  private final List<TextBlock> textBlocks;
  private CharSequence baseUrl;
  private CharSequence pageTitle;
  private CharSequence contentTitle;
  private Instant publishTime = Instant.EPOCH;
  private Instant modifiedTime = Instant.EPOCH;
  private int dateTimeCount = 0;
  private Stat stat = new Stat();
  private PageCategory pageCategory = PageCategory.UNKNOWN;
  private Map<CharSequence, CharSequence> fields = new LinkedHashMap<>();

  public TextDocument(CharSequence baseUrl, CharSequence pageTitle, List<TextBlock> textBlocks) {
    this.baseUrl = baseUrl;
    this.pageTitle = pageTitle;
    this.textBlocks = textBlocks;
  }

  public List<TextBlock> getTextBlocks() { return textBlocks; }

  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

  public String getBaseUrl() { return baseUrl.toString(); }

  public String getPageTitle() { return pageTitle == null ? "" : pageTitle.toString(); }

  public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

  public String getContentTitle() { return contentTitle == null ? "" : contentTitle.toString(); }

  public void setContentTitle(CharSequence contentTitle) { this.contentTitle = contentTitle; }

  public Instant getPublishTime() { return publishTime; }

  public void setPublishTime(Instant publishTime) { this.publishTime = publishTime; }

  public Instant getModifiedTime() { return modifiedTime; }

  public void setModifiedTime(Instant modifiedTime) { this.modifiedTime = modifiedTime; }

  public int getDateTimeCount() { return dateTimeCount; }

  public void setDateTimeCount(int dateTimeCount) { this.dateTimeCount = dateTimeCount; }

  public void setStat(Stat stat) { this.stat = stat; }

  public Stat getStat() { return this.stat; }

  public PageCategory getPageCategory() {
    return pageCategory;
  }

  public String getPageCategoryAsString() {
    return pageCategory.toString();
  }

  public void setPageCategory(PageCategory pageCategory) {
    this.pageCategory = pageCategory;
  }

  public Map<CharSequence, CharSequence> getFields() {
    return fields;
  }

  public CharSequence getField(CharSequence name) {
    return fields.get(name);
  }

  public CharSequence getFieldOrDefault(CharSequence name, CharSequence defaultValue) {
    return fields.getOrDefault(name, defaultValue);
  }

  public void setField(CharSequence name, CharSequence value) {
    fields.put(name, value);
  }

  public String getTextContent() {
    return getTextContent(true, false);
  }

  public String getTextContent(boolean includeContent, boolean includeNonContent) {
    StringBuilder sb = new StringBuilder();
    for (TextBlock block : getTextBlocks()) {
      if (block.isContent()) {
        if (!includeContent) {
          continue;
        }
      } else {
        if (!includeNonContent) {
          continue;
        }
      }
      sb.append(block.getText());
      sb.append('\n');
    }
    return sb.toString();
  }

  public String getHtmlContent() {
    return getHtmlContent(true, false);
  }

  public String getHtmlContent(boolean includeContent, boolean includeNonContent) {
    StringBuilder sb = new StringBuilder();
    sb.append("<div>");
    LOOP:
    for (TextBlock block : getTextBlocks()) {
      if (block.isContent()) {
        if (!includeContent) {
          continue LOOP;
        }
      } else {
        if (!includeNonContent) {
          continue LOOP;
        }
      }

      if (block.getRichText().length() > 1) {
        sb.append("<div>");
        sb.append(block.getRichText());
        sb.append("</div>\n");
      }
    }
    sb.append("</div>");
    return sb.toString();
  }

  public String debugString() {
    StringBuilder sb = new StringBuilder();
    for (TextBlock tb : getTextBlocks()) {
      if (!tb.getLabels().isEmpty()) {
        sb.append("[LABLE : " + tb.getLabels() + "]");
      }
      sb.append(tb.toString());
      sb.append('\n');
    }
    return sb.toString();
  }

  @Override
  public TextDocument clone() {
    final List<TextBlock> list = new ArrayList<>(textBlocks.size());
    for (TextBlock tb : textBlocks) {
      list.add(tb.clone());
    }
    return new TextDocument(baseUrl, pageTitle, list);
  }
}
