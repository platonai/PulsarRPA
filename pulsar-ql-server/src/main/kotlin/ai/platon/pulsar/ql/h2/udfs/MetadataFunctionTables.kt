package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import ai.platon.pulsar.ql.h2.Queries
import org.h2.engine.Session
import ai.platon.pulsar.ql.annotation.H2Context
import java.sql.Connection
import java.sql.ResultSet
import java.time.Duration

@UDFGroup(namespace = "META")
object MetadataFunctionTables {

    @UDFunction(description = "Load a page specified by url from the database, " +
            "return the fields of the page as key-value pairs")
    @JvmStatic
    fun load(@H2Context conn: Connection, configuredUrl: String): ResultSet {
        val page = H2SessionFactory.getSession(conn).load(configuredUrl)
        return Queries.toResultSet(page)
    }

    @UDFunction(description = "Load a page specified by url from the database, " +
            "fetch it from the internet if absent or expired" +
            "return the fields of the page as key-value pairs")
    @JvmStatic
    fun fetch(@H2Context conn: Connection, configuredUrl: String): ResultSet {
        val urlAndArgs = Urls.splitUrlArgs(configuredUrl)
        val loadOptions = LoadOptions.parse(urlAndArgs.second)
        loadOptions.expires = Duration.ZERO

        val page = H2SessionFactory.getSession(conn).load(urlAndArgs.first, loadOptions)
        return Queries.toResultSet(page)
    }
}
