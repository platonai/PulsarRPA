package ai.platon.pulsar.ql

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.h2.H2Db
import org.junit.After
import org.junit.Before
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import kotlin.test.assertEquals

/**
 * The base class for all tests
 */
abstract class TestBase {

    companion object {
        init {
            SQLContexts.activate()
        }

        val log = LoggerFactory.getLogger(TestBase::class.java)

        val history = mutableListOf<String>()
        val startTime = Instant.now()
    }

    lateinit var connection: Connection

    @Before
    fun setup() {
        connection = H2Db().getRandomConnection()
    }

    @After
    fun tearDown() {
        connection.close()
    }

    fun execute(sql: String, printResult: Boolean = true) {
        connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
            .use { stat ->
                val regex = "^(SELECT|CALL).+".toRegex()
                if (sql.toUpperCase().filter { it != '\n' }.trimIndent().matches(regex)) {
                    val rs = stat.executeQuery(sql)
                    if (printResult) {
                        println(ResultSetFormatter(rs, withHeader = true))
                    }
                } else {
                    val r = stat.execute(sql)
                    if (printResult) {
                        println(r)
                    }
                }
                // SysProperties.serializeJavaObject = lastSerializeJavaObject
                history.add("${sql.trim { it.isWhitespace() }};")
            }
    }

    fun query(sql: String, printResult: Boolean = true): ResultSet {
        connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
            .use { stat ->
                val rs = stat.executeQuery(sql)
                if (printResult) {
                    println(ResultSetFormatter(rs))
                }
                history.add("${sql.trim { it.isWhitespace() }};")
                return rs
            }
    }

    fun assertResultSetEquals(expected: String, sql: String) {
        assertEquals(expected, ResultSetFormatter(query(sql)).toString())
    }
}
