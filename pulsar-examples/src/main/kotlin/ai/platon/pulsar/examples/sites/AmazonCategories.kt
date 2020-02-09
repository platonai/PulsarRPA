package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.examples.WebAccess

class AmazonCategories: WebAccess() {
    private val url = "https://www.amazon.com/"
    private val siteDirectory = "https://www.amazon.com/gp/site-directory?ref_=nav_em_T1_0_2_2_35__fullstore"

    init {
//        WebDriverControl..imagesEnabled = false
//        WebDriverControl.headless = false
    }

    fun collectFromSiteDirectory() {
        val page = i.load(siteDirectory, LoadOptions.parse("-i 1s"))
        val document = i.parse(page)
        document.absoluteLinks()
        var i = 0
        document.body.select(".fsdDeptBox").forEach {
            val group = it.selectFirst("h2")?.text()?:""
            val href = it.selectFirst("a")?.attr("href")?:""
            println()
            println(group)
            it.select("a").forEach {
                println(String.format("%-5d | %20s | %s", ++i, it.text()?:"", it.attr("href")?:""))
            }
        }
    }

    fun collectSubCategories() {
    }

    fun collectFromMenu() {
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
}

fun main() {
    val categories = AmazonCategories()
    categories.collectFromSiteDirectory()
}
