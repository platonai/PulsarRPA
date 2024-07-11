package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.boilerpipe.document.TextDocument
import ai.platon.pulsar.boilerpipe.extractors.ChineseNewsExtractor
import ai.platon.pulsar.boilerpipe.sax.SAXInput
import ai.platon.pulsar.boilerpipe.utils.BoiConstants.*
import ai.platon.pulsar.boilerpipe.utils.ProcessingException
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.dom.nodes.node.ext.location
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.annotation.H2Context
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import ai.platon.pulsar.ql.h2.DomToH2Queries
import ai.platon.pulsar.ql.h2.addColumn
import ai.platon.pulsar.ql.types.ValueDom
import org.h2.jdbc.JdbcConnection
import org.h2.tools.SimpleResultSet
import org.h2.value.DataType
import org.h2.value.Value
import org.h2.value.ValueArray
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.*

@UDFGroup(namespace = "NEWS")
object NewsFunctionTables {
    private val logger = LoggerFactory.getLogger(NewsFunctionTables::class.java)

    /**
     * Load page by url and out pages from it, extract articles from them
     *
     * @param url The portal url to fetch
     * @return The [ResultSet]
     */
    @UDFunction(
        description = "Load out pages from a portal url, and extract articles using biolerpipe algorithm"
    )
    @JvmStatic
    fun loadAndExtract(
        @H2Context conn: JdbcConnection,
        url: String,
    ): ResultSet {
        if (H2SessionFactory.isColumnRetrieval(conn)) {
            return createResultSet()
        }

        val session = H2SessionFactory.getSession(conn)
        val document = session.loadDocument(url)

        return extract(conn, ValueDom.get(document))
    }

    /**
     * Load page by url and out pages from it, extract articles from them
     *
     * @param portalUrl The portal url to fetch
     * @return The [ResultSet]
     */
    @UDFunction(
        hasShortcut = true,
        description = "Load out pages from a portal url, and extract articles using biolerpipe algorithm"
    )
    @JvmOverloads
    @JvmStatic
    fun loadOutPagesAndHarvestArticles(
        @H2Context conn: JdbcConnection,
        portalUrl: String,
        restrictCss: String = ":root",
        offset: Int = 1,
        limit: Int = Integer.MAX_VALUE,
        normalize: Boolean = false,
        ignoreQuery: Boolean = false
    ): ResultSet {
        return loadOutPagesAndExtractArticles(conn, portalUrl, restrictCss, offset, limit, normalize, ignoreQuery)
    }

    /**
     * Load page by url and out pages from it, extract articles from them
     *
     * @param portalUrl The portal url to fetch
     * @return The [ResultSet]
     */
    @UDFunction(
        hasShortcut = true,
        description = "Load out pages from a portal url, and extract articles using biolerpipe algorithm"
    )
    @JvmOverloads
    @JvmStatic
    fun loadOutPagesAndExtractArticles(
        @H2Context conn: JdbcConnection,
        portalUrl: String,
        restrictCss: String = ":root",
        offset: Int = 1,
        limit: Int = Integer.MAX_VALUE,
        normalize: Boolean = false,
        ignoreQuery: Boolean = false
    ): ResultSet {
        if (H2SessionFactory.isColumnRetrieval(conn)) {
            return createResultSet()
        }

        val ss = H2SessionFactory.getSession(conn)
        val (url, args) = UrlUtils.splitUrlArgs(portalUrl)
        ss.load(url, ss.options(args))

        val docs = DomToH2Queries.loadOutPages(ss, portalUrl, restrictCss, offset, limit, normalize, ignoreQuery)
            .asSequence()
            .map { ss.parse(it) }
        val doms = docs.map { ValueDom.get(it.document) }

        return extractAllInternal(doms)
    }

    @UDFunction
    @JvmStatic
    fun extract(
        @H2Context conn: JdbcConnection, dom: ValueDom): ResultSet {
        if (H2SessionFactory.isColumnRetrieval(conn)) {
            return createResultSet()
        }

        return extractAll(conn, ValueArray.get(arrayOf(dom)))
    }

    @UDFunction
    @JvmStatic
    fun extractAll(
        @H2Context conn: JdbcConnection, domArray: ValueArray): ResultSet {
        if (H2SessionFactory.isColumnRetrieval(conn)) {
            return createResultSet()
        }

        domArray.list.forEach { require(it is ValueDom) }
        return extractAllInternal(domArray.list.asSequence().mapNotNull { it as? ValueDom })
    }

    private fun createResultSet(): SimpleResultSet {
        val rs = ResultSets.newSimpleResultSet()

        rs.addColumn(DOC_FIELD_CONTENT_TITLE.uppercase(Locale.getDefault()))
        rs.addColumn(DOC_FIELD_PAGE_TITLE.uppercase(Locale.getDefault()))

        val timestampType = DataType.convertTypeToSQLType(Value.TIMESTAMP)

        rs.addColumn(DOC_FIELD_PUBLISH_TIME.uppercase(Locale.getDefault()), timestampType, 0, 0)
        rs.addColumn(DOC_FIELD_MODIFIED_TIME.uppercase(Locale.getDefault()), timestampType, 0, 0)

        rs.addColumn(DOC_FIELD_TEXT_CONTENT.uppercase(Locale.getDefault()))
        rs.addColumn(DOC_FIELD_PAGE_CATEGORY.uppercase(Locale.getDefault()))

        val colType = ValueDom.type
        val sqlType = DataType.convertTypeToSQLType(colType)
        rs.addColumn("DOM", sqlType, 0, 0)
        rs.addColumn("DOC", sqlType, 0, 0)

        return rs
    }

    class BoilerpipeResult(val textDocument: TextDocument, val dom: ValueDom)

    private fun extractAllInternal(doms: Sequence<ValueDom>): ResultSet {
        val extractor = ChineseNewsExtractor()
        val documents = doms
            .mapNotNull {
                try {
                    val textDocument = SAXInput().parse(it.element.location, it.element.outerHtml())
                    extractor.process(textDocument)
                    BoilerpipeResult(textDocument, it)
                } catch (e: ProcessingException) {
                    logger.warn(e.stringify())
                    null
                }
            }
        return buildResultSet(documents.asIterable())
    }

    private fun buildResultSet(results: Iterable<BoilerpipeResult>): ResultSet {
        val rs = createResultSet()

        results.forEach { result ->
            val textDocument = result.textDocument
            val elementDOM = result.dom
            val documentDOM = ValueDom.get(elementDOM.element.ownerDocument())

            try {
                rs.addRow(
                    textDocument.contentTitle,
                    textDocument.pageTitle,
                    java.sql.Timestamp.from(textDocument.publishTime),
                    java.sql.Timestamp.from(textDocument.modifiedTime),
                    textDocument.textContent,
                    textDocument.pageCategoryAsString,
                    elementDOM,
                    documentDOM
                )
            } catch (e: ProcessingException) {
                logger.warn(e.message)
            }
        }

        return rs
    }
}
