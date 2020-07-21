package ai.platon.pulsar.ql

import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.ql.h2.udfs.CommonFunctions
import org.h2.ext.pulsar.annotation.H2Context
import org.h2.tools.SimpleResultSet
import org.junit.Assert
import org.junit.Test
import java.sql.Types
import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaType

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestAnything {

    @Test
    fun testURL() {
        var urls = listOf(
                "http://bond.eastmoney.com/news/1326,20160811671616734.html",
                "http://bond.eastmoney.com/news/1326,20161011671616734.html",
                "http://tech.huanqiu.com/photo/2016-09/2847279.html",
                "http://tech.hexun.com/2016-09-12/186368492.html",
                "http://opinion.cntv.cn/2016/04/17/ARTI1397735301366288.shtml",
                "http://tech.hexun.com/2016-11-12/186368492.html",
                "http://ac.cheaa.com/2016/0905/488888.shtml",
                "http://ankang.hsw.cn/system/2016/0927/16538.shtml",
                "http://auto.nbd.com.cn/articles/2016-09-28/1042037.html",
                "http://bank.cnfol.com/pinglunfenxi/20160901/23399283.shtml",
                "http://bank.cnfol.com/yinhanglicai/20160905/23418323.shtml"
        )

        // longer url comes first
        urls = urls.sortedByDescending { it.length }.toList()
        Assert.assertTrue(11 == urls.size)
        Assert.assertTrue(urls[0].length > urls[1].length)

        val domains = urls.map { URLUtil.getHostName(it) }.filter { Objects.nonNull(it) }.toList()
        Assert.assertTrue(11 == domains.size)
    }

    @Test
    fun testResultSetFormatter() {
        val rs = SimpleResultSet()
        rs.addColumn("A", Types.DOUBLE, 6, 4)
        rs.addColumn("B")
        val a = arrayOf(0.8056499951626754, 0.5259673975756397, 0.869188405723, 0.4140625)
        val b = arrayOf("a", "b", "c", "d")
        a.zip(b).map { arrayOf(it.first, it.second) }.forEach { rs.addRow(*it) }
        val fmt = ResultSetFormatter(rs)
        println(fmt.toString())
    }

    @Test
    fun testNormalize() {
        val session = PulsarContexts.createSession()
        var url = "http://shop.mogujie.com/detail/1llurfa?acm=3.ms.1_4_1llurfa.15.1331-68998.tPlDtqPaugJED.sd_117_116-swt_15-imt_6-t_tPlDtqPaugJED-lc_3-fcid_10059513-bid_15-dit_17-idx_39-dm1_5002"
        val url2 = session.normalize(url)
        println(url2)
    }

    @Test
    fun testShowLoadOptions() {
        LoadOptions.helpList.forEach {
            println(it)
        }
    }

    @Test
    fun testFunctionParameters() {
        CommonFunctions::class.declaredMemberFunctions
                .filter { it.visibility == KVisibility.PUBLIC }.map { it.parameters }.forEach {
            it.filter { it.kind == KParameter.Kind.VALUE }.filter { !it.annotations.any { it is H2Context } }.forEach {
                println("${it.name}: ${it.type.javaType.typeName}")
            }
            println()
        }
    }
}
