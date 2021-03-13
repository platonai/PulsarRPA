package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.h2.engine.Session
import ai.platon.pulsar.ql.annotation.H2Context
import java.sql.Connection

@UDFGroup(namespace = "META")
object MetadataFunctions {

    @UDFunction(description = "Get a page specified by url from the database, return the formatted page as a string")
    @JvmStatic
    fun get(@H2Context conn: Connection, url: String): String {
        val page = H2SessionFactory.getSession(conn).load(url)
        return WebPageFormatter(page).toString()
    }
}
