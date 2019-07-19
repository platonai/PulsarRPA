package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.config.PulsarConstants.SHORTEST_VALID_URL_LENGTH
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.common.math.vectors.isEmpty
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.features.NodeFeature.Companion.isFloating
import ai.platon.pulsar.dom.features.NodeFeature.Companion.registeredFeatures
import ai.platon.pulsar.dom.nodes.node.ext.name
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.dom.select.select2
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.WebPageFormatter
import ai.platon.pulsar.ql.QuerySession
import ai.platon.pulsar.ql.types.ValueDom
import org.apache.commons.math3.linear.RealVector
import org.apache.hadoop.classification.InterfaceStability
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueString
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.sql.ResultSet
import java.util.*
import kotlin.math.pow

object Queries {

    /**
     * Load all Web pages
     *
     * @param session        The session
     * @param configuredUrls The configured urls, can be a single string represented by a [ValueString],
     * or an array of strings represented by a [ValueArray]
     * @return A collection of [WebPage]s
     */
    @InterfaceStability.Evolving
    fun loadAll(session: QuerySession, configuredUrls: Value): Collection<WebPage> {
        val pages = ArrayList<WebPage>()

        if (configuredUrls is ValueString) {
            pages.add(session.load(configuredUrls.getString()))
        } else if (configuredUrls is ValueArray) {
            for (configuredUrl in configuredUrls.list) {
                pages.add(session.load(configuredUrl.string))
            }
        } else {
            throw DbException.get(ErrorCode.FUNCTION_NOT_FOUND_1, "Unknown custom type")
        }

        return pages
    }

    /**
     * Load all Web pages, and translate Web pages to targets using the given transformer
     *
     * @param session        The session
     * @param configuredUrls The configured urls, can be a single string represented by a {@link ValueString},
     *                       or an array of strings represented by a {@link ValueArray}
     * @param cssQuery       The css query
     * @param offset         The offset
     * @param limit          The limit
     * @param transformer    The transformer used to translate a Web page into something else
     * @return A collection of {@link WebPage}s
     */
    @InterfaceStability.Evolving
    fun <O> loadAll(session: QuerySession,
                    configuredUrls: Value, cssQuery: String, offset: Int, limit: Int,
                    transformer: (Element, String, Int, Int) -> Collection<O>) : Collection<O> {
        val collection: Collection<O>

        if (configuredUrls is ValueString) {
            val doc = loadAndParse(session, configuredUrls.getString())
            collection = transformer(doc.document, cssQuery, offset, limit)
        } else if (configuredUrls is ValueArray) {
            collection = ArrayList()
            for (configuredUrl in configuredUrls.list) {
                val doc = loadAndParse(session, configuredUrl.string)
                collection.addAll(transformer(doc.document, cssQuery, offset, limit))
            }
        } else {
            throw DbException.get(ErrorCode.FUNCTION_NOT_FOUND_1, "Unknown custom type")
        }

        return collection
    }

    fun loadOutPages(
            session: QuerySession, configuredPortal: Value, restrictCss: String): Collection<WebPage> {
        return loadOutPages(session, configuredPortal, restrictCss, 0, Int.MAX_VALUE, true, false)
    }

    fun loadOutPages(
            session: QuerySession, configuredPortal: Value, restrictCss: String, offset: Int, limit: Int): Collection<WebPage> {
        return loadOutPages(session, configuredPortal, restrictCss, offset, limit, true, false)
    }

    fun loadOutPages(
            session: QuerySession,
            configuredPortal: Value, restrictCss: String,
            offset: Int, limit: Int,
            normalize: Boolean, ignoreQuery: Boolean): Collection<WebPage> {
        var links: Collection<String>

        if (ignoreQuery) {
            links = loadAll(session, configuredPortal, restrictCss, offset, limit) {
                ele, rc, os, lt -> getLinks(ele, rc, os, lt)
            }
        } else {
            links = loadAll(session, configuredPortal, restrictCss, offset, limit) {
                ele, rc, os, lt -> getLinksIgnoreQuery(ele, rc, os, lt)
            }
        }

        if (normalize) {
            links = links.mapNotNull { session.normalize(it).takeIf { it.isValid }?.url }
        }

        return session.loadAll(links, LoadOptions.create())
    }

    fun loadAndParse(session: QuerySession, configuredUrl: String): FeaturedDocument {
        return session.parse(session.load(configuredUrl))
    }

    fun select(ele: Element, cssQuery: String, offset: Int, limit: Int): Collection<Element> {
        return ele.select2(cssQuery, offset, limit)
    }

    fun <O> select(dom: ValueDom, cssQuery: String, transform: (Element) -> O): ValueArray {
        val values = dom.element.select2(cssQuery)
                .map { transform(it) }
                .map { ValueString.get(it.toString()) }
                .toTypedArray()

        return ValueArray.get(values)
    }

    fun <O> selectFirst(dom: ValueDom, cssQuery: String, transformer: (Element) -> O): O? {
        return dom.element.first(cssQuery, transformer)
    }

    fun <O> selectNth(dom: ValueDom, cssQuery: String, n: Int, transform: (Element) -> O): O? {
        require(n > 0)
        val elements = dom.element.select2(cssQuery)
        return if (elements.size >= n) {
            transform(elements[n - 1])
        } else null
    }

    fun <O> selectIgnoreEmpty(dom: ValueDom, cssQuery: String, transform: (Element) -> O): ValueArray {
        val values = dom.element.select2(cssQuery)
                .map { transform(it).toString() }
                .filter { it.isNotEmpty() }
                .map { ValueString.get(it) }
                .toTypedArray()

        return ValueArray.get(values)
    }

    fun getTexts(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        return select(ele, restrictCss, offset, limit)
                .map { it.text() }
                .toSet()
    }

    fun getLinks(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        val css = appendIfMissingIgnoreCase(restrictCss, "a")
        var i = 1
        return select(ele, css, 1, Integer.MAX_VALUE)
                .map { e -> e.absUrl("href") }
                .takeWhile { i++ >= offset && i <= limit }
                .filterNotNull()
                .filter { it.length >= SHORTEST_VALID_URL_LENGTH }
    }

    fun getLinksIgnoreQuery(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        val css = appendIfMissingIgnoreCase(restrictCss, "a")
        return select(ele, css, offset, limit)
                .map { it.absUrl("href") }
                .filterNotNull()
                .filter { it.length >= SHORTEST_VALID_URL_LENGTH }
                .map { it.substringBefore("?") }
    }

    fun getImages(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        val cs = appendIfMissingIgnoreCase(restrictCss, "img")
        return select(ele, cs, offset, limit)
                .map { it.absUrl("src") }
                .filterNotNull()
                .filter { it.length >= SHORTEST_VALID_URL_LENGTH }
    }

    fun getFeatures(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<RealVector> {
        return select(ele, restrictCss, offset, limit).map { it.features }
    }

    fun toValueArray(elements: Elements): ValueArray {
        val values = arrayOfNulls<ValueDom>(elements.size)
        for (i in elements.indices) {
            values[i] = ValueDom.getOrNil(elements[i])
        }
        return ValueArray.get(values)
    }

    /**
     * Get a result set, the result set contains just one column DOM
     * TODO: generalization
     */
    @InterfaceStability.Evolving
    fun <E> toResultSet(colName: String, collection: Iterable<E>): ResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false
        val colType = if (colName.equals("DOM", ignoreCase = true)) ValueDom.type else Value.STRING
        rs.addColumn(colName, DataType.convertTypeToSQLType(colType), 0, 0)

        if (colType == ValueDom.type) {
            collection.forEach { rs.addRow(it) }
        } else {
            collection.forEach { e -> rs.addRow(ValueString.get(e.toString())) }
        }

        return rs
    }

    /**
     * Get result set for each field in Web page
     */
    fun toResultSet(page: WebPage): ResultSet {
        val rs = SimpleResultSet()
        rs.addColumn("KEY")
        rs.addColumn("VALUE")

        val fields = WebPageFormatter(page).toMap()
        for (entry in fields.entries) {
            val value = if (entry.value == null) null else entry.value.toString()
            rs.addRow(entry.key, value)
        }

        return rs
    }

    /**
     * Get a row of data contains the DOM itself and all it's feature values
     * Every float feature has 2 fraction digits
     */
    fun getFeatureRow(ele: Element): Array<Any?> {
        val columnCount = 1 + registeredFeatures.size + 1
        val values = arrayOfNulls<Any>(columnCount)
        values[0] = ValueDom.get(ele)
        val features = if (!ele.features.isEmpty) ele.features else return values

        // TODO: configurable
        val base = 10f
        val fractionDigits = 2
        val factor = base.pow(fractionDigits)
        for (j in 1..registeredFeatures.size) {
            val key = j - 1
            val v = features[key]

            if (isFloating(key)) {
                values[j] = 1.0 * Math.round(factor * v) / factor
            } else {
                values[j] = v.toInt()
            }
        }

        return values
    }

    fun appendIfMissingIgnoreCase(cssQuery: String, appendix: String): String {
        var cssQuery = cssQuery.toLowerCase().replace("\\s+".toRegex(), " ").trim()
        val appendix = appendix.trim()

        val parts = cssQuery.split(" ")
        if (!parts[parts.size - 1].startsWith(appendix)) {
            cssQuery += " $appendix"
        }

        return cssQuery
    }
}
