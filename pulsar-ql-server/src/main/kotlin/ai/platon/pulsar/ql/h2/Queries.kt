package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.common.math.vectors.isEmpty
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.features.NodeFeature.Companion.isFloating
import ai.platon.pulsar.dom.features.NodeFeature.Companion.registeredFeatures
import ai.platon.pulsar.dom.nodes.Anchor
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.dom.select.select
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
import kotlin.math.roundToInt
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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
        var pages: Collection<WebPage> = listOf()

        when (configuredUrls) {
            is ValueString -> {
                pages = ArrayList()
                pages.add(session.load(configuredUrls.getString()))
            }
            is ValueArray ->
                for (configuredUrl in configuredUrls.list) {
                    pages = session.loadAll(configuredUrls.list.map { configuredUrl.string })
                }
            else -> throw DbException.get(ErrorCode.FUNCTION_NOT_FOUND_1, "Unknown custom type")
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
     * @return A collection of O
     */
    @InterfaceStability.Evolving
    fun <O> loadAll(session: QuerySession,
                    configuredUrls: Value, restrictCss: String, offset: Int, limit: Int,
                    transformer: (Element, String, Int, Int) -> Collection<O>): Collection<O> {
        val collection: Collection<O>

        when (configuredUrls) {
            is ValueString -> {
                val doc = session.loadAndParse(configuredUrls.getString())
                collection = transformer(doc.document, restrictCss, offset, limit)
            }
            is ValueArray -> {
                collection = ArrayList()
                for (configuredUrl in configuredUrls.list) {
                    val doc = session.loadAndParse(configuredUrl.string)
                    collection.addAll(transformer(doc.document, restrictCss, offset, limit))
                }
            }
            else -> throw DbException.get(ErrorCode.FUNCTION_NOT_FOUND_1, "Unknown custom type")
        }

        return collection
    }

    fun loadOutPages(
            session: QuerySession,
            portalUrl: String, restrictCss: String,
            offset: Int, limit: Int,
            normalize: Boolean, ignoreQuery: Boolean): Collection<WebPage> {
        val transformer = if (ignoreQuery) this::getLinksIgnoreQuery else this::getLinks

        val document = session.parse(session.load(portalUrl)).document
        var links = transformer(document, restrictCss, offset, limit)

        if (normalize) {
            links = links.mapNotNull { session.normalize(it).takeIf { it.isValid }?.url }
        }

        return session.loadAll(links, LoadOptions.create())
    }

    fun loadOutPages(
            session: QuerySession,
            configuredPortal: Value, restrictCss: String,
            offset: Int, limit: Int,
            normalize: Boolean, ignoreQuery: Boolean): Collection<WebPage> {
        val transformer = if (ignoreQuery) this::getLinksIgnoreQuery else this::getLinks
        var links = loadAll(session, configuredPortal).map { session.parse(it).document }
                .flatMap { transformer(it, restrictCss, offset, limit) }

        if (normalize) {
            links = links.mapNotNull { session.normalize(it).takeIf { it.isValid }?.url }
        }

        return session.loadAll(links, LoadOptions.create())
    }

    /**
     * TODO: any type support
     * */
    fun <O> select(dom: ValueDom, cssQuery: String, transform: (Element) -> O): ValueArray {
        val values = dom.element.select(cssQuery) { ValueString.get(transform(it).toString()) }
                .toTypedArray()

        return ValueArray.get(values)
    }

    fun <O> selectFirst(dom: ValueDom, cssQuery: String, transformer: (Element) -> O): O? {
        return dom.element.first(cssQuery, transformer)
    }

    fun <O> selectNth(dom: ValueDom, cssQuery: String, n: Int, transform: (Element) -> O): O? {
        return dom.element.select(cssQuery, n, 1) { transform(it) }.firstOrNull()
    }

    fun getTexts(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        return ele.select(restrictCss, offset, limit) { it.text() }
    }

    fun getLinks(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        val cssQuery = appendSelectorIfMissing(restrictCss, "a")
        return ele.select(cssQuery, offset, limit) {
            it.absUrl("href")
        }.filterNotNull()
    }

    fun getLinksIgnoreQuery(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        val cssQuery = appendSelectorIfMissing(restrictCss, "a")
        return ele.select(cssQuery, offset, limit) {
            it.absUrl("href").takeIf { Urls.isValidUrl(it) }?.substringBefore("?")
        }.filterNotNull()
    }

    fun getAnchors(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<Anchor> {
        val cssQuery = appendSelectorIfMissing(restrictCss, "a")
        return ele.select(cssQuery, offset, limit).mapNotNull {
            it.takeIf { Urls.isValidUrl(it.absUrl("href")) }
                    ?.let { Anchor(it.absUrl("href"), it.cleanText, it.cssSelector(),
                            it.left, it.top, it.width, it.height) }
        }
    }

    fun getImages(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<String> {
        val cssQuery = appendSelectorIfMissing(restrictCss, "img")
        return ele.select(cssQuery, offset, limit) {
            it.absUrl("src").takeIf { Urls.isValidUrl(it) }
        }.filterNotNull()
    }

    fun getFeatures(ele: Element, restrictCss: String, offset: Int, limit: Int): Collection<RealVector> {
        return ele.select(restrictCss, offset, limit) { it.features }
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
    fun toResultSet(anchors: Collection<Anchor>): ResultSet {
        val rs = SimpleResultSet()
        rs.addColumn("URL")
        rs.addColumn("TEXT")
        rs.addColumn("PATH")
        rs.addColumn("LEFT")
        rs.addColumn("TOP")
        rs.addColumn("WIDTH")
        rs.addColumn("HEIGHT")

        anchors.forEach {
            rs.addRow(it.url, it.text, it.path, it.left, it.top, it.width, it.height)
        }

        return rs
    }

    /**
     * Get result set of a data class
     * TODO: test is required
     */
    fun toResultSet(objects: Iterable<Any>): ResultSet {
        val rs = SimpleResultSet()
        val first = objects.firstOrNull()?:return rs
        val primaryConstructor = first::class.primaryConstructor ?: return rs

        val propertyNames = primaryConstructor.parameters.mapIndexed { i, kParameter ->
            kParameter.name?:"C${1 + i}"
        }
        propertyNames.forEach {
            rs.addColumn(it.toUpperCase())
        }

        val memberProperties = first::class.memberProperties.filter { it.name in propertyNames }
        objects.forEach { obj ->
            val values = memberProperties
                    .filter { it.name in propertyNames }
                    .map { it.getter.call(obj).toString() }
                    .toTypedArray()
            rs.addRow(*values)
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
                values[j] = 1.0 * (factor * v).roundToInt() / factor
            } else {
                values[j] = v.toInt()
            }
        }

        return values
    }
}
