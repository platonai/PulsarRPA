package ai.platon.pulsar.ql

import ai.platon.pulsar.common.options.LoadOptionDefaults
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.component.LoadComponent
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.ql.context.DefaultClassPathXmlSQLContext
import ai.platon.pulsar.ql.h2.H2Db
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.context.StaticH2SQLContext
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
            PulsarContexts.activate(DefaultClassPathXmlSQLContext())

            /**
             * Load options are in webpage scope, so it should be initialized after PulsarContextInitializer
             * */
            LoadOptionDefaults.apply {
                parse = true
                ignoreFailure = true
                nJitRetry = 3
                test = 1
                browser = BrowserType.MOCK_CHROME
            }
        }

        val logger = LoggerFactory.getLogger(TestBase::class.java)

        val history = mutableListOf<String>()
        val startTime = Instant.now()
    }

    val context = SQLContexts.activate(DefaultClassPathXmlSQLContext())
    val session = context.createSession()

    fun execute(sql: String, printResult: Boolean = true) {
        context.run { connection ->
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
    }

    fun query(sql: String, printResult: Boolean = true): ResultSet {
        return context.runQuery { connection ->
            connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
                .use { stat ->
                    val rs = stat.executeQuery(sql)
                    if (printResult) {
                        println(ResultSetFormatter(rs))
                    }
                    history.add("${sql.trim { it.isWhitespace() }};")
                    rs
                }
        }
    }

    fun assertResultSetEquals(expected: String, sql: String) {
        assertEquals(expected, ResultSetFormatter(query(sql)).toString())
    }
}
