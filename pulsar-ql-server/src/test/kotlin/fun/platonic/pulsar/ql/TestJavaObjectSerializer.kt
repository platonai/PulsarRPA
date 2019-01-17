package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.common.math.vectors.get
import `fun`.platonic.pulsar.dom.FeaturedDocument
import `fun`.platonic.pulsar.ql.Db.config
import `fun`.platonic.pulsar.ql.Db.deleteDb
import `fun`.platonic.pulsar.ql.Db.generateTempDbName
import `fun`.platonic.pulsar.ql.h2.domValue
import `fun`.platonic.pulsar.ql.types.ValueDom
import org.h2.engine.SysProperties
import org.jsoup.Jsoup
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.sql.Types
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestJavaObjectSerializer : TestBase() {

//    init {
//        Db.config.traceTest = false
//        Db.config.memory = true
//        Db.config.networked = true
//    }

    private lateinit var baseDom: ValueDom

    @Before
    override fun setup() {
        super.setup()

        // val doc = Jsoup.connect("http://www.baidu.com/").get()
        val doc = FeaturedDocument.createShell("http://example.com/")
        doc.body.append("<div>Hello World</div>")
        baseDom = domValue(doc)
    }

    @Ignore("org.jsoup.nodes.Element has no proper equals method")
    @Test
    fun testElementSerialization() {
        val ele = baseDom.element
        // TODO: should we need a equals method for Element?
        assertEquals(ele, Jsoup.parse(ele.toString()))
    }

    @Test
    fun testLocalSerialization() {
        SysProperties.serializeJavaObject = true

        val serializer = PulsarObjectSerializer()

        val baseUri = "http://example.com/"
        val doc = Jsoup.parseBodyFragment("<div>Hello World</div>", baseUri)

        baseDom = ValueDom.get(doc.body().selectFirst("div"))
        val bytes = serializer.serialize(baseDom)
        val obj = serializer.deserialize(bytes)
        assertTrue(obj is ValueDom)
        val dom = obj as ValueDom

        println(dom.outHtml)

        val bytes2 = serializer.serialize(dom)
        val obj2 = serializer.deserialize(bytes2)
        assertTrue(obj2 is ValueDom)
        val dom2 = obj2 as ValueDom
        assertTrue { dom.equals(dom2) }

        // The document is loaded from cache, so the two dom object contains the same Document object
        assertFalse { obj === obj2 }
        assertTrue { dom.element.baseUri() == dom2.element.baseUri() }
        assertTrue { dom.element.ownerDocument() == dom2.element.ownerDocument() }

        // Assert DOM features are correct
        assertTrue { dom.element.features.dimension > 0 }
        assertTrue { dom.element.features[0] == dom2.element.features[1] }

//        assertEquals(":root@" + baseUri, dom2.toString())
//        assertEquals(baseUri, dom2.element.selectFirst("body").attr("baseUri"))

        // TODO: the serialization is OK, but the Document instantiation seems not symmetrical
//        assertEquals(dom, dom2)
//        assertEquals(dom.string, dom2.string)
        // assertEquals(dom.toString(), dom2.element.attr("str"))
    }

    @Test
    fun testNetworkSerialization() {
        assertTrue(stat is org.h2.jdbc.JdbcStatement)

        stat.execute("create table t(id int, val other)")

        val ins = conn.prepareStatement("insert into t(id, val) values(?, ?)")

        ins.setInt(1, 2)
        ins.setObject(2, baseDom, Types.JAVA_OBJECT)
        assertEquals(1, ins.executeUpdate())

        val s = conn.createStatement()
        // val rs = s.executeQuery("select val from t")
        val rs = s.executeQuery("select val, id from t")

        assertTrue(rs.next())
        assertTrue(rs.getObject(1) is ValueDom)
        assertTrue((rs.getObject(1) as ValueDom).element.toString().contains("Hello World"))
        // println(rs.getString(1))

        conn.close()
        deleteDb(name)
    }

    @Test
    fun testNetworkSerialization2() {
        SysProperties.serializeJavaObject = false

        val name = generateTempDbName()
        val conn = Db.getConnection(name)
        val stat = conn.createStatement()
        assertTrue(stat is org.h2.jdbc.JdbcStatement)

        val sql = """
            SELECT
              DOM,
              CHAR, TXT_ND, A,
              DOM_UNIQUE_NAME(DOM) AS NAME,
              DOM_CSS_SELECTOR(DOM) AS CSS_SELECTOR
            FROM
              LOAD_AND_GET_FEATURES('https://www.baidu.com/s?wd=nutch&pn=10&oq=nutch&ie=utf-8', 'DIV', 1, 100)
            WHERE
              CHAR > 50
        """
        val rs = stat.executeQuery(sql)

        assertTrue(rs.next())
        assertTrue(rs.getObject(1) is ValueDom)
        assertTrue((rs.getObject(1) as ValueDom).element.toString().contains("nutch"))
        // println(rs.getString(1))

        conn.close()
        deleteDb(name)
    }
}
