package ai.platon.pulsar.ql

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.ql.h2.addColumn
import org.h2.value.ValueString
import org.junit.Test
import java.sql.Types
import kotlin.test.assertEquals

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestResultSetUtils {

    @Test
    fun testResultSetFormatter() {
        val rs = ResultSets.newSimpleResultSet()
        rs.addColumn("A", Types.DOUBLE, 6, 4)
        rs.addColumn("B")
        val a = arrayOf(0.8056499951626754, 0.5259673975756397, 0.869188405723, 0.4140625)
        val b = arrayOf("a", "b", "c", "d")
        a.zip(b).map { arrayOf(it.first, it.second) }.forEach { rs.addRow(*it) }
        val fmt = ResultSetFormatter(rs)
        println(fmt.toString())
    }

    @Test
    fun `Extract url from sql's from clause using regex`() {
        val url = "http://amazon.com/a/reviews/123?pageNumber=21&a=b"
        val sql = """
            select dom_first_text(dom, '#container'), dom_first_text(dom, '.price')
            from load_and_select('$url', ':root body');
        """.trimIndent()
        val actualUrl = ResultSetUtils.extractUrlFromFromClause(sql)
        assertEquals(url, actualUrl)
    }

    @Test
    fun testTranspose() {
        val rs = ResultSets.newSimpleResultSet()
        val columnCount = 5
        val transposedRowCount = 10
        IntRange(1, columnCount).map { i ->
            rs.addColumn("C$i", Types.ARRAY, 0, 0)
        }

        val row = IntRange(1, columnCount).map { i ->
            IntRange(1, transposedRowCount).map { j -> ValueString.get("C${i}R$j") }.toTypedArray()
        }.toTypedArray()
        rs.addRow(*row)

        if (rs.next()) {
            val c1 = "'C1R1', 'C1R2', 'C1R3', 'C1R4', 'C1R5', 'C1R6', 'C1R7', 'C1R8', 'C1R9', 'C1R10'"
            val array = rs.getArray("C1").array as Array<Any>
            assertEquals(c1, array.joinToString())
        }

        rs.beforeFirst()
        println(ResultSetFormatter(rs, withHeader = true).toString())

        rs.beforeFirst()
        val newRs = ResultSetUtils.transpose(rs)
        var i = 0
        while (rs.next()) {
            ++i
            assertEquals("C${i}R${i}", rs.getString("C1"))
        }
        println(ResultSetFormatter(newRs, withHeader = true).toString())
    }
}
