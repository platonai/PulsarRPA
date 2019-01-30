package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.dom.FeaturedDocument
import `fun`.platonic.pulsar.dom.nodes.node.ext.uniqueName
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
import kotlin.test.assertTrue

class TestJavaObjectSerializer : TestBase() {

    private lateinit var baseDom: ValueDom

    @Before
    override fun setup() {
        db.config.traceTest = false
        db.config.memory = true
        db.config.networked = true
        SysProperties.serializeJavaObject = true

        super.setup()

        // val doc = Jsoup.connect("http://www.baidu.com/").get()
        val doc = FeaturedDocument.createShell("http://example.com/")
        doc.body.append("<div>Hello World</div>")
        baseDom = domValue(doc)
    }

    @After
    override fun teardown() {
        super.teardown()
        // SysProperties.serializeJavaObject = false
    }

    @Test
    fun testLocalSerialization() {
        val serializer = PulsarObjectSerializer()

        val baseUri = "http://example.com/"
        val doc = Jsoup.parseBodyFragment("<div>Hello World</div>", baseUri)

        baseDom = ValueDom.get(doc.body().selectFirst("div"))
        val bytes = serializer.serialize(baseDom)
        val obj = serializer.deserialize(bytes)
        assertTrue(obj is ValueDom)
        val dom = obj as ValueDom

        // println(dom.outHtml)

        val deserializeBytes = serializer.serialize(dom)
        val deserializeObject = serializer.deserialize(deserializeBytes)
        assertTrue(deserializeObject is ValueDom)
        val dom2 = deserializeObject
        assertTrue { dom == dom2 }

        assertTrue { obj !== deserializeObject }
        assertTrue { dom.element.baseUri() == dom2.element.baseUri() }
        assertTrue { dom.element.ownerDocument() != dom2.element.ownerDocument() }
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
    }

    @Test
    fun testNetworkSerialization2() {
        assertTrue(stat is org.h2.jdbc.JdbcStatement)

        val expr = "sibling > 20 && char > 40 && char < 100 && width > 200"
        val sql = """SELECT
            DOM, DOM_FIRST_HREF(DOM), TOP, LEFT, WIDTH, HEIGHT, CHAR, IMG, A, SIBLING, DOM_TEXT(DOM)
            FROM LOAD_AND_GET_FEATURES('$productIndexUrl', '*:expr($expr)')
            ORDER BY SIBLING DESC, CHAR DESC LIMIT 500"""
        val rs = stat.executeQuery(sql)

        assertTrue(rs.next())
        assertTrue(rs.getObject(1) is ValueDom)
        val dom = rs.getObject(1) as ValueDom
        assertTrue { dom.element.uniqueName.contains("nfItem") }
    }
}
