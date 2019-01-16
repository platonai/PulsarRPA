package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.common.math.vectors.get
import `fun`.platonic.pulsar.dom.FeaturedDocument
import `fun`.platonic.pulsar.ql.Db.config
import `fun`.platonic.pulsar.ql.Db.deleteDb
import `fun`.platonic.pulsar.ql.Db.getDBName
import `fun`.platonic.pulsar.ql.h2.domValue
import `fun`.platonic.pulsar.ql.types.ValueDom
import org.h2.api.JavaObjectSerializer
import org.h2.engine.SysProperties
import org.h2.util.JdbcUtils
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

    lateinit var baseDom: ValueDom

    @Before
    override fun setup() {
        super.setup()

        config.traceTest = false
        config.memory = true
        config.networked = true
        // config.beforeTest()

        // val doc = Jsoup.connect("http://www.baidu.com/").get()
        val doc = FeaturedDocument.createShell("http://example.com/")
        doc.body.append("<div>Hello World</div>")
        baseDom = domValue(doc)
    }

    @After
    override fun teardown() {
        super.teardown()
        afterTest()
    }

    @Ignore("org.jsoup.nodes.Element has no proper equals method")
    @Test
    fun testElementSerialization() {
        val ele = baseDom.element
        // TODO: should we need a equals method for Element?
        assertEquals(ele, Jsoup.parse(ele.toString()))
    }

    @Test
    fun testMetadata() {
        val sql = "SELECT * FROM METAT_PARSE('http://news.huaxi100.com/index.php?m=content&c=index&a=show&catid=18&id=959926')"
        val rs = stat.executeQuery(sql)

        var i = 1
        while (rs.next()) {
            println("${i++}. --------------------------")
            println(rs.getString(1))
            println(rs.getString(2))
        }

        conn.close()
        deleteDb(name)
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
    fun testStaticGlobalSerializer() {
        JdbcUtils.serializer = object : JavaObjectSerializer {
            override fun serialize(obj: Any): ByteArray {
                return byteArrayOf(1, 2, 3)
            }

            override fun deserialize(bytes: ByteArray): Any {
                return 100500
            }
        }

        try {
            deleteDb("javaSerializer")
            val conn = Db.getConnection("javaSerializer")

            val stat = conn.createStatement()
            stat.execute("create table t(id identity, val other)")

            val ins = conn.prepareStatement("insert into t(val) values(?)")

            ins.setObject(1, 100500, Types.JAVA_OBJECT)
            assertEquals(1, ins.executeUpdate())

            val s = conn.createStatement()
            val rs = s.executeQuery("select val, id from t")

            assertTrue(rs.next())

            assertEquals(100500, rs.getObject(1) as Int)

            // assertEquals(byteArrayOf(1, 2, 3), rs.getBytes(1))

            conn.close()
            deleteDb("javaSerializer")
        } finally {
            JdbcUtils.serializer = null
        }
    }

    @Test
    fun testNetworkSerialization2() {
        SysProperties.serializeJavaObject = false

        val name = getDBName()
        val conn = Db.getConnection(name)
        val stat = conn.createStatement()
        assertTrue(stat is org.h2.jdbc.JdbcStatement)

        val sql = """
            SELECT
              DOM,
              `_char`, `_txt-blk`, `_a`,
              DOM_prettyName(DOM) AS `Name`,
              DOM_cssSelector(DOM) AS `Css Selector`
            FROM
              DOM_features('https://www.baidu.com/s?wd=nutch&pn=10&oq=nutch&ie=utf-8', 'DIV', 1, 100)
            WHERE
              `_char` > 50
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
