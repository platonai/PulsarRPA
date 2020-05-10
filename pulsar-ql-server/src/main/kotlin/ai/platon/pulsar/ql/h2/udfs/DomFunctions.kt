package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.features.NodeFeature
import ai.platon.pulsar.dom.features.defined.*
import ai.platon.pulsar.dom.nodes.A_LABELS
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.ql.SQLContext
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.types.ValueDom
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
    private val sqlContext = SQLContext.getOrCreate()

    @UDFunction(description = "Load the page specified by url from db, if absent or expired, " +
            "fetch it from the web, and then parse it into a document")
    @JvmStatic
    fun load(@H2Context h2session: Session, configuredUrl: String): ValueDom {
        return sqlContext.getSession(h2session).run {
            parseValueDom(load(configuredUrl))
        }
    }

    @UDFunction(description = "Fetch the page specified by url immediately, and then parse it into a document")
    @JvmStatic
    fun fetch(@H2Context h2session: Session, configuredUrl: String): ValueDom {
        val urlAndArgs = Urls.splitUrlArgs(configuredUrl)
        val options = LoadOptions.parse(urlAndArgs.second).apply { expires = Duration.ZERO }

        return sqlContext.getSession(h2session.serialId).run {
            parseValueDom(load(urlAndArgs.first, options))
        }
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
    fun siblingSize(dom: ValueDom): Int {
        return dom.element.siblingSize()
    }

    @UDFunction
    @JvmStatic
    fun siblingIndex(dom: ValueDom): Int {
        return dom.element.siblingIndex()
    }

    @UDFunction
    @JvmStatic
    fun elementSiblingIndex(dom: ValueDom): Int {
        return dom.element.elementSiblingIndex()
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
    fun location(dom: ValueDom): String {
        return dom.element.location
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

    @UDFunction(description = "Get the element title")
    @JvmStatic
    fun title(dom: ValueDom): String {
        return dom.element.attr("title")
    }

    @UDFunction(description = "Get the document title")
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

    @UDFunction(description = "Extract the first group of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re1(dom: ValueDom, regex: String): String {
        val text = text(dom)
        return RegexExtractor().re1(text, regex)
    }

    @UDFunction(description = "Extract the nth group of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re1(dom: ValueDom, regex: String, group: Int): String {
        val text = text(dom)
        return ai.platon.pulsar.common.RegexExtractor().re1(text, regex, group)
    }

    @UDFunction(description = "Extract two groups of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re2(dom: ValueDom, regex: String): ValueArray {
        val text = text(dom)
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction(description = "Extract two groups(key and value) of the result of java.util.regex.matcher() over the node text")
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

    @UDFunction(description = "Get the area of the css box of a DOM, area = width * height")
    @JvmStatic
    fun area(dom: ValueDom): Double {
        return width(dom) * height(dom)
    }

    @UDFunction(description = "Get the aspect ratio of the DOM, aspect ratio = width / height")
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
