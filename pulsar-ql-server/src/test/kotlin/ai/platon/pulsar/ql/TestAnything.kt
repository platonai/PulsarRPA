package ai.platon.pulsar.ql

import ai.platon.pulsar.common.PulsarContext
import org.jsoup.Jsoup
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestAnything {

    val session = PulsarContext.createSession()

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

        val domains = urls.map { ai.platon.pulsar.common.URLUtil.getHostName(it) }.filter { Objects.nonNull(it) }.toList()
        Assert.assertTrue(11 == domains.size)
    }

    @Test
    fun test() {
        val str = "<div>hello</div><div> world</div>"
        val doc = Jsoup.parseBodyFragment(str)
        println(doc.outerHtml())
        println(doc.body().attr("baseuri"))
    }

    @Test
    fun testNormalize() {
        var url = "http://shop.mogujie.com/detail/1llurfa?acm=3.ms.1_4_1llurfa.15.1331-68998.tPlDtqPaugJED.sd_117_116-swt_15-imt_6-t_tPlDtqPaugJED-lc_3-fcid_10059513-bid_15-dit_17-idx_39-dm1_5002"
        val url2 = session.normalize(url)
        println(url2)
    }
}
