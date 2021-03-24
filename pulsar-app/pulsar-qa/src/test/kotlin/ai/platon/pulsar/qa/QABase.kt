package ai.platon.pulsar.qa

import ai.platon.pulsar.common.XSqlRunner
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest
class QABase {
    open val resourcePrefix = "config/sites/amazon/crawl/parse/sql"

    fun assertAnyNotBlank(url: String, sqlResource: String, field: String, message: String) {
        val rs = XSqlRunner().execute(url, sqlResource)
        var found = 0
        rs.beforeFirst()
        while (rs.next()) {
            val value = rs.getString(field)
            if (value.isNotBlank()) {
                ++found
                break
            }
        }
        assertTrue(message) { found > 0 }
    }

    fun assertAllNotBlank(url: String, sqlResource: String, field: String, message: String) {
        val rs = XSqlRunner().execute(url, sqlResource)
        rs.beforeFirst()
        while (rs.next()) {
            val value = rs.getString(field)
            assertTrue(message) { value.isNotBlank() }
        }
    }

    fun assertAllNotBlank(url: String, sqlResource: String, fields: Iterable<String>, message: String) {
        val rs = XSqlRunner().execute(url, sqlResource)
        rs.beforeFirst()
        while (rs.next()) {
            fields.forEach {
                val value = rs.getString(it)
                assertTrue(message) { value.isNotBlank() }
            }
        }
    }
}
