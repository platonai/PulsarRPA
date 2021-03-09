package ai.platon.pulsar.ql

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.ql.annotation.H2Context
import ai.platon.pulsar.ql.h2.SqlUtils
import ai.platon.pulsar.ql.h2.addColumn
import ai.platon.pulsar.ql.h2.udfs.CommonFunctions
import org.junit.Assert
import org.junit.Test
import java.sql.Types
import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestSqlUtils {

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
    fun `extract url from sql's from clause using regex`() {
        val url = "http://amazon.com/a/reviews/123?pageNumber=21&a=b"
        val sql = """
            select dom_first_text(dom, '#container'), dom_first_text(dom, '.price')
            from load_and_select('$url', ':root body');
        """.trimIndent()
        val actualUrl = SqlUtils.extractUrlFromFromClause(sql)
        assertEquals(url, actualUrl)
    }
}
