package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.types.ValueDom
import org.h2.engine.SessionInterface
import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

interface SQLSession : PulsarSession {
    val sqlContext get() = context as SQLContext

    val sessionDelegate: SessionDelegate

    val udfClassSamples: Set<KClass<out Any>>

    val registeredAllUdfClasses: Set<Class<out Any>>

    val registeredAdminUdfClasses
        get() = registeredAllUdfClasses
            .filter { it.annotations.any { it is UDFGroup && it.namespace == "ADMIN" } }
            .toSet()

    val registeredUdfClasses
        get() = registeredAllUdfClasses
            .filterNot { it in registeredAdminUdfClasses }
            .toSet()

    fun parseValueDom(page: WebPage): ValueDom

    fun isColumnRetrieval(conn: Connection): Boolean

    fun registerDefaultUdfs(session: SessionInterface)

    fun registerUdfsInPackage(session: SessionInterface, classLoader: ClassLoader, packageName: String)

    fun registerUdfs(session: SessionInterface, udfClass: Class<out Any>)

    fun execute(sql: String)

    fun executeQuery(sql: String): ResultSet
}
