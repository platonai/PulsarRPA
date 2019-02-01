package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2QueryEngine
import ai.platon.pulsar.ql.h2.Queries
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
        val urlAndArgs = ai.platon.pulsar.common.UrlUtil.splitUrlArgs(configuredUrl)
        val loadOptions = ai.platon.pulsar.common.options.LoadOptions.parse(urlAndArgs.value)
        loadOptions.expires = Duration.ZERO

        val page = H2QueryEngine.getSession(h2session).load(urlAndArgs.key, loadOptions)
        return Queries.toResultSet(page)
    }

    @UDFunction
    @JvmStatic
    fun parse(@H2Context h2session: Session, configuredUrl: String): ResultSet {
        val urlAndArgs = ai.platon.pulsar.common.UrlUtil.splitUrlArgs(configuredUrl)
        val loadOptions = ai.platon.pulsar.common.options.LoadOptions.parse(urlAndArgs.value)
        loadOptions.isParse = true

        val page = H2QueryEngine.getSession(h2session).load(urlAndArgs.key, loadOptions)
        return Queries.toResultSet(page)
    }
}
