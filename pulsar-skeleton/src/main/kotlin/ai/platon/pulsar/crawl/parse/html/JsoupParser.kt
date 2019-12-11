package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.CollectionOptions
import ai.platon.pulsar.common.options.EntityOptions
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.isNil
import ai.platon.pulsar.persist.WebPage
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

/**
 * Created by vincent on 16-9-14.
 *
 *
 * General parser, Css selector, XPath selector, Regex and Scent selectors are supported
 */
class JsoupParser(
        private val page: WebPage,
        private val conf: ImmutableConfig
) : EntityOptions.Builder() {
    var document: Document = FeaturedDocument.NIL.document
        private set
    private val entities: MutableList<FieldCollection> = LinkedList()

    fun parse(): Document {
        if (page.encoding == null) {
            val primerParser = PrimerParser(conf)
            primerParser.detectEncoding(page)
        }

        try {
            document = Jsoup.parse(page.contentAsInputStream, page.encoding, page.url)
            return document
        } catch (e: IOException) {
            LOG.warn("Failed to parse page {}", page.url)
            LOG.warn(e.toString())
        }

        return document
    }

    /**
     * Extract all fields using EntityOptions
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun extractAll(options: EntityOptions = build()): List<FieldCollection> { // No rules
        if (!options.hasRules()) {
            return entities
        }
        val fields = extract(options)
        if (!fields.isEmpty()) {
            entities.add(fields)
            extract(options.collectionOptions)
        }
        return entities
    }

    /**
     * Parse entity
     */
    @JvmOverloads
    fun extract(options: EntityOptions = build()): FieldCollection {
        val fields = FieldCollection()
        if (document.isNil) {
            return fields
        }

        val root = query(options.root, document).first() ?: return fields
        fields.name = options.name
        options.cssRules.forEach { (key: String, value: String) -> extract(key, value, root, fields) }
        return fields
    }

    /**
     * Parse sub entity collection
     */
    fun extract(rules: CollectionOptions): List<FieldCollection> {
        if (document.isNil) {
            return listOf()
        }

        // Parse fields for sub entity collection
        val root = query(rules.root, document).first() ?: return entities
        // Parse fields for sub entity collection
        val elements = query(rules.item, root)
        for ((i, ele) in elements.withIndex()) {
            val fields = FieldCollection()
            fields.name = "sub_" + (i + 1)
            rules.cssRules.forEach { (key: String, value: String) -> extract(key, value, ele, fields) }
            entities.add(fields)
        }
        return entities
    }

    companion object {
        val LOG = LoggerFactory.getLogger(JsoupParser::class.java)
        /**
         * Apply css selector
         */
        fun extract(name: String, selector_: String, root: Element, fields: FieldCollection) {
            var selector = selector_
            var required = true
            if (selector.endsWith("?")) {
                required = false
                selector = selector.substring(0, selector.length - 1)
            }
            selector = selector.replace("%".toRegex(), " ")
            val value = getText(selector, root)
            if (required) {
                fields.increaseRequired(1)
                if (value == "") {
                    fields.loss(1)
                }
            }
            if (value != "") {
                fields[name] = value
            }
        }

        /**
         *
         */
        fun getText(cssQuery: String, root: Element): String {
            val elements = query(cssQuery, root)
            return if (elements.isEmpty()) {
                ""
            } else StringUtils.strip(elements.first().text())
        }

        /**
         * Select a element set using css selector.
         *
         * @param cssQuery The css selector
         * @param root     The root element
         * The root element to query
         * @return An element set who matches the css selector
         */
        fun query(cssQuery: String, root: Element): Elements {
            try {
                return root.select(cssQuery)
            } catch (ignored: Throwable) {}

            return Elements()
        }
    }
}