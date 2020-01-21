package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.examples.WebAccess

class AmazonCategories: WebAccess() {
    private val url = "https://www.amazon.com/"

    init {
        BrowserControl.imagesEnabled = false
        BrowserControl.headless = true
    }

    fun collectCategories() {
        // TODO: click event support is required
        // click-and-select using javascript
        val args = """-ol "ul.hmenu > li " """
        val homePage = load(url, args)
    }
}

fun main() {
    val html = ResourceLoader.readString("sites/amazon/amazon-menu.html")
    val document = Documents.parse(html, "https://www.amazon.com/")
    document.absoluteLinks()
    var i = 0
    document.body.select("ul li").forEach {
        val title = it.selectFirst("div")?.text()?:""
        val href = it.selectFirst("a")?.attr("href")?:""
        println(String.format("%-5d | %20s | %s", ++i, title, href))
    }
}
