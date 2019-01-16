package `fun`.platonic.pulsar.ql.h2.udfs

import `fun`.platonic.pulsar.dom.features.FeatureFormatter.FEATURE_NAMES
import `fun`.platonic.pulsar.dom.features.FeatureFormatter.isFloating
import `fun`.platonic.pulsar.dom.features.SIB
import `fun`.platonic.pulsar.dom.nodes.node.ext.first
import `fun`.platonic.pulsar.dom.nodes.node.ext.getFeature
import `fun`.platonic.pulsar.dom.nodes.node.ext.select2
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.annotation.UDFunction
import `fun`.platonic.pulsar.ql.h2.H2QueryEngine
import `fun`.platonic.pulsar.ql.h2.Queries
import `fun`.platonic.pulsar.ql.h2.Queries.toResultSet
import `fun`.platonic.pulsar.ql.h2.domValue
import `fun`.platonic.pulsar.ql.types.ValueDom
import org.apache.hadoop.classification.InterfaceStability
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.sql.ResultSet
import java.util.*

@Suppress("unused")
@UDFGroup(namespace = "DOM")
object DomFunctionTables {

    @JvmStatic
    @UDFunction
    fun help(): ResultSet {
        val rs = SimpleResultSet()
        rs.addColumn("FUNCTION_NAME", DataType.convertTypeToSQLType(Value.STRING), 0, 0)
        rs.addColumn("PARAMETERS", DataType.convertTypeToSQLType(Value.STRING), 0, 0)
        rs.addColumn("DESCRIPTION", DataType.convertTypeToSQLType(ValueDom.type), 0, 0)

        rs.addRow("HELP", "", "print help message")

        return rs
    }

    /**
     * Load all urls
     * For example:
     * CALL loadAll(ARRAY('http://...1', 'http://...2', 'http://...3'));
     */
    @InterfaceStability.Evolving
    @JvmStatic
    @UDFunction(hasShortcut = true)
    fun loadAll(@H2Context h2session: Session, configuredUrls: ValueArray): ResultSet {
        val session = H2QueryEngine.getSession(h2session)

        val pages = Queries.loadAll(session, configuredUrls)
        val doms = pages.map { session.parseToValue(it) }

        return toResultSet("DOM", doms)
    }

    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmStatic
    @JvmOverloads
    fun loadAndSelect(
            @H2Context h2session: Session, configuredPortal: Value, cssQuery: String, offset: Int = 1, limit: Int = Integer.MAX_VALUE): ResultSet {
        val session = H2QueryEngine.getSession(h2session)
        val elements = Queries.loadAll(session, configuredPortal, cssQuery, offset, limit, Queries::select)
        return toResultSet("DOM", elements.map { ValueDom.get(it) })
    }

    @InterfaceStability.Stable
    @JvmStatic
    @UDFunction
    fun select(dom: ValueDom, cssQuery: String): ResultSet {
        Objects.requireNonNull(dom)
        return select(dom, cssQuery, 0, Integer.MAX_VALUE)
    }

    @InterfaceStability.Stable
    @UDFunction
    @JvmStatic
    fun select(dom: ValueDom, cssQuery: String, offset: Int, limit: Int): ResultSet {
        val elements = dom.element.select2(cssQuery, offset, limit)
        return toResultSet("DOM", elements.map { ValueDom.get(it) })
    }

    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmStatic
    @JvmOverloads
    fun loadAndGetLinks(
            @H2Context h2session: Session,
            configuredPortal: Value, restrictCss: String = ":root", offset: Int = 1, limit: Int = Integer.MAX_VALUE): ResultSet {
        val session = H2QueryEngine.getSession(h2session)
        val links = Queries.loadAll(session, configuredPortal, restrictCss, offset, limit, Queries::getLinks)
        return toResultSet("LINK", links)
    }

    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmOverloads
    @JvmStatic
    fun links(dom: ValueDom, restrictCss: String = ":root", offset: Int = 1, limit: Int = Integer.MAX_VALUE): ResultSet {
        return toResultSet("LINK", Queries.getLinks(dom.element, restrictCss, offset, limit))
    }

    /**
     * Load configuredPortal page(s) and follow all links
     *
     * @param configuredPortal The configuredPortal page(s) to fetch,
     * a configuredPortal can be
     * 1) a configured urls, or
     * 2) a String[]
     * 3) a List
     * @return The [ResultSet]
     */
    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmOverloads
    @JvmStatic
    fun loadOutPages(@H2Context h2session: Session,
                     configuredPortal: Value,
                     restrictCss: String = ":root",
                     offset: Int = 1,
                     limit: Int = Integer.MAX_VALUE,
                     normalize: Boolean = true): ResultSet {
        return loadOutPagesAsRsInternal(h2session, configuredPortal, restrictCss, offset, limit, normalize = normalize)
    }

    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmOverloads
    @JvmStatic
    fun loadOutPagesIgnoreUrlQuery(@H2Context h2session: Session,
                                   configuredPortal: Value,
                                   restrictCss: String = ":root",
                                   offset: Int = 1,
                                   limit: Int = Int.MAX_VALUE,
                                   normalize: Boolean = true): ResultSet {
        return loadOutPagesAsRsInternal(h2session,
                configuredPortal, restrictCss, offset, limit, normalize = normalize, ignoreQuery = true)
    }

    /**
     * Load configuredPortal page(s) and follow all links
     *
     * @param configuredPortal The configuredPortal page(s) to fetch,
     * a configuredPortal can be
     * 1) a configured urls, or
     * 2) a String[]
     * 3) a List
     * @return The [ResultSet]
     */
    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmOverloads
    @JvmStatic
    fun loadOutPagesAndSelectFirst(
            @H2Context h2session: Session,
            configuredPortal: Value,
            restrictCss: String = ":root",
            offset: Int = 1,
            limit: Int = Integer.MAX_VALUE,
            targetCss: String = ":root",
            normalize: Boolean = true): ResultSet {
        return loadOutPagesAsRsInternal(h2session,
                configuredPortal, restrictCss, offset, limit, targetCss = targetCss, normalize = normalize)
    }

    private fun loadOutPagesAsRsInternal(
            @H2Context h2session: Session,
            configuredPortal: Value,
            restrictCss: String = ":root",
            offset: Int = 1,
            limit: Int = Int.MAX_VALUE,
            targetCss: String = ":root",
            normalize: Boolean = true,
            ignoreQuery: Boolean = false): ResultSet {
        val session = H2QueryEngine.getSession(h2session)

        val docs =
                Queries.loadOutPages(session, configuredPortal, restrictCss, offset, limit, normalize, ignoreQuery)
                .map { session.parse(it) }

        val elements = if (targetCss == ":root") {
            docs.map { it.document }
        } else {
            docs.map { it.first(targetCss) }
        }

        return toResultSet("DOM", elements.filterNotNull().map { domValue(it) })
    }

    /**
     * Get the feature
     *
     * @param configuredUrl The configured url
     * @return The [ResultSet] with element features for all match elements
     */
    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmStatic
    @JvmOverloads
    fun loadAndGetFeatures(
            @H2Context h2session: Session,
            configuredUrl: String,
            cssQuery: String = "DIV,P,UL,OL,LI,DL,DT,DD,TABLE,TR,TD,H1,H2,H3",
            offset: Int = 1,
            limit: Int = Int.MAX_VALUE): ResultSet {
        val session = H2QueryEngine.getSession(h2session)
        val page = session.load(configuredUrl)
        val dom = if (page.isNil) ValueDom.NIL else session.parseToValue(page)
        return features(h2session, dom, cssQuery, offset, limit)
    }

    /**
     * @param dom         Element to parse
     * @param cssSelector css selector
     * @param offset      offset of element set, start at 1
     * @param limit       limit of element set
     * @return The [ResultSet] with element features for all match elements
     */
    @InterfaceStability.Stable
    @UDFunction
    @JvmOverloads
    @JvmStatic
    fun features(@H2Context h2session: Session,
                 dom: ValueDom,
                 cssSelector: String = "DIV,P,UL,OL,LI,DL,DT,DD,TABLE,TR,TD",
                 offset: Int = 1,
                 limit: Int = Integer.MAX_VALUE): ResultSet {
        val rs = createFeatureResultSet()

        if (dom.isNil) {
            return rs
        }

        /**
         * Notice: be careful use rs.addRow(*it) to make sure a vararg is passed into rs.addRow
         */
        dom.element.select2(cssSelector, offset, limit)
                .map { Queries.getFeatureRow(it) }
                .forEach { rs.addRow(*it) }

        return rs
    }

    /**
     * Get the feature
     *
     * @param configuredUrl The configured url
     * @return The [ResultSet] with element features for all match elements
     */
    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmOverloads
    @JvmStatic
    fun loadAndGetElementsWithMostSibling(
            @H2Context h2session: Session,
            configuredUrl: String,
            restrictCss: String = "DIV,P,UL,OL,LI,DL,DT,DD,TABLE,TR,TD",
            offset: Int = 1,
            limit: Int = Integer.MAX_VALUE): ResultSet {
        val session = H2QueryEngine.getSession(h2session)
        val page = session.load(configuredUrl)
        val dom = if (page.isNil) ValueDom.NIL else session.parseToValue(page)
        return getElementsWithMostSibling(h2session, dom, restrictCss, offset, limit)
    }

    /**
     * @param dom         Element to parse
     * @param restrictCss css selector
     * @param offset      offset of element set, start at 1
     * @param limit       limit of element set
     * @return The [ResultSet] with element features for all match elements
     */
    @InterfaceStability.Stable
    @UDFunction(hasShortcut = true)
    @JvmOverloads
    @JvmStatic
    fun getElementsWithMostSibling(
            @H2Context h2session: Session,
            dom: ValueDom,
            restrictCss: String = "DIV,P,UL,OL,LI,DL,DT,DD,TABLE,TR,TD",
            offset: Int = 1,
            limit: Int = Integer.MAX_VALUE): ResultSet {
        val rs = createFeatureResultSet()

        if (dom.isNil) {
            return rs
        }

        val elements = dom.element.select2(restrictCss)

        // traversal on each of the selected elements, add all descendants to result
        val result = Elements()
        elements.traverse { node, _ ->
            if (node is Element) {
                result.add(node)
            }
        }

        var i = 1
        result.sortedByDescending { it.getFeature(SIB) }
                .takeWhile { ++i > offset && i <= limit }
                .map { Queries.getFeatureRow(it) }
                .forEach { rs.addRow(it) }

        return rs
    }

    private fun createFeatureResultSet(): SimpleResultSet {
        val rs = SimpleResultSet()
        rs.autoClose = false

        rs.addColumn("DOM", DataType.convertTypeToSQLType(ValueDom.type), 0, 0)
        for (name in FEATURE_NAMES) {
            if (isFloating(name)) {
                val type = DataType.convertTypeToSQLType(Value.DOUBLE)
                rs.addColumn(name.toUpperCase(), type, 0, 0)
            } else {
                val type = DataType.convertTypeToSQLType(Value.INT)
                rs.addColumn(name.toUpperCase(), type, 0, 0)
            }
        }

        return rs
    }
}
