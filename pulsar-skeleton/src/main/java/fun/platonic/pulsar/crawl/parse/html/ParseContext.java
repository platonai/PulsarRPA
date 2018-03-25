package fun.platonic.pulsar.crawl.parse.html;

import org.w3c.dom.DocumentFragment;
import fun.platonic.pulsar.crawl.parse.ParseResult;
import fun.platonic.pulsar.persist.WebPage;

/**
 * Created by vincent on 17-7-28.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class ParseContext {
    private final String url;
    private final WebPage page;
    private HTMLMetaTags metaTags;
    private DocumentFragment documentFragment;
    private ParseResult parseResult;

    public ParseContext(WebPage page, HTMLMetaTags metaTags, DocumentFragment documentFragment, ParseResult parseResult) {
        this.url = page.getUrl();
        this.page = page;
        this.metaTags = metaTags;
        this.documentFragment = documentFragment;
        this.parseResult = parseResult;
    }

    public String getUrl() {
        return url;
    }

    public WebPage getPage() {
        return page;
    }

    public HTMLMetaTags getMetaTags() {
        return metaTags;
    }

    public void setMetaTags(HTMLMetaTags metaTags) {
        this.metaTags = metaTags;
    }

    public DocumentFragment getDocumentFragment() {
        return documentFragment;
    }

    public void setDocumentFragment(DocumentFragment documentFragment) {
        this.documentFragment = documentFragment;
    }

    public ParseResult getParseResult() {
        return parseResult;
    }

    public void setParseResult(ParseResult parseResult) {
        this.parseResult = parseResult;
    }
}
