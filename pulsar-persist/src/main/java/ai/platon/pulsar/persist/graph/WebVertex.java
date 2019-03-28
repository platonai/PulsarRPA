package ai.platon.pulsar.persist.graph;

import ai.platon.pulsar.persist.WebPage;

/**
 * Created by vincent on 16-12-29.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class WebVertex {
    public final static WebVertex EMPTY_WEB_VERTEX = new WebVertex();

    private String url;
    private WebPage page;

    public WebVertex() {
    }

    public WebVertex(CharSequence url) {
        this.url = url.toString();
    }

    public WebVertex(WebPage page) {
        this.url = page.toString();
        this.page = page;
    }

    public String getUrl() {
        return url;
    }

    public void reset(WebPage page) {
        this.url = page.getUrl();
        this.page = page;
    }

    public WebPage getWebPage() {
        return page;
    }

    public void setWebPage(WebPage page) {
        this.page = page;
    }

    public boolean hasWebPage() {
        return this.page != null;
    }

    @Override
    public boolean equals(Object vertex) {
        if (!(vertex instanceof WebVertex)) {
            return false;
        }

        return url.equals(((WebVertex) vertex).url);
    }

    @Override
    public int hashCode() {
        return (url == null) ? 0 : url.hashCode();
    }

    @Override
    public String toString() {
        return url;
    }
}
