package ai.platon.pulsar.examples

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.ql.h2.H2MemoryDb

class SqlManual {
    private val conn = H2MemoryDb().getRandomConnection()
    private val statement = conn.createStatement()
    private val url = "https://list.jd.com/list.html?cat=652,12345,12349"

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

    fun runAll() {
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

fun main() = withContext { SqlManual().runAll() }
