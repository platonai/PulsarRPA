package `fun`.platonic.pulsar.ql.h2.udfs

import `fun`.platonic.pulsar.common.UrlUtil
import `fun`.platonic.pulsar.common.options.LoadOptions
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.annotation.UDFunction
import `fun`.platonic.pulsar.ql.h2.H2QueryEngine
import `fun`.platonic.pulsar.ql.h2.Queries
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import java.sql.ResultSet
import java.time.Duration

@UDFGroup(namespace = "META")
object MetadataFunctionTables {

    @UDFunction
    @JvmStatic
    fun load(@H2Context h2session: Session, configuredUrl: String): ResultSet {
        val page = H2QueryEngine.getSession(h2session).load(configuredUrl)
        return Queries.toResultSet(page)
    }

    @UDFunction
    @JvmStatic
    fun fetch(@H2Context h2session: Session, configuredUrl: String): ResultSet {
        val urlAndArgs = UrlUtil.splitUrlArgs(configuredUrl)
        val loadOptions = LoadOptions.parse(urlAndArgs.value)
        loadOptions.expires = Duration.ZERO

        val page = H2QueryEngine.getSession(h2session).load(urlAndArgs.key, loadOptions)
        return Queries.toResultSet(page)
    }

    @UDFunction
    @JvmStatic
    fun parse(@H2Context h2session: Session, configuredUrl: String): ResultSet {
        val urlAndArgs = UrlUtil.splitUrlArgs(configuredUrl)
        val loadOptions = LoadOptions.parse(urlAndArgs.value)
        loadOptions.isParse = true

        val page = H2QueryEngine.getSession(h2session).load(urlAndArgs.key, loadOptions)
        return Queries.toResultSet(page)
    }
}
