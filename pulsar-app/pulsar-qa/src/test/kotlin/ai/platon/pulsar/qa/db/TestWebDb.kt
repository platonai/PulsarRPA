package ai.platon.pulsar.qa.db

import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.ql.context.AbstractSQLContext
import ai.platon.pulsar.ql.context.SQLContexts
import org.junit.Test

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestWebDb {
    private val context = SQLContexts.create() as AbstractSQLContext
    private val webDB = context.webDb

    @Test
    fun testDbQuery() {
        val query = DbQuery()
        webDB.query(query).asSequence().forEach {
            println(it.url)
        }
    }
}
