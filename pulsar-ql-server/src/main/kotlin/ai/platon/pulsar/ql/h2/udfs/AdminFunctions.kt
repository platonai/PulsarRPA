package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.ql.SQLContext
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.slf4j.LoggerFactory

@UDFGroup(namespace = "ADMIN")
object AdminFunctions {
    val log = LoggerFactory.getLogger(AdminFunctions::class.java)
    private val sqlContext = SQLContext.getOrCreate()

    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context h2session: Session, message: String): String {
        return message
    }

    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context h2session: Session, message: String, message2: String): String {
        return "$message, $message2"
    }

    @UDFunction
    @JvmStatic
    fun print(message: String) {
        println(message)
    }

    @UDFunction
    @JvmStatic
    fun sessionCount(@H2Context h2session: Session): Int {
        checkPrivilege(h2session)
        return sqlContext.sessionCount()
    }

    @UDFunction
    @JvmStatic
    fun closeSession(@H2Context h2session: Session): String {
        checkPrivilege(h2session)
        H2SessionFactory.closeSession(h2session.serialId)
        return h2session.toString()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun save(@H2Context h2session: Session, url: String, postfix: String = ".htm"): String {
        checkPrivilege(h2session)
        val page = H2SessionFactory.getSession(h2session.serialId).load(url)
        val path = AppPaths.WEB_CACHE_DIR.resolve(AppPaths.fromUri(url, "", postfix))
        return AppFiles.saveTo(page, path).toString()
    }

    private fun checkPrivilege(h2session: Session) {

    }
}
