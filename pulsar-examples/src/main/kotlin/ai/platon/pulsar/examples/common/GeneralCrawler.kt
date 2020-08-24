package ai.platon.pulsar.examples.common

import ai.platon.pulsar.common.FileCommand
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_AFTER_FETCH_BATCH_HANDLER
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_BEFORE_FETCH_BATCH_HANDLER
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.persist.model.WebPageFormatter
import com.google.common.collect.Lists
import org.slf4j.LoggerFactory
import java.net.URL

class GeneralCrawler(context: PulsarContext): Crawler(context) {
    private val log = LoggerFactory.getLogger(GeneralCrawler::class.java)

    private val seeds = mapOf(
            0 to "http://category.dangdang.com/cid4002590.html",
            1 to "https://list.mogujie.com/book/magic/51894",
            2 to "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",
            3 to "https://category.vip.com/search-5-0-1.html?q=3|142346||&rp=26600|103675&ff=|0|6|9&adidx=1&f=ad&adp=130612&adid=632821",
            4 to "https://category.vip.com/search-5-0-1.html?q=3|320726||&rp=30068|320513",
            5 to "https://list.jd.com/list.html?cat=6728,6742,13246",
            6 to "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
            7 to "https://search.yhd.com/c0-0/k%25E7%2594%25B5%25E8%25A7%2586/",
            8 to "https://music.163.com/",
            9 to "https://news.sogou.com/ent.shtml",
            10 to "http://shop.boqii.com/brand/",
            11 to "https://list.gome.com.cn/cat10000070-00-0-48-1-0-0-0-1-0-0-1-0-0-0-0-0-0.html?intcmp=phone-163",
            12 to "http://dzhcg.sinopr.org/channel/103",
            13 to "http://blog.zhaojie.me/",
            14 to "https://shopee.vn/search?keyword=qu%E1%BA%A7n%20l%C3%B3t%20na",
            15 to "https://www.darphin.com/collections/essential-oil-elixir",
            16 to "https://qingqueyi.tmall.com/category-1406159179.htm?spm=a220o.1000855.w5002-20531914773.3.647438a1i0xkPI&search=y&catName=%D0%C2%C6%B7-%B3%A4%D0%E4%CC%D7%D7%B0",
            17 to "https://list.suning.com/0-20006-0-0-0-0-0-0-0-0-11635.html"
    )

    private val trivialUrls = listOf(
            "http://futures.hexun.com/2019-06-13/197504448.html",
            "http://www.drytailings.cn/case_tiekuang_xixuan_shebei.html",
            "http://futures.jrj.com.cn/2019/06/04095227661424.shtml",
            "http://futures.cnfol.com/mingjialunshi/20190614/27538801.shtml",
            "http://www.51wctt.com/News/43726/Detail/2",
            "http://www.ijiuai.com/keji/588723.html",
            "http://m.ali213.net/news/gl1906/341009_2.html",
            "http://futures.eastmoney.com/qihuo/i.html",
            "https://news.smm.cn/news/100936727",
            "http://www.96369.net/Indices/125",
            "http://zsjjyjy27.cn.b2b168.com/shop/supply/36379543.html",
            "http://www.b2b168.com/",
            "http://www.96369.net/indices/1003",
            "https://tianjiaji.b2b168.com/ranyoutianjiaji/ranseji/"
    )

    private val loadOptions = "-expires 1d"

    fun collectLinks() {
        // val url = "https://list.mogujie.com/book/magic/51894 -expires 1s"
        // val url = "https://www.mia.com/formulas.html -expires 1s -pageLoadTimeout 1m"
        // val url = "http://category.dangdang.com/cid4002590.html -expires 1s"
        val url = "https://www.hao123.com/ -i 1d"
        val page = i.load(url)
        val doc = i.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        doc.select("a") { it.attr("abs:href") }
                .filter { Urls.isValidUrl(it) }
                .mapTo(HashSet()) { URL(it).let { it.protocol + "://" + it.host } }
                .filter { NetUtil.testHttpNetwork(URL(it)) }
                .joinToString("\n") { it }
                .also { println(it) }

        val path = i.export(doc)
        log.info("Export to: file://{}", path)
    }

    fun load() {
        // val url = "https://list.mogujie.com/book/magic/51894 -expires 1s"
        // val url = "https://www.mia.com/formulas.html -expires 1s -pageLoadTimeout 1m"
        // val url = "http://category.dangdang.com/cid4002590.html -expires 1s"
        // val url = "https://afusjt.tmall.com/search.htm?spm=a1z10.3-b-s.w5001-17122979309.4.454b36d3OGiU6M&scene=taobao_shop -i 1s"
        // val url = "https://search.jd.com/Search?keyword=basketball&enc=utf-8&wq=basketball&pvid=27d8a05385cd49298b5caff778e14b97"
        // val url = "https://qingqueyi.tmall.com/category-1406159179.htm?spm=a220o.1000855.w5002-20531914773.3.647438a1i0xkPI&search=y&catName=%D0%C2%C6%B7-%B3%A4%D0%E4%CC%D7%D7%B0"
//        val url = "https://www.lazada.com.my/shop-small-kitchen-appliances/?spm=a2o4k.home.cate_3.3.75f82e7eneQBGa"
        val url = "https://list.suning.com/0-20006-0-0-0-0-0-0-0-0-11635.html"
        val options = LoadOptions.parse("-i 1s")
        val page = i.load(url, options)
        val doc = i.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        doc.select("a[.product-box href~=product]") { it.attr("abs:href") }.asSequence()
                .filter { Urls.isValidUrl(it) }
                .mapTo(HashSet()) { it.substringBefore(".com")  }
                .filter { !it.isBlank() }
                .mapTo(HashSet()) { "$it.com" }
                .filter { NetUtil.testHttpNetwork(URL(it)) }
                .take(10)
                .joinToString("\n") { it }
                .also { println(it) }

        val path = i.export(doc)
        log.info("Export to: file://{}", path)
    }

    fun loadOutPages() {
        val url = seeds[17]?:return

        var args = "-ic -i 1s -ii 1s"
        // val outlink = ".goods_list_mod a"
        val outlink = when {
            "mia" in url -> "a[href~=item]"
            "gome" in url -> "a[href~=item]"
            "jd.com" in url -> "a[href~=item.jd]"
            "mogu" in url -> "a[href~=detail]"
            "vip" in url -> "a[href~=detail-]"
            "sinopr" in url -> ".title a[href~=p_id]"
            "suning" in url -> ".product-box a[href~=product]"
            else -> "a"
        }

        val page = i.load("$url $args")
        val document = i.parse(page)
        document.absoluteLinks()
        val path = i.export(document)
        println("Export to: file://$path")

        val links = document.select(outlink) { it.attr("abs:href") }
                .mapNotNullTo(mutableSetOf()) { i.normalizeOrNull(it)?.spec }
                .take(20)
        links.forEach { println(it) }

        i.sessionConfig.putBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, BeforeBatchHandler())
        i.sessionConfig.putBean(FETCH_AFTER_FETCH_BATCH_HANDLER, AfterBatchHandler())

        val pages = i.loadAll(links, LoadOptions.parse(args))
//
//        pages.map { i.parse(it) }.map { it.first(".goods_price") }.forEach {
//            println(it?.text()?:"(null)")
//        }

        println("All done.")
        // page.liveLinks.keys.stream().parallel().forEach { i.load(it.toString()) }
        // println(WebPageFormatter(page).withLinks())
    }

    fun loadOutPagesSinopr() {
        val url = "http://dzhcg.sinopr.org/channel/103"
        val args = "-ic -i 1s -ii 10d -rs 10000 -irs 100000"
        val opt = LoadOptions.parse(args)
        val outlink = ".title a[href~=p_id]"

        val links = i.parse(i.load(url, opt))
                .select(outlink) { it.attr("abs:href") }.toSet().take(20)
        links.forEach { println(it) }

        val pages = i.loadAll(links, opt, areItems = true)

        pages.map { i.parse(it) }.map { it.first(".goods_price") }.forEachIndexed { i, it ->
            println("${i + 1}.\t" + (it?.text()?:"(null)"))
        }

        println("All done.")
        // page.liveLinks.keys.stream().parallel().forEach { i.load(it.toString()) }
        // println(WebPageFormatter(page).withLinks())
    }

    fun parallelLoadOutPages() {
        IntRange(0, 10).toList().parallelStream().forEach {
            loadOutPages()
        }
    }

    fun parallelLoadAllOutPages() {
        val args = "-parse -expires 1s -preferParallel true"
        val options = LoadOptions.parse(args)
        val tasks = i.loadAll(seeds.values, options).flatMap { it.links }.map { it.toString() }
                .groupBy { URLUtil.getHost(it, URLUtil.GroupMode.BY_DOMAIN) }.toList()
        Lists.partition(tasks, AppConstants.FETCH_THREADS).forEach { partition ->
            partition.parallelStream().forEach { (_, urls) ->
                i.context.createSession().use {
                    it.loadAll(urls.distinct().shuffled().take(10), options)
                }
            }
        }
    }

    fun loadAllProducts() {
        val url = seeds[2]?:return
        // val outlinkSelector = ".goods_item a[href~=detail]"
        val outlinkSelector = ".cloth_shoplist li a.pic"

        val links = i.load("$url -expires 1s")
                .let { i.parse(it) }
                .select(outlinkSelector) { it.attr("href") }
                .sortedBy { it.length }
                .take(40)
        log.info("Loading {} pages", links.size)
        val pages = i.loadAll(links, LoadOptions.parse("-retry -expires 1s"))

        println(pages.size)
    }

    fun parallelLoadAllProducts() {
        val url = seeds[3]?:return

        val portal = i.load("$url $loadOptions")
        val doc = i.parse(portal)
        doc.select(".goods_item a[href~=detail]")
                .map { it.attr("abs:href").substringBefore("?") }
                .forEach { portal.vividLinks[it] = "" }
        println(WebPageFormatter(portal))
        println(portal.simpleVividLinks)
        val links = portal.simpleLiveLinks.filter { it.contains("detail") }
        val pages = i.parallelLoadAll(links, LoadOptions.parse("-ps"))
        pages.forEach { println("${it.url} ${it.pageTitle}") }
    }

    fun loadAllNews() {
        val url = seeds[8]?:return

        val portal = i.load("$url $loadOptions")
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = i.parallelLoadAll(links, LoadOptions.Companion.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }

    fun localFileCommand() {
        while (true) {
            if (FileCommand.check(AppConstants.CMD_PROXY_RECONNECT)) {
                println("Execute local file command: " + AppConstants.CMD_PROXY_RECONNECT)
            }
            Thread.sleep(5000)
        }
    }

    fun run() {
        // load()
        // collectLinks()
        loadOutPages()
        // loadOutPagesSinopr()
        // repeat(10) {
        //   parallelLoadOutPages()
        // }
        // loadAllProducts()
        // parallelLoadAll()
        // parallelLoadAllProducts()
        // extractAds()
    }
}

fun main() = withContext { GeneralCrawler(it).run() }
