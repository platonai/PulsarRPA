package ai.platon.pulsar.examples

import ai.platon.pulsar.common.PulsarContext
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.data.BrowserControl
import ai.platon.pulsar.dom.nodes.node.ext.formatFeatures
import ai.platon.pulsar.net.SeleniumEngine
import ai.platon.pulsar.persist.WebPageFormatter
import com.google.gson.GsonBuilder

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
            8 to "http://www.sh.chinanews.com/jinrong/index.shtml"
    )

    // private val loadOptions = "--parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome"
    private val loadOptions = "--expires=1s"

    fun load() {
        val url = seeds[3]?:return

        val page = i.load("$url $loadOptions")
        // println(WebPageFormatter(page).withLinks())
        // println(WebPageFormatter(page))

        val document = i.parse(page)
//        val title = document.first(".goods_item .title")?.text()
//        println(title)
        val ele = document.first(".goods_item")
        if (ele != null) {
            println(ele.attributes())
            println(ele.formatFeatures())
        }

        i.export(page)
    }

    fun loadAllProducts() {
        val url = seeds[2]?:return
        // val outlinkSelector = ".goods_item a[href~=detail]"
        val outlinkSelector = ".shoplist .cloth_shoplist li a.pic"

        initClientJs()

        i.load("$url --expires 1d")
                .let { i.parse(it) }
                // .also { println(it.document) }
                .select(outlinkSelector) { it.attr("href") }
//                .asSequence()
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
        val pages = i.parallelLoadAll(links, LoadOptions.parse("--parse"))
        pages.forEach { println("${it.url} ${it.pageTitle}") }
    }

    fun loadAllNews() {
        val url = seeds[8]?:return

        val portal = i.load("$url $loadOptions")
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = i.parallelLoadAll(links, LoadOptions.parse("--parse"))
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
}

fun main() {
    WebAccess.loadAllProducts()
}
