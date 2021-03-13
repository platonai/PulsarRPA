package ai.platon.pulsar.ql

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.h2.SqlUtils
import ai.platon.pulsar.ql.h2.udfs.CommonFunctionTables
import org.h2.value.ValueArray
import org.h2.value.ValueResultSet
import org.h2.value.ValueString
import org.junit.Test
import java.sql.Types
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestUdfs {

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

        println(ResultSetFormatter(rs, withHeader = true).toString())

        val newRs = SqlUtils.transpose(rs)
        println(ResultSetFormatter(newRs, withHeader = true).toString())
    }
}
