package ai.platon.pulsar.examples.experimental

import ai.platon.pulsar.common.sql.ResultSetFormatter
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import java.util.*

class SqlManual(val context: SQLContext = SQLContexts.create()) {
    private val url = "https://list.jd.com/list.html?cat=652,12345,12349"

    fun scrapeOutPages() = execute("""
        select
            dom_first_text(dom, '.p-price') as price,
            dom_first_text(dom, '.sku-name') as name
        from
            load_out_pages('$url -i 1s -ii 7s', 'a[href~=item]')"""
    )

    fun runAll() {
        scrapeOutPages()
    }

    private fun execute(sql: String) {
        val regex = "^(SELECT|CALL).+".toRegex()
        if (sql.uppercase(Locale.getDefault()).filter { it != '\n' }.trimIndent().matches(regex)) {
            val rs = context.executeQuery(sql)
            println(ResultSetFormatter(rs, withHeader = true))
        } else {
            val r = context.execute(sql)
            println(r)
        }
    }
}

fun main() {
    SqlManual().runAll()
}
