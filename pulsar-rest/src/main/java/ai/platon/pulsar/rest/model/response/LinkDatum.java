package ai.platon.pulsar.rest.model.response;

/**
 * Created by vincent on 17-6-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class LinkDatum {
  private String url;
  private String anchor;
  private int order;

  public LinkDatum() {
  }

  public LinkDatum(String url, String anchor, int order) {
    this.url = url;
    this.anchor = anchor;
    this.order = order;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getAnchor() {
    return anchor;
  }

  public void setAnchor(String anchor) {
    this.anchor = anchor;
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public String toString() {
    return "url: " + url + ", anchor: " + anchor + ", order: " + order;
  }
}
