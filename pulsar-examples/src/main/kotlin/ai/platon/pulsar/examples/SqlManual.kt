package ai.platon.pulsar.examples

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.ql.h2.H2Db

class SqlManual {
    val conn = H2Db().getRandomConnection()
    val statement = conn.createStatement()

    val url = "https://list.jd.com/list.html?cat=652,12345,12349"

    fun load() = execute("select dom_doc_title(dom_load('$url -i 1d')) as title")

    fun scrape() = execute("""
        select
            dom_first_text(dom, '.p-name em') as name,
            dom_first_text(dom, '.p-price') as price
        from
            load_and_select('$url -i 1d -ii 7d', 'li[data-sku]')"""
    )

    fun scrapeOutPages() = execute("""
        select
            dom_first_text(dom, '.sku-name') as name,
            dom_first_text(dom, '.p-price') as price
        from
            load_out_pages('$url -i 1d -ii 7d', 'a[href~=item]')"""
    )

    fun run() {
        load()
        scrape()
        scrapeOutPages()
    }

    private fun execute(sql: String) {
        val regex = "^(SELECT|CALL).+".toRegex()
        if (sql.toUpperCase().filter { it != '\n' }.trimIndent().matches(regex)) {
            val rs = statement.executeQuery(sql)
            println(ResultSetFormatter(rs, withHeader = true))
        } else {
            val r = statement.execute(sql)
            println(r)
        }
    }
}

fun main() = withContext { SqlManual().run() }
