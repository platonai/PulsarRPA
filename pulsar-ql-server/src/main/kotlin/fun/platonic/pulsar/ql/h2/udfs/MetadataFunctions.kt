package `fun`.platonic.pulsar.ql.h2.udfs

import `fun`.platonic.pulsar.persist.WebPageFormatter
import `fun`.platonic.pulsar.ql.DbSession
import `fun`.platonic.pulsar.ql.annotation.UDFGroup
import `fun`.platonic.pulsar.ql.annotation.UDFunction
import `fun`.platonic.pulsar.ql.h2.H2QueryEngine
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context

@UDFGroup(namespace = "META")
object MetadataFunctions {

    @UDFunction
    @JvmStatic
    operator fun get(@H2Context h2session: Session, url: String): String {
        val page = H2QueryEngine.getSession(DbSession(h2session)).load(url)
        return WebPageFormatter(page).toString()
    }
}
