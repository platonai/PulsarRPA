package ai.platon.pulsar.test

import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.h2.utils.CSV
import java.nio.file.Path

open class ProductExtractor(
    val exportDirectory: Path,
    val context: SQLContext = SQLContexts.create(),
) {
    val executor = VerboseSQLExecutor(context)

    fun extract(itemsSQL: String, reviewsSQLTemplate: String? = null) {
        val rs = executor.executeQuery(itemsSQL)

        var path = CSV().export(rs, exportDirectory.resolve("item.csv"))
        println("Items are written to file://$path")

        if (reviewsSQLTemplate == null) {
            return
        }

        val sqls = mutableListOf<String>()
        while (rs.next()) {
            val url = rs.getString("baseUri")
            val sql = SQLTemplate(reviewsSQLTemplate).createInstance(url).sql
            sqls.add(sql)
        }

        val resultSets = executor.queryAll(sqls)

        path = CSV().export(resultSets.values, exportDirectory.resolve("reviews.csv"))
        println("Reviews are written to file://$path")
    }
}
