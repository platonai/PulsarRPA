package `fun`.platonic.pulsar.ql.h2.udfs

import `fun`.platonic.pulsar.common.RegexExtractor
import `fun`.platonic.pulsar.common.UrlUtil
import `fun`.platonic.pulsar.common.options.LoadOptions
import `fun`.platonic.pulsar.dom.features.*
import `fun`.platonic.pulsar.dom.features.defined.*
import `fun`.platonic.pulsar.dom.nodes.A_LABELS
import `fun`.platonic.pulsar.dom.nodes.node.ext.*
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.annotation.UDFunction
import `fun`.platonic.pulsar.ql.h2.H2QueryEngine
import `fun`.platonic.pulsar.ql.types.ValueDom
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueString
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.time.Duration

/**
 * Created by vincent on 17-11-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@Suppress("unused")
@UDFGroup(namespace = "DOM")
object DomFunctions {
    /**
     * Load the given url from db, if absent, fetch it from the web, and then parse it
     *
     * @return The dom
     */
    @UDFunction
    @JvmStatic
    fun load(@H2Context h2session: Session, configuredUrl: String): ValueDom {
        val session = H2QueryEngine.getSession(h2session)
        val page = session.load(configuredUrl)
        return session.parseToValue(page)
    }

    /**
     * Fetch the given url immediately without cache, and then parse it
     *
     * @return The dom
     */
    @UDFunction
    @JvmStatic
    fun fetch(@H2Context h2session: Session, configuredUrl: String): ValueDom {
        val session = H2QueryEngine.getSession(h2session)

        val urlAndArgs = UrlUtil.splitUrlArgs(configuredUrl)
        val loadOptions = LoadOptions.parse(urlAndArgs.value)
        loadOptions.expires = Duration.ZERO

        val page = session.load(urlAndArgs.key, loadOptions)
        return session.parseToValue(page)
    }

    /**
     * Load the given url from db, if absent, fetch it from the web, and then parse it
     *
     * @return The dom
     */
    @UDFunction
    @JvmStatic
    fun parse(@H2Context h2session: Session, url: String): ValueDom {
        val session = H2QueryEngine.getSession(h2session)
        val page = session.load(url)
        if (!page.isInternal && page.protocolStatus.isSuccess) {
            return session.parseToValue(page)
        }

        return ValueDom.NIL;
    }

    /**
     * Check if this is a nil DOM
     */
    @UDFunction
    @JvmStatic
    fun isNil(dom: ValueDom): Boolean {
        return dom.isNil
    }

    /**
     * Check if this is a not nil DOM
     */
    @UDFunction
    @JvmStatic
    fun isNotNil(dom: ValueDom): Boolean {
        return dom.isNotNil
    }

    /**
     * Get the value of the given attribute
     */
    @UDFunction
    @JvmStatic
    fun attr(dom: ValueDom, attrName: String): String {
        return dom.element.attr(attrName)
    }

    /**
     * Get the value of the given attribute
     */
    @UDFunction
    @JvmStatic
    fun labels(dom: ValueDom): String {
        return dom.element.attr(A_LABELS)
    }

    /**
     * Get the value of the given indicator
     */
    @UDFunction
    @JvmStatic
    fun feature(dom: ValueDom, featureName: String): Double {
        return NodeFeature.getValue(featureName, dom.element)
    }

    @UDFunction
    @JvmStatic
    fun hasAttr(dom: ValueDom, attrName: String): Boolean {
        return dom.element.hasAttr(attrName)
    }

    @UDFunction
    @JvmStatic
    fun style(dom: ValueDom, styleName: String): String {
        return dom.element.getStyle(styleName)
    }

    @UDFunction
    @JvmStatic
    fun sequence(dom: ValueDom): Int {
        return dom.element.sequence
    }

    @UDFunction
    @JvmStatic
    fun depth(dom: ValueDom): Int {
        return dom.element.depth
    }

    @UDFunction
    @JvmStatic
    fun cssSelector(dom: ValueDom): String {
        return dom.element.cssSelector()
    }

    @UDFunction
    @JvmStatic
    fun cssPath(dom: ValueDom): String {
        return dom.element.cssSelector()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun symbolPath(dom: ValueDom, numParts: Int = Int.MAX_VALUE): String {
        val path = dom.element.cssSelector()

        if (numParts != Int.MAX_VALUE) {
            return path.split(">").takeLast(numParts).map { it.trim() }.joinToString(" > ")
        }

        return path
    }

    @UDFunction
    @JvmStatic
    fun siblingSize(dom: ValueDom): Int {
        return dom.element.siblingSize()
    }

    @UDFunction
    @JvmStatic
    fun baseUri(dom: ValueDom): String {
        return dom.element.baseUri()
    }

    @UDFunction
    @JvmStatic
    fun absUrl(dom: ValueDom, attributeKey: String): String {
        return dom.element.absUrl(attributeKey)
    }

    @UDFunction
    @JvmStatic
    fun childNodeSize(dom: ValueDom): Int {
        return dom.element.childNodeSize()
    }

    @UDFunction
    @JvmStatic
    fun tagName(dom: ValueDom): String {
        return dom.element.tagName()
    }

    @UDFunction
    @JvmStatic
    fun href(dom: ValueDom): String {
        return dom.element.attr("href")
    }

    @UDFunction
    @JvmStatic
    fun absHref(dom: ValueDom): String {
        return dom.element.absUrl("href")
    }

    @UDFunction
    @JvmStatic
    fun src(dom: ValueDom): String {
        return dom.element.attr("src")
    }

    @UDFunction
    @JvmStatic
    fun absSrc(dom: ValueDom): String {
        return dom.element.absUrl("abs:src")
    }

    @UDFunction
    @JvmStatic
    fun title(dom: ValueDom): String {
        return dom.element.attr("title")
    }

    @UDFunction
    @JvmStatic
    fun docTitle(dom: ValueDom): String {
        val ele = dom.element
        if (ele is Document) {
            return ele.title()
        }

        return dom.element.ownerDocument().title()
    }

    @UDFunction
    @JvmStatic
    fun hasText(dom: ValueDom): Boolean {
        return dom.element.hasText()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun text(dom: ValueDom, truncate: Int = Int.MAX_VALUE): String {
        val text = dom.element.text()!!
        return if (truncate > text.length) {
            text
        } else {
            text.substring(0, truncate)
        }
    }

    @UDFunction
    @JvmStatic
    fun textLen(dom: ValueDom): Int {
        return dom.element.text().length
    }

    @UDFunction
    @JvmStatic
    fun textLength(dom: ValueDom): Int {
        return dom.element.text().length
    }

    @UDFunction
    @JvmStatic
    fun ownText(dom: ValueDom): String {
        return dom.element.ownText()
    }

    @UDFunction
    @JvmStatic
    fun ownTextLen(dom: ValueDom): Int {
        return dom.element.ownText().length
    }

    @UDFunction
    @JvmStatic
    fun re1(dom: ValueDom, regex: String): String {
        val text = text(dom)
        return RegexExtractor().re1(text, regex)
    }

    @UDFunction
    @JvmStatic
    fun re1(dom: ValueDom, regex: String, group: Int): String {
        val text = text(dom)
        return RegexExtractor().re1(text, regex, group)
    }

    @UDFunction
    @JvmStatic
    fun re2(dom: ValueDom, regex: String): ValueArray {
        val text = text(dom)
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun re2(dom: ValueDom, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val text = text(dom)
        val result = RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun data(dom: ValueDom): String {
        return dom.element.data()
    }

    @UDFunction
    @JvmStatic
    fun id(dom: ValueDom): String {
        return dom.element.id()
    }

    @UDFunction
    @JvmStatic
    fun className(dom: ValueDom): String {
        return dom.element.className()
    }

    @UDFunction
    @JvmStatic
    fun classNames(dom: ValueDom): Set<String> {
        return dom.element.classNames()
    }

    @UDFunction
    @JvmStatic
    fun hasClass(dom: ValueDom, className: String): Boolean {
        return dom.element.hasClass(className)
    }

    @UDFunction
    @JvmStatic
    fun value(dom: ValueDom): String {
        return dom.element.`val`()
    }

    @UDFunction
    @JvmStatic
    fun parent(dom: ValueDom): ValueDom {
        return ValueDom.get(dom.element.parent())
    }

    @UDFunction
    @JvmStatic
    fun parentName(dom: ValueDom): String {
        return dom.element.parent().uniqueName
    }

    @UDFunction
    @JvmStatic
    fun dom(dom: ValueDom): ValueDom {
        return dom
    }

    @UDFunction
    @JvmStatic
    fun html(dom: ValueDom): String {
        return dom.element.html()
    }

    @UDFunction
    @JvmStatic
    fun outerHtml(dom: ValueDom): String {
        return dom.element.outerHtml()
    }

    @UDFunction
    @JvmStatic
    fun uniqueName(dom: ValueDom): String {
        return dom.element.uniqueName
    }

    @UDFunction
    @JvmStatic
    fun links(dom: ValueDom): ValueArray {
        val elements = dom.element.getElementsByTag("a")
        return toValueArray(elements)
    }

    @UDFunction
    @JvmStatic
    fun ch(dom: ValueDom): Double {
        return getFeature(dom, CH)
    }

    @UDFunction
    @JvmStatic
    fun tn(dom: ValueDom): Double {
        return getFeature(dom, TN)
    }

    @UDFunction
    @JvmStatic
    fun img(dom: ValueDom): Double {
        return getFeature(dom, IMG)
    }

    @UDFunction
    @JvmStatic
    fun a(dom: ValueDom): Double {
        return getFeature(dom, A)
    }

    @UDFunction
    @JvmStatic
    fun sib(dom: ValueDom): Double {
        return getFeature(dom, SIB)
    }

    @UDFunction
    @JvmStatic
    fun c(dom: ValueDom): Double {
        return getFeature(dom, C)
    }

    @UDFunction
    @JvmStatic
    fun dep(dom: ValueDom): Double {
        return getFeature(dom, DEP)
    }

    @UDFunction
    @JvmStatic
    fun seq(dom: ValueDom): Double {
        return getFeature(dom, SEQ)
    }

    @UDFunction
    @JvmStatic
    fun top(dom: ValueDom): Double {
        return getFeature(dom, TOP)
    }

    @UDFunction
    @JvmStatic
    fun left(dom: ValueDom): Double {
        return getFeature(dom, LEFT)
    }

    @UDFunction
    @JvmStatic
    fun width(dom: ValueDom): Double {
        return getFeature(dom, WIDTH)
    }

    @UDFunction
    @JvmStatic
    fun height(dom: ValueDom): Double {
        return getFeature(dom, HEIGHT)
    }

    /**
     * Get the area of the css box of a DOM, area = width * height
     */
    @UDFunction
    @JvmStatic
    fun area(dom: ValueDom): Double {
        return width(dom) * height(dom)
    }

    /**
     * Get the aspect ratio of the DOM, aspect ratio = width / height
     */
    @UDFunction
    @JvmStatic
    fun aspectRatio(dom: ValueDom): Double {
        return width(dom) / height(dom)
    }

    private fun getFeature(dom: ValueDom, key: Int): Double {
        return dom.element.getFeature(key)
    }

    private fun toValueArray(elements: Elements): ValueArray {
        val values = arrayOf<Value>()
        for (i in 0 until elements.size) {
            values[i] = ValueDom.get(elements[i])
        }
        return ValueArray.get(values)
    }
}
