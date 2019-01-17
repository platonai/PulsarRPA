package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.common.ScentPaths
import `fun`.platonic.pulsar.common.StringUtil
import `fun`.platonic.pulsar.ql.Db.BASE_TEST_DIR
import `fun`.platonic.pulsar.ql.h2.H2QueryEngine
import `fun`.platonic.pulsar.ql.utils.ResultSetFormatter
import com.udojava.evalex.Expression.e
import org.h2.store.fs.FileUtils
import org.h2.tools.DeleteDbFiles
import org.h2.tools.Server
import org.h2.tools.SimpleResultSet
import org.junit.After
import org.junit.Before
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * The base class for all tests.
 */
abstract class TestBase {

    val productIndexUrl = "https://www.mia.com/formulas.html"
    val productDetailUrl = "https://www.mia.com/item-1687128.html"
    val newsIndexUrl = "http://news.baidu.com/guoji"
    val newsDetailUrl = "http://news.163.com/17/1119/09/D3JJF1290001875P.html"
    val newsDetailUrl2 = "http://www.chinanews.com/gn/2018/03-02/8458538.shtml"

    var urlGroups = mutableMapOf<String, Array<String>>()
    val history = mutableListOf<String>()

    init {
        urlGroups["baidu"] = arrayOf(
                "https://www.baidu.com/s?wd=马航&oq=马航&ie=utf-8"
        )
        urlGroups["jd"] = arrayOf(
                "https://list.jd.com/list.html?cat=670,671,672",
                "https://item.jd.com/1238838350.html",
                "http://search.jd.com/Search?keyword=长城葡萄酒&enc=utf-8&wq=长城葡萄酒",
                "https://detail.tmall.com/item.htm?id=536690467392",
                "https://detail.tmall.com/item.htm?id=536690467392",
                "http://item.jd.com/1304924.html",
                "http://item.jd.com/3564062.html",
                "http://item.jd.com/1304923.html",
                "http://item.jd.com/3188580.html",
                "http://item.jd.com/1304915.html"
        )
        urlGroups["mia"] = arrayOf(
                "https://www.mia.com/formulas.html",
                "https://www.mia.com/item-2726793.html",
                "https://www.mia.com/item-1792382.html",
                "https://www.mia.com/item-1142813.html"
        )
        urlGroups["mogujie"] = arrayOf(
                "http://list.mogujie.com/book/jiadian/10059513",
                "http://list.mogujie.com/book/skirt",
                "http://shop.mogujie.com/detail/1kcnxeu",
                "http://shop.mogujie.com/detail/1lrjy2c"
        )
        urlGroups["meilishuo"] = arrayOf(
                "http://www.meilishuo.com/search/catalog/10057053",
                "http://www.meilishuo.com/search/catalog/10057051",
                "http://item.meilishuo.com/detail/1lvld0y",
                "http://item.meilishuo.com/detail/1lwebsw"
        )
        urlGroups["vip"] = arrayOf(
                "http://category.vip.com/search-1-0-1.html?q=3|29736",
                "https://category.vip.com/search-5-0-1.html?q=3|182725",
                "http://detail.vip.com/detail-2456214-437608397.html",
                "https://detail.vip.com/detail-2640560-476811105.html"
        )
        urlGroups["wikipedia"] = arrayOf(
                "https://en.wikipedia.org/wiki/URL",
                "https://en.wikipedia.org/wiki/URI",
                "https://en.wikipedia.org/wiki/URN"
        )
    }

    var name: String = Db.generateTempDbName()
    lateinit var conn: Connection
    lateinit var stat: Statement
    lateinit var engine: H2QueryEngine
    var server: Server? = null

    @Before
    open fun setup() {
        try {
            beforeTest()

            Db.config.traceTest = true
            Db.config.memory = true
            Db.config.networked = false

            engine = H2QueryEngine

            Db.deleteDb(name)
            conn = Db.getConnection(name)
            stat = conn.createStatement()
        } catch (e: Throwable) {
            println(StringUtil.stringifyException(e))
        }
    }

    @After
    open fun teardown() {
        try {
            stat.close()
            conn.close()
            Db.deleteDb(name)

            afterTest()
        } catch (e: Throwable) {
            println(StringUtil.stringifyException(e))
        }
    }

    fun execute(sql: String, printResult: Boolean = true) {
        try {
            val regex = "^(SELECT|CALL).+".toRegex()
            if (sql.filter { it != '\n' }.trimIndent().matches(regex)) {
                val rs = stat.executeQuery(sql)
                if (printResult) {
                    println(ResultSetFormatter(rs))
                }
            } else {
                val r = stat.execute(sql)
                if (printResult) {
                    println(r)
                }
            }

            history.add(sql)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun executeQuery(sql: String, printResult: Boolean = true): ResultSet {
        try {
            val rs = stat.executeQuery(sql)
            if (printResult) {
                println(ResultSetFormatter(rs))
            }
            history.add(sql)
            return rs
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return SimpleResultSet()
    }

    /**
     * This method is called before a complete set of tests is run. It deletes
     * old database files in the test directory and trace files. It also starts
     * a TCP server if the test uses remote connections.
     */
    fun beforeTest() {
        val config = Db.config
        FileUtils.deleteRecursive(Db.BASE_TEST_DIR.toString(), true)
        DeleteDbFiles.execute(Db.BASE_TEST_DIR.toString(), null, true)
        FileUtils.deleteRecursive("trace.db", false)
        if (config.networked) {
            val args = if (config.ssl)
                arrayOf("-tcpSSL", "-tcpPort", config.port.toString())
            else
                arrayOf("-tcpPort", config.port.toString())

            server = Server.createTcpServer(*args)
            try {
                server!!.start()
            } catch (e: SQLException) {
                // println("FAIL: can not start server (may already be running)")
                (e.cause as Exception).printStackTrace()
            }
        }
    }

    /**
     * Stop the server if it was started.
     */
    fun afterTest() {
        val config = Db.config

        if (config.networked && server != null) {
            server!!.stop()
        }
        FileUtils.deleteRecursive("trace.db", true)
        FileUtils.deleteRecursive(Db.BASE_TEST_DIR.toString(), true)

        val content = history.joinToString("\n") { it }
        val path = ScentPaths.get(BASE_TEST_DIR, "sql-history.sql")
        Files.createDirectories(path.parent)
        Files.write(path, content.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
    }
}
