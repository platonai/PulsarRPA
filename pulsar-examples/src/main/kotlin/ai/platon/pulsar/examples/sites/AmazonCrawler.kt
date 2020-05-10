package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.examples.common.Crawler

class AmazonAccess: Crawler() {
    private val url = "https://www.amazon.com/"
    private val loadOutPagesArgs = "-ic -i 1s -ii 7d -ol \"a[href~=/dp/]\""

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
}

fun main() {
    // System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    AmazonAccess().use { it.laptops() }
}
