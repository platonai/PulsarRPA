package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.ql.SQLContexts
import ai.platon.pulsar.ql.annotation.H2Context
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.slf4j.LoggerFactory
import java.sql.Connection

@UDFGroup(namespace = "ADMIN")
object AdminFunctions {
    val log = LoggerFactory.getLogger(AdminFunctions::class.java)
    private val sqlContext get() = SQLContexts.activate()

    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context conn: Connection, message: String): String {
        return message
    }

    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context conn: Connection, message: String, message2: String): String {
        return "$message, $message2"
    }

    @UDFunction
    @JvmStatic
    fun print(message: String) {
        println(message)
    }

    @UDFunction
    @JvmStatic
    fun sessionCount(@H2Context conn: Connection): Int {
        return sqlContext.sessionCount()
    }

    @UDFunction
    @JvmStatic
    fun closeSession(@H2Context conn: Connection): String {
        val h2session = H2SessionFactory.getH2Session(conn)
        H2SessionFactory.closeSession(h2session.serialId)
        return h2session.toString()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun save(@H2Context conn: Connection, url: String, postfix: String = ".htm"): String {
        val page = H2SessionFactory.getSession(conn).load(url)
        val path = AppPaths.WEB_CACHE_DIR.resolve(AppPaths.fromUri(url, "", postfix))
        return AppFiles.saveTo(page, path).toString()
    }
}
