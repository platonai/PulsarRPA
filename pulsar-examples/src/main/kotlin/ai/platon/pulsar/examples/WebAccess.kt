package ai.platon.pulsar.examples

import ai.platon.pulsar.common.PulsarContext
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.URLUtil
import ai.platon.pulsar.net.browser.SeleniumEngine
import ai.platon.pulsar.persist.WebPageFormatter

object WebAccess {
    private val i = PulsarContext.createSession()

    val seeds = mapOf(
            0 to "https://www.mia.com/formulas.html",
            1 to "https://www.mia.com/diapers.html",
            2 to "http://category.dangdang.com/cid4002590.html",
            3 to "https://list.mogujie.com/book/magic/51894",
            4 to "https://category.vip.com/search-1-0-1.html?q=3|49738||&rp=26600|48483&ff=|0|2|1&adidx=2&f=ad&adp=130610&adid=632686",
            5 to "https://list.jd.com/list.html?cat=6728,6742,13246",
            6 to "https://list.gome.com.cn/cat10000055-00-0-48-1-0-0-0-1-2h8q-0-0-10-0-0-0-0-0.html?intcmp=bx-1000078331-1",
            7 to "https://search.yhd.com/c0-0/k%25E7%2594%25B5%25E8%25A7%2586/",
            8 to "http://www.sh.chinanews.com/jinrong/index.shtml",
            9 to "https://music.163.com/",
            10 to "https://news.sogou.com/ent.shtml",
            11 to "http://shop.boqii.com/brand/"
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

    // private val loadOptions = "--parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome"
    private val loadOptions = "--expires=1d"

    fun load() {
        val url = seeds[11]?:return
        val args = "-ps -expires 1s"

        val page = i.load("$url $args")
        val document = i.parse(page)
        // page.links.stream().parallel().forEach { i.load("$it") }
        // println(WebPageFormatter(page).withLinks())

//        val document = i.parse(page)
//        i.export(page)
    }

    fun parallelLoadAll() {
        val args = "-parse -expires 1s -preferParallel"
        val options = LoadOptions.parse(args)
        i.loadAll(trivialUrls, options).flatMap { it.links }.map { it.toString() }
                .groupBy { URLUtil.getHost(it, URLUtil.GroupMode.BY_DOMAIN) }
                .forEach { domain, urls ->
                    PulsarContext.createSession().loadAll(urls.distinct().shuffled().take(20), options)
                }
    }

    fun loadAllProducts() {
        val url = seeds[2]?:return
        // val outlinkSelector = ".goods_item a[href~=detail]"
        val outlinkSelector = ".shoplist .cloth_shoplist li a.pic"

        initClientJs()

        i.load("$url --expires 1s")
                .let { i.parse(it) }
                // .also { println(it.document) }
                .select(outlinkSelector) { it.attr("href") }
//                .asSequence() // seems sync
                .sortedBy { it.length }
                .take(40)
//                .onEach { println(it) }
                .map { i.load("$it -persist") }
                .map { i.parse(it) }
                .onEach { println("${it.location}\t${it.title}") }
                .map { i.export(it) }
    }

    fun loadAllProducts2() {
        val url = seeds[3]?:return

        val portal = i.load("$url $loadOptions")
        val doc = i.parse(portal)
        doc.select(".goods_item a[href~=detail]")
                .map { it.attr("abs:href").substringBefore("?") }
                .forEach { portal.vividLinks[it] = "" }
        println(WebPageFormatter(portal))
        println(portal.simpleVividLinks)
        val links = portal.simpleLiveLinks.filter { it.contains("detail") }
        val pages = i.parallelLoadAll(links, LoadOptions.Companion.parse("--parse"))
        pages.forEach { println("${it.url} ${it.pageTitle}") }
    }

    fun loadAllNews() {
        val url = seeds[8]?:return

        val portal = i.load("$url $loadOptions")
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = i.parallelLoadAll(links, LoadOptions.Companion.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }

    fun scan() {
        val contractBaseUri = "http://www.ccgp-hubei.gov.cn:8040/fcontractAction!download.action?path="
        i.pulsar.scan(contractBaseUri).forEachRemaining {
            val size = it.content?.array()?.size?:0
            println(size)
        }
    }

    fun piped() {
        val url = seeds[8]?:return

        arrayOf(url)
                .map { i.load(it) }
                .map { i.parse(it) }
                .forEach { println("${it.location} ${it.title}") }
    }

    fun truncate() {
        i.pulsar.webDb.truncate()
    }

    private fun initClientJs() {
        val parameters = mapOf("propertyNames" to listOf("font-size", "color", "background-color"))
        val browserControl = BrowserControl(parameters)
        SeleniumEngine.browserControl = browserControl
    }

    fun run() {
        load()
        // parallelLoadAll()
    }
}

fun main() {
    WebAccess.run()
}
