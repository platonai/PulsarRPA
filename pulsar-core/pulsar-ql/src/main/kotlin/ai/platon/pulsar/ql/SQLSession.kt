package ai.platon.pulsar.ql

import ai.platon.pulsar.skeleton.session.PulsarSession
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.common.types.ValueDom
import org.h2.engine.SessionInterface
import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * A SQL session enables the X-SQL execution.
 * */
interface SQLSession : PulsarSession {
    /**
     * The SQL context
     * */
    val sqlContext get() = context as SQLContext

    /**
     * The session delegate which provides implementation.
     * */
    val sessionDelegate: SessionDelegate

    /**
     * UDF class samples used to detect all other UDF classes.
     * */
    val udfClassSamples: Set<KClass<out Any>>

    /**
     * All registered UDF classes.
     * */
    val registeredAllUdfClasses: Set<Class<out Any>>

    /**
     * All registered admin UDF classes.
     * */
    val registeredAdminUdfClasses
        get() = registeredAllUdfClasses
            .filter { it.annotations.any { it is UDFGroup && it.namespace == "ADMIN" } }
            .toSet()

    /**
     * All registered non-admin UDF classes.
     * */
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
