package ai.platon.pulsar.qa

import ai.platon.pulsar.test.XSQLRunner
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import java.sql.ResultSet
import kotlin.test.assertTrue

data class CheckEntry(
    val url: String,
    val sqlResource: String,
    val fields: List<String>,
    val message: String? = null,
    val transpose: Boolean = false
)

/*******************************
 * Assert field contains
 * *****************************/

fun assertFieldContains(url: String, sqlResource: String, field: String, substring: String, message: String? = null) {
    val sql = SQLInstance.load(url, sqlResource)
    val rs = XSQLRunner().execute(sql)
    assertFieldContains(url, rs, field, substring, message)
}

fun assertFieldContains(url: String, sql: SQLInstance, field: String, substring: String, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertFieldContains(url, rs, field, substring, message)
}

fun assertFieldContains(url: String, rs: ResultSet, field: String, substring: String, message: String? = null) {
    rs.beforeFirst()
    val value = if (rs.next()) {
        rs.getString(field)
    } else {
        "(empty result set)"
    }

    var msg = message?.let { "\n$message" } ?: ""
    msg = "Field <$field> must contains <$substring> actual <$value> $msg\n$url"
    assertTrue(msg) { substring in value }
}

/*******************************
 * Assert any records not blank
 * *****************************/

fun assertAnyRecordsNotBlank(url: String, rs: ResultSet, fields: Iterable<String>, message: String? = null) {
    fields.forEach { field ->
        assertAnyRecordsNotBlank(url, rs, field, message)
    }
}

fun assertAnyRecordsNotBlank(sql: SQLInstance, fields: Iterable<String>, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAnyRecordsNotBlank(sql.url, rs, fields, message)
}

fun assertAnyRecordsNotBlank(check: CheckEntry) {
    val (url, resource, fields, message) = check
    val sql = SQLInstance.load(url, resource)
    assertAnyRecordsNotBlank(sql, fields, message)
}

fun assertAnyRecordsNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    val numNonBlanks = ResultSetUtils.count(rs) { it.getString(field)?.isNotBlank() == true }

    var msg = message?.let { "\n$message" } ?: ""
    msg = "Field <$field> must not be blank$msg\n$url"
    assertTrue(msg) { numNonBlanks > 0 }
}

fun assertAnyRecordsNotBlank(sql: SQLInstance, field: String, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAnyRecordsNotBlank(sql.url, rs, field, message)
}

/*******************************
 * Assert most records not blank
 * *****************************/

fun assertMostRecordsNotBlank(url: String, rs: ResultSet, fields: Iterable<String>, message: String? = null) {
    fields.forEach { field ->
        assertMostRecordsNotBlank(url, rs, field, message)
    }
}

fun assertMostRecordsNotBlank(sql: SQLInstance, fields: Iterable<String>, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertMostRecordsNotBlank(sql.url, rs, fields, message)
}

fun assertMostRecordsNotBlank(check: CheckEntry) {
    val (url, resource, fields, message) = check
    val sql = SQLInstance.load(url, resource)
    assertMostRecordsNotBlank(sql, fields, message)
}

fun assertMostRecordsNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    val expectedCount = ResultSetUtils.count(rs) / 2
    val numNonBlanks = ResultSetUtils.count(rs) { it.getString(field)?.isNotBlank() == true }

    var msg = message?.let { "\n$message" } ?: ""
    msg = "Non-blank field <$field> must be more than <$expectedCount>, actual <$numNonBlanks> $msg\n$url"
    assertTrue(msg) { numNonBlanks > expectedCount }
}

fun assertMostRecordsNotBlank(sql: SQLInstance, field: String, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertMostRecordsNotBlank(sql.url, rs, field, message)
}

/*******************************
 * Assert all records not blank
 * *****************************/

fun assertAllRecordsNotBlank(sql: SQLInstance, field: String, message: String? = null) {
    assertAllRecordsNotBlank(sql, listOf(field), message)
}

fun assertAllRecordsNotBlank(sql: SQLInstance, fields: Iterable<String>, message: String? = null) {
    val rs = XSQLRunner().execute(sql)
    assertAllRecordsNotBlank(sql.url, rs, fields, message)
}

fun assertAllRecordsNotBlank(check: CheckEntry) {
    val (url, resource, fields, message) = check
    val sql = SQLInstance.load(url, resource)
    assertAllRecordsNotBlank(sql, fields, message)
}

fun assertAllRecordsNotBlank(url: String, rs: ResultSet, fields: Iterable<String>, message: String? = null) {
    fields.forEach { field ->
        assertAllRecordsNotBlank(url, rs, field, message)
    }
}

fun assertAllRecordsNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    val numBlanks = ResultSetUtils.count(rs) { it.getString(field)?.isBlank() == true }

    var msg = message?.let { "\n$message" } ?: ""
    msg = "Field <$field> must not be blank$msg\n$url"
    assertTrue(msg) { numBlanks == 0 }
}
