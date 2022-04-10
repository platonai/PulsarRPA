package ai.platon.pulsar.cn

import ai.platon.pulsar.common.config.AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.test.VerboseSQLRunner
import kotlin.system.exitProcess

class NewsCrawler(
    val portalUrl: String,
    val outLinkCss: String,
    val navigationCss: String,
    val context: SQLContext = SQLContexts.create(PULSAR_CONTEXT_CONFIG_LOCATION)
) {
    val sqlRunner = VerboseSQLRunner(context)

    fun extractLinks(): List<String> {
        val sql = """
            select dom_all_hrefs(dom, '$outLinkCss') as links from load_and_select('$portalUrl', 'body')
        """.let { SQLTemplate(it).createInstance(portalUrl) }

        val rs = sqlRunner.execute(sql)
        rs.next()
        val array = rs.getObject("LINKS") as Array<Any>
        return array.map { it.toString() }.distinct()
    }

    fun scrapeOutPages() {
        extractLinks().forEachIndexed { i, link ->
            println("$i.\t$link")
            val qlt = """
select
    dom_first_text(doc, '.container .title h1, #main .news_tit_ly') as title,
    dom_first_text(doc, '#con_time, .container .pubdate, #main .newsly_ly') as publish_time,
    dom_first_text(doc, '.container .content, #tex') as content,
    dom_all_imgs(doc, '.container .content, #tex') as content_imgs,
    dom_uri(doc) as URI,
    *
from
    news_load_and_extract(@url)
                """.trimIndent()
            val sql = SQLTemplate(qlt).createInstance(link)
            sqlRunner.execute(sql)
        }
    }
}

fun main() {
//    NewsCrawler(
//        "http://jxj.beijing.gov.cn/jxdt/zwyw/",
//        ".container ul.list li a",
//        ".changepage"
//    ).scrapeOutPages()

    NewsCrawler(
        "http://gxt.jl.gov.cn/xxgk/zcwj/",
        "#content ul li a",
        ".fy_ly"
    ).scrapeOutPages()

    exitProcess(0)
}
