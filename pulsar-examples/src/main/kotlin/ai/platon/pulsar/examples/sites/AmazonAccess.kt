package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.examples.WebAccess
import ai.platon.pulsar.net.browser.WebDriverPool
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.model.WebPageFormatter
import com.google.common.collect.Lists
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import java.nio.file.Files

class AmazonAccess: WebAccess() {
    private val url = "https://www.amazon.com/"
    private val loadOutPagesArgs = "-ic -i 1s -ii 7d -ol \"a[href~=/dp/]\""
    private var round = 0
    private var numTotalPages = 0
    private val driverPool = env.getBean(WebDriverPool::class.java)

    init {
        BrowserControl.imagesEnabled = false
        BrowserControl.headless = true
    }

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
        val portalUrlTemplates = arrayOf(
                "https://www.amazon.com/s?i=specialty-aps&srs=13575748011&page={{page}}&qid=1575032004&ref=lp_13575748011_pg_{{page}}",
                "https://www.amazon.com/s?i=fashion-girls-intl-ship&bbn=16225020011&rh=n%3A7141123011%2Cn%3A16225020011%2Cn%3A3880961&page={{page}}&qid=1578841587&ref=sr_pg_{{page}}",
                "https://www.amazon.com/s?i=fashion-boys-intl-ship&bbn=16225021011&rh=n%3A7141123011%2Cn%3A16225021011%2Cn%3A6358551011&page={{page}}&qid=1578842855&ref=sr_pg_{{page}}",
                "https://www.amazon.com/s?i=pets-intl-ship&bbn=16225013011&rh=n%3A16225013011%2Cn%3A2975312011&page={{page}}&qid=1578842918&ref=sr_pg_{{page}}"
        )

        val portalUrls = portalUrlTemplates.flatMap { template ->
            IntRange(1, 10).map { template.replace("{{page}}", it.toString()) }
        }.shuffled()

        portalUrls.forEach {
            println(it)
        }

        portalUrls.forEach {
            testIpLimit(it)
        }
    }

    fun testIpLimit(portalUrl: String) {
        val args = "-i 1s -ii 1s -ol \"a[href~=/dp/]\""
        val options = LoadOptions.parse(args)

        ++round

        log.info("\n\n\n--------------------------\nRound $round $portalUrl")

        val portalPage = i.load(portalUrl, options)

        val portalDocument = i.parse(portalPage)
        val links = portalDocument.select("a[href~=/dp/]") {
            it.attr("abs:href").substringBeforeLast("#")
        }.toSet()

        val sb = StringBuilder("\n")
        links.forEachIndexed { j, l ->
            sb.appendln(String.format("%-10s%s", "$j.", l))
        }
        log.info(sb.toString())
        sb.setLength(0)

        if (links.isEmpty()) {
            log.info("Warning: No links")
            val link = AppPaths.symbolicLinkFromUri(portalPage.url)
            log.info("file://$link")
            log.info("Page details: \n" + WebPageFormatter(portalPage))
        }

        val pages = mutableListOf<WebPage>()
        val itemOptions = options.createItemOption()
        Lists.partition(links.toList(), 20).forEach { urls ->
            driverPool.reset()
            i.loadAll(urls, itemOptions).let { pages.addAll(it) }
        }

//        Lists.partition(links.toList(), 5).take(1).forEach { urls ->
//            i.loadAll(urls, itemOptions).let { pages.addAll(it) }
//        }

        if (pages.isEmpty()) return

        numTotalPages += pages.size
        val lengths = pages.map { it.contentBytes.toLong() }.sortedDescending()
        val shortLengths = lengths.filter { it < 1e6 }

        val ds = SummaryStatistics()
        pages.forEach { ds.addValue(it.contentBytes.toDouble()) }

        sb.setLength(0)
        log.info("== Page report ==")
        sb.append("\nStatus: ")
        pages.joinTo(sb) { it.protocolStatus.name }
        sb.append("\nLength: ")
        lengths.joinTo(sb) { StringUtil.readableByteCount(it) }
        sb.appendln()
        sb.append(ds)
        log.info(sb.toString())

        // half pages are less than 1M
        if (shortLengths.size > 0.5 * lengths.size) {
            val bannedIps = pages.filter { it.contentBytes < 1e6 }
                    .mapNotNullTo(HashSet()) { it.metadata[Name.PROXY] }.joinToString { it }
            log.info("Ip banned after $numTotalPages pages")
            log.info("Ips banned: $bannedIps")
        }
        // TODO: re-fetch all broken pages
    }
}

fun main() {
    val archiveDir = AppPaths.TMP_ARCHIVE_DIR.resolve(DateTimeUtil.now("MMdd.HHmm"))
    Files.move(AppPaths.WEB_CACHE_DIR, archiveDir)

    val access = AmazonAccess()
    // access.laptops()
    access.testIpLimit()
}
