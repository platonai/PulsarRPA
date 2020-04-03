package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.nodes.node.ext.name
import ai.platon.pulsar.dom.nodes.node.ext.uniqueName
import ai.platon.pulsar.examples.Crawler
import ai.platon.pulsar.persist.WebPage
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element

class AmazonCategories: Crawler() {
    private val url = "https://www.amazon.com/"
    private val siteDirectory = "https://www.amazon.com/gp/site-directory?ref_=nav_em_T1_0_2_2_35__fullstore"
    private val loadOptions = LoadOptions.parse("-i 1s")
    private var j = 0

    fun collectFromSiteDirectory() {
        val page = i.load(siteDirectory, loadOptions)
        val document = i.parse(page)
        document.absoluteLinks()
        var j = 0
        document.body.select(".fsdDeptBox").forEach {
            val group = it.selectFirst("h2")?.text()?:""
            println()
            println(group)
            it.select("a").forEach {
                val text = it.text()?:""
                val href = it.attr("href")?:""
                println(String.format("%-5d | %20s | %s", ++j, text, href))
            }
        }
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
    AmazonCategories().use {
        it.collectFromSiteDirectory()
    }
}
