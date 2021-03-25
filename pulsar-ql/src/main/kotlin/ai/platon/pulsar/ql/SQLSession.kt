package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.types.ValueDom
import org.h2.engine.SessionInterface
import java.sql.Connection

interface SQLSession : PulsarSession {
    val sessionDelegate: SessionDelegate

    val registeredAllUdfClasses: List<Class<out Any>>

    val registeredAdminUdfClasses
        get() = registeredAllUdfClasses.filter { it.annotations.any { it is UDFGroup && it.namespace == "ADMIN" } }

    val registeredUdfClasses
        get() = registeredAllUdfClasses.filterNot { it in registeredAdminUdfClasses }

    fun parseValueDom(page: WebPage): ValueDom

    fun isColumnRetrieval(conn: Connection): Boolean

    fun registerDefaultUdfs(session: SessionInterface)

    fun registerUdfsInPackage(session: SessionInterface, classLoader: ClassLoader, packageName: String)

    fun registerUdfs(session: SessionInterface, udfClass: Class<out Any>)
}
