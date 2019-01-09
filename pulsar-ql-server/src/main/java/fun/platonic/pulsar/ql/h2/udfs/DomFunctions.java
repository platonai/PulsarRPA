package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.ql.types.ValueDom;
import fun.platonic.pulsar.common.RegexExtractor;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.options.LoadOptions;
import fun.platonic.pulsar.persist.WebPage;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueString;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Created by vincent on 17-11-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@SuppressWarnings("unused")
@UDFGroup(namespace = "DOM")
public class DomFunctions {

    private static final QueryEngine engine = QueryEngine.getInstance();

    /**
     * Load the given url from db, if absent, fetch it from the web, and then parse it
     *
     * @return The dom
     */
    @Nonnull
    @UDFunction
    public static Value load(@H2Context Session h2session, String configuredUrl) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        WebPage page = session.load(configuredUrl);
        return ValueDom.get(session.parse(page));
    }

    /**
     * Fetch the given url immediately without cache, and then parse it
     *
     * @return The dom
     */
    @Nonnull
    @UDFunction
    public static ValueDom fetch(@H2Context Session h2session, String configuredUrl) {
        QuerySession session = engine.getSession(new DbSession(h2session));

        Pair<String, String> urlAndArgs = UrlUtil.splitUrlArgs(configuredUrl);
        LoadOptions loadOptions = LoadOptions.parse(urlAndArgs.getValue());
        loadOptions.setExpires(Duration.ZERO);

        WebPage page = session.load(urlAndArgs.getKey(), loadOptions);
        return ValueDom.get(session.parse(page));
    }

    /**
     * Load the given url from db, if absent, fetch it from the web, and then parse it
     *
     * @return The dom
     */
    @Nonnull
    @UDFunction
    public static ValueDom parse(@H2Context Session h2session, String url) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        WebPage page = session.getPulsar().get(url);
        if (page != null && !page.isInternal() && page.getProtocolStatus().isSuccess()) {
            return ValueDom.get(session.parse(page));
        }

        return ValueDom.NIL;
    }

    /**
     * Check if this is a nil DOM
     */
    @UDFunction
    public static boolean isNil(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.isNil();
    }

    /**
     * Check if this is a not nil DOM
     */
    @UDFunction
    public static boolean isNotNil(ValueDom dom) {
        Objects.requireNonNull(dom);
        return !dom.isNil();
    }

    /**
     * Get the value of the given attribute
     */
    @UDFunction
    public static String attr(ValueDom dom, String attrName) {
        Objects.requireNonNull(dom);
        return dom.getElement().attr(attrName);
    }

    @UDFunction
    public static boolean hasAttr(ValueDom dom, String attrName) {
        Objects.requireNonNull(dom);
        return dom.getElement().hasAttr(attrName);
    }

    @UDFunction
    public static String cssSelector(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().cssSelector();
    }

    @UDFunction
    public static String cssPath(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().cssSelector();
    }

    @UDFunction
    public static int siblingSize(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().siblingSize();
    }

    @UDFunction
    public static String baseUri(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().baseUri();
    }

    @UDFunction
    public static String absUrl(ValueDom dom, String attributeKey) {
        Objects.requireNonNull(dom);
        return dom.getElement().absUrl(attributeKey);
    }

    @UDFunction
    public static int childNodeSize(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().childNodeSize();
    }

    @UDFunction
    public static String tagName(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().tagName();
    }

    @UDFunction
    public static String href(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().attr("href");
    }

    @UDFunction
    public static String absHref(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().absUrl("href");
    }

    @UDFunction
    public static String src(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().attr("src");
    }

    @UDFunction
    public static String absSrc(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().absUrl("abs:src");
    }

    @UDFunction
    public static String title(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().attr("title");
    }

    @UDFunction
    public static String docTitle(ValueDom dom) {
        Objects.requireNonNull(dom);
        Element ele = dom.getElement();
        if (ele instanceof Document) {
            return ((Document) ele).title();
        }

        return dom.getElement().ownerDocument().title();
    }

    @UDFunction
    public static boolean hasText(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().hasText();
    }

    @UDFunction
    public static String text(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().text();
    }

    @UDFunction
    public static int textLen(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().text().length();
    }

    @UDFunction
    public static String ownText(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().ownText();
    }

    @UDFunction
    public static int ownTextLen(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().ownText().length();
    }

    @UDFunction
    public static String re1(ValueDom dom, String regex) {
        String text = text(dom);
        return new RegexExtractor().re1(text, regex);
    }

    @UDFunction
    public static String re1(ValueDom dom, String regex, int group) {
        String text = text(dom);
        return new RegexExtractor().re1(text, regex, group);
    }

    @UDFunction
    public static ValueArray re2(ValueDom dom, String regex) {
        String text = text(dom);
        Pair<String, String> result = new RegexExtractor().re2(text, regex);
        Value[] array = {ValueString.get(result.getKey()), ValueString.get(result.getValue())};
        return ValueArray.get(array);
    }

    @UDFunction
    public static ValueArray re2(ValueDom dom, String regex, int keyGroup, int valueGroup) {
        String text = text(dom);
        Pair<String, String> result = new RegexExtractor().re2(text, regex, keyGroup, valueGroup);
        Value[] array = {ValueString.get(result.getKey()), ValueString.get(result.getValue())};
        return ValueArray.get(array);
    }

    @UDFunction
    public static String data(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().data();
    }

    @UDFunction
    public static String id(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().id();
    }

    @UDFunction
    public static String className(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().className();
    }

    @UDFunction
    public static Set<String> classNames(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().classNames();
    }

    @UDFunction
    public static boolean hasClass(ValueDom dom, String className) {
        Objects.requireNonNull(dom);
        return dom.getElement().hasClass(className);
    }

    @UDFunction
    public static String val(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().val();
    }

    @UDFunction
    @Nonnull
    public static ValueDom parent(ValueDom dom) {
        Objects.requireNonNull(dom);
        return ValueDom.get(dom.getElement().parent());
    }

    @UDFunction
    @Nonnull
    public static ValueDom dom(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom;
    }

    @UDFunction
    public static String html(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().html();
    }

    @UDFunction
    public static String outerHtml(ValueDom dom) {
        Objects.requireNonNull(dom);
        return dom.getElement().outerHtml();
    }

    @UDFunction
    @Nonnull
    public static ValueArray links(ValueDom dom) {
        Objects.requireNonNull(dom);

        Elements elements = dom.getElement().getElementsByTag("a");
        return toValueArray(elements);
    }

    private static double getFeature(ValueDom dom, int key) {
        Objects.requireNonNull(dom);
        return dom.getElement().getFeatures().getEntry(key);
    }

    private static ValueArray toValueArray(Elements elements) {
        ValueDom[] values = new ValueDom[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            values[i] = ValueDom.get(elements.get(i));
        }
        return ValueArray.get(values);
    }
}
