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
    private val productPortalUrl = "https://list.mogujie.com/book/magic/51894"
    private val newsPortalUrl = "http://www.sh.chinanews.com/jinrong/index.shtml"
    // private val loadOptions = "--parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome"
    private val loadOptions = "--expires=1s"
    private val i = PulsarContext.createSession()

    fun load() {
        val page = i.load("$productPortalUrl $loadOptions")
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
        initClientJs()

        i.load("$productPortalUrl $loadOptions")
                .let { i.parse(it) }
                .select(".goods_item a[href~=detail]") { it.attr("abs:href") }
                .asSequence().distinct()
                .take(40)
//                .onEach { println(it) }
                .map { i.load("$it -persist -shortenKey") }
                .map { i.parse(it) }
                .onEach { println(it.title) }
                .map { i.export(it) }
    }

    fun loadAllProducts2() {
        val portal = i.load("$productPortalUrl $loadOptions")
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
        val portal = i.load("$newsPortalUrl $loadOptions")
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
        arrayOf(newsPortalUrl)
                .map { i.load(it) }
                .map { i.parse(it) }
                .forEach { println("${it.baseUri} ${it.title}") }
    }

    private fun initClientJs() {
        val browserControl = BrowserControl()
        mapOf(
                "propertyNames" to listOf("font-size", "color", "background-color")
        ).also { browserControl.parameters.putAll(it) }
        SeleniumEngine.CLIENT_JS = browserControl.getJs(true)
    }
}

fun main() {
    WebAccess.loadAllProducts()
}
