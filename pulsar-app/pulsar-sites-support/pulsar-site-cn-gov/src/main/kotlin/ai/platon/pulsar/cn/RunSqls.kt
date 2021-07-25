package ai.platon.pulsar.cn

import ai.platon.pulsar.common.config.AppConstants.PULSAR_CONTEXT_CONFIG_LOCATION
import ai.platon.pulsar.common.sql.SQLInstance
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLRunner
import kotlin.system.exitProcess

fun main() = withSQLContext(PULSAR_CONTEXT_CONFIG_LOCATION) { cx ->
    // System.setProperty(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

    val sqls = listOf(
        "http://jxj.beijing.gov.cn/jxdt/zwyw/" to """
select * from loadOutPagesAndExtractArticles(@url, '.container ul.list li');
        """,

        "http://jxj.beijing.gov.cn/jxdt/zwyw/" to """
select
    dom_first_text(dom, '.article h1') as title,
    dom_first_text(dom, '.article .pages-date') as publish_time,
    dom_first_text(dom, '.article .pages-date span') as source,
    dom_first_text(dom, '.article #UCAP-CONTENT') as article,
    dom_uri(dom)
from
    load_out_pages(@url, '.container ul.list li');
        """,
    )
        .map { SQLInstance(it.first, SQLTemplate(it.second)) }
        .filter { "loadOutPagesAndExtractArticles" in it.sql }

    VerboseSQLRunner(cx).executeAll(sqls)

    exitProcess(0)
}
