package `fun`.platonic.pulsar.examples

import `fun`.platonic.pulsar.Pulsar
import `fun`.platonic.pulsar.common.options.LoadOptions
import `fun`.platonic.pulsar.persist.WebPageFormatter

object WebAccess {
    private val productPortalUrl = "http://list.mogujie.com/book/jiadian/10059513"
    private val newsPortalUrl = "http://www.sh.chinanews.com/jinrong/index.shtml"
    private val loadOptions = "--parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome"
    private val pulsar = Pulsar()

    fun load() {
        val page = pulsar.load("$productPortalUrl $loadOptions")
        println(WebPageFormatter(page).withLinks())

        val document = pulsar.parse(page)
        val title = document.first(".goods_item .title")?.text()
        println(title)
    }

    fun loadAllProducts() {
        val portal = pulsar.load("$productPortalUrl $loadOptions")
        val doc = pulsar.parse(portal)
        doc.select(".goods_item a[href~=detail]")
                .map { it.attr("abs:href").substringBefore("?") }
                .forEach { portal.vividLinks[it] = "" }
        println(WebPageFormatter(portal))
        println(portal.simpleVividLinks)
        val pages = pulsar.parallelLoadAll(portal.simpleVividLinks)
        pages.forEach { println("${it.url} ${it.pageTitle}") }
    }

    fun loadAllNews() {
        val portal = pulsar.load("$newsPortalUrl $loadOptions")
        val pages = pulsar.parallelLoadAll(portal.simpleLiveLinks.filter { it.contains("jinrong") }, LoadOptions.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }
}

fun main(args: Array<String>) {
    WebAccess.load()
    // WebAccess.loadAllProducts()
}
