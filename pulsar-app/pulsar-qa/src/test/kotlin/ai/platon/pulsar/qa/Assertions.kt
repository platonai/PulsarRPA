package ai.platon.pulsar.qa

import ai.platon.pulsar.common.XSQLRunner
import ai.platon.pulsar.common.sql.SQLTemplate
import java.sql.ResultSet
import kotlin.test.assertTrue

fun assertAnyNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    var found = 0
    rs.beforeFirst()
    while (rs.next()) {
        val value = rs.getString(field)
        if (value.isNotBlank()) {
            ++found
            break
        }
    }

    assertTrue("Require not blank: <$field>. $message\n$url") { found > 0 }
}

fun assertAnyNotBlank(url: String, template: SQLTemplate, field: String, message: String? = null) {
    val rs = XSQLRunner().execute(url, template)
    assertAnyNotBlank(url, rs, field, message)
}

fun assertAllNotBlank(url: String, rs: ResultSet, field: String, message: String? = null) {
    rs.beforeFirst()
    while (rs.next()) {
        val value = rs.getString(field)
        assertTrue("Require not blank: <$field>. $message\n$url") { value.isNotBlank() }
    }
}

fun assertAllNotBlank(url: String, template: SQLTemplate, field: String, message: String? = null) {
    val rs = XSQLRunner().execute(url, template)
    assertAllNotBlank(url, rs, field, message)
}

fun assertAllNotBlank(url: String, rs: ResultSet, fields: Iterable<String>, message: String? = null) {
    rs.beforeFirst()
    while (rs.next()) {
        fields.forEach { field ->
            val value = rs.getString(field)
            assertTrue("Require not blank: <$field>. $message\n$url") { value.isNotBlank() }
        }
    }
}

fun assertAllNotBlank(url: String, template: SQLTemplate, fields: Iterable<String>, message: String? = null) {
    val rs = XSQLRunner().execute(url, template)
    assertAllNotBlank(url, rs, fields, message)
}
