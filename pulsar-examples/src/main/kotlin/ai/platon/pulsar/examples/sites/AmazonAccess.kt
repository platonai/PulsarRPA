package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.examples.WebAccess
import org.apache.commons.math3.stat.descriptive.SummaryStatistics

class AmazonAccess: WebAccess() {
    val url = "https://www.amazon.com/"
    val loadOutPagesArgs = "-ic -i 1s -ii 7d -ol \"a[href~=/dp/]\""

    fun load() {
        // TODO: click event support is required
        // click-and-select using javascript
        val args = """-ol "ul.hmenu > li " """
        val homePage = load(url, args)
    }

    fun bestSeller() {
        val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs/ref=zg_bs_unv_hg_0_1063236_2"
        i.load(portalUrl)
    }

    fun smartHome() {
        val portalUrl = "https://www.amazon.com/gp/browse.html?node=6563140011&ref_=nav_em_T1_0_4_13_1_amazon_smart_home"
    }

    fun laptops() {
        val portalUrl = "https://www.amazon.com/s?rh=n%3A565108%2Cp_72%3A4-&pf_rd_i=565108&pf_rd_m=ATVPDKIKX0DER&pf_rd_p=2afdf005-5dad-59c6-b6b0-be699f2d03aa&pf_rd_r=5GYSAF186DKPTYBZT9J6&pf_rd_s=merchandised-search-10&pf_rd_t=101&ref=Oct_TopRatedC_565108_SAll"
        loadOutPages(portalUrl, loadOutPagesArgs)
    }

    fun smartHomeLighting() {
        val portalUrl = "https://www.amazon.com/s?i=specialty-aps&srs=13575748011&page=2&qid=1575032004&ref=lp_13575748011_pg_2"
        loadOutPages(portalUrl, loadOutPagesArgs)
    }

    fun testIpLimit() {
        val portalUrlBase = "https://www.amazon.com/s?i=specialty-aps&srs=13575748011&page=2&qid=1575032004&ref=lp_13575748011_pg_"
        val portalUrls = IntRange(1, 20).map { "$portalUrlBase$it" }
        val args = "-ic -i 1s -ii 1s -ol \"a[href~=/dp/]\""
        val options = LoadOptions.parse(args)

        var round = 0
        var k = 0
        portalUrls.forEach { portalUrl ->
            ++round

            println("\n\n\n")
            println("--------------------------")
            println("Round $round")

            val document = i.parse(i.load(portalUrl))
            val links = document.select("a[href~=/dp/]") {
                it.attr("abs:href").substringBeforeLast("#")
            }.toSet()
            links.forEachIndexed { i, l ->
                println("$i\t$l")
            }

            val pages = i.loadOutPages(portalUrl, "a[href~=/dp/]", options)
            if (pages.isEmpty()) return@forEach

            k += pages.size
            val lengths = pages.map { it.contentBytes.toLong() }.sortedDescending()
                    .joinToString { StringUtil.readableByteCount(it) }

            val ds = SummaryStatistics()
            pages.forEach { ds.addValue(it.contentBytes.toDouble()) }
            println("Page length report")
            println(lengths)
            println(ds.toString())

            // if the average length is less than 1M
            if (ds.mean < 0.2 * 1e6) {
                pages.forEach { i.export(it, "banned") }
                println("Ip banned after $k pages")
                // return
            }
        }
    }
}

fun main() {
    val access = AmazonAccess()
    access.laptops()
    access.testIpLimit()
}
