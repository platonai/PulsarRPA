package ai.platon.pulsar.qa

import ai.platon.pulsar.common.XSQLRunner
import ai.platon.pulsar.common.sql.SQLInstance
import java.sql.ResultSet
import kotlin.test.assertTrue

data class AssertEntry(
    val url: String,
    val sqlResource: String,
    val fields: List<String>,
    val message: String? = null,
)

fun assertAnyRecordsNotBlank(assert: AssertEntry) {
    val (url, resource, fields, message) = assert
    val sql = SQLInstance.load(url, resource)
    assertAnyRecordsNotBlank(sql, fields, message)
}

fun assertAnyRecordsNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    var found = 0
    rs.beforeFirst()
    while (rs.next()) {
        val value = rs.getString(field)
        if (value.isNotBlank()) {
            ++found
            break
        }
    }

    var msg = "\n$message" ?: ""
    msg = "Field <$field> must not be blank$msg\n$url"
    assertTrue(msg) { found > 0 }
}

fun assertAnyRecordsNotBlank(sql: SQLInstance, field: String, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAnyRecordsNotBlank(sql.url, rs, field, message)
}

fun assertAnyRecordsNotBlank(url: String, rs: ResultSet, fields: Iterable<String>, message: String? = null) {
    var found = 0
    rs.beforeFirst()
    while (rs.next()) {
        fields.forEach { field ->
            val value = rs.getString(field)
            if (value.isNotBlank()) {
                ++found
            }
        }
    }

    var msg = "\n$message" ?: ""
    msg = "Fields <$fields> must not be blank$msg\n$url"
    assertTrue(msg) { found > 0 }
}

fun assertAnyRecordsNotBlank(sql: SQLInstance, fields: Iterable<String>, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAnyRecordsNotBlank(sql.url, rs, fields, message)
}

fun assertAllRecordsNotBlank(assert: AssertEntry) {
    val (url, resource, fields, message) = assert
    val sql = SQLInstance.load(url, resource)
    assertAllRecordsNotBlank(sql, fields, message)
}

fun assertAllRecordsNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    rs.beforeFirst()
    while (rs.next()) {
        val value = rs.getString(field)
        var msg = message?.let { "\n$it" } ?: ""
        msg = "Field <$field> must not be blank$msg\n$url"
        assertTrue(msg) { value.isNotBlank() }
    }
}

fun assertAllRecordsNotBlank(sql: SQLInstance, field: String, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAllRecordsNotBlank(sql.url, rs, field, message)
}

fun assertAllRecordsNotBlank(url: String, rs: ResultSet, fields: Iterable<String>, message: String? = null) {
    rs.beforeFirst()
    while (rs.next()) {
        fields.forEach { field ->
            val value = rs.getString(field)
            var msg = "\n$message" ?: ""
            msg = "Field <$field> must not be blank$msg\n$url"
            assertTrue(msg) { value.isNotBlank() }
        }
    }
}

fun assertAllRecordsNotBlank(sql: SQLInstance, fields: Iterable<String>, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAllRecordsNotBlank(sql.url, rs, fields, message)
}
