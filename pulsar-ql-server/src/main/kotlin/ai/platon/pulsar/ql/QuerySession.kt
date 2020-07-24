package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.annotation.UDAggregation
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.udas.GroupCollect
import ai.platon.pulsar.ql.h2.udas.GroupFetch
import ai.platon.pulsar.ql.h2.udfs.CommonFunctions
import ai.platon.pulsar.ql.types.ValueDom
import com.google.common.reflect.ClassPath
import org.h2.engine.Constants
import org.h2.engine.SessionInterface
import java.sql.Connection
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

open class QuerySession(
        val pulsarContext: AbstractPulsarContext,
        val dbSession: DbSession,
        config: SessionConfig
): PulsarSession(pulsarContext, config, dbSession.id) {
    private var totalUdfs = AtomicInteger()
    private var totalUdas = AtomicInteger()

    val registeredAllUdfClasses = mutableListOf<Class<out Any>>()
    val registeredAdminUdfClasses get() = registeredAllUdfClasses.filter {
        it.annotations.any { it is UDFGroup && it.namespace == "ADMIN" }
    }
    val registeredUdfClasses get() = registeredAllUdfClasses.filterNot {
        it in registeredAdminUdfClasses
    }

    init {
        if (dbSession.implementation is org.h2.engine.Session) {
            synchronized(QuerySession::class.java) {
                registerDefaultUdfs(dbSession.implementation)
                registerUdaf(dbSession.implementation, GroupCollect::class)
                registerUdaf(dbSession.implementation, GroupFetch::class)
            }
        }
    }

    fun parseValueDom(page: WebPage): ValueDom {
        return ValueDom.get(parse(page))
    }

    fun isColumnRetrieval(conn: Connection): Boolean {
        return Constants.CONN_URL_COLUMNLIST in conn.metaData.url
    }

    /**
     * Register user defined functions into database
     */
    @Synchronized
    fun registerDefaultUdfs(session: SessionInterface) {
        val udfClasses = ClassPath.from(CommonFunctions.javaClass.classLoader)
                .getTopLevelClasses(CommonFunctions.javaClass.`package`.name)
                .map { it.load() }
                .filter { it.annotations.any { it is UDFGroup } }

        registeredAllUdfClasses.addAll(udfClasses)
        registeredAllUdfClasses.forEach { registerUdfs(session, it.kotlin) }

        if (totalUdfs.get() > 0) {
            log.debug("Added total {} new UDFs for session {}", totalUdfs, session)
        }
    }

    @Synchronized
    fun registerUdfsInPackage(session: SessionInterface, classLoader: ClassLoader, packageName: String) {
        val udfClasses = ClassPath.from(classLoader)
                .getTopLevelClasses(packageName)
                .map { it.load() }
                .filter { it.annotations.any { it is UDFGroup } }

        registeredAllUdfClasses.addAll(udfClasses)
        registeredAllUdfClasses.forEach { registerUdfs(session, it.kotlin) }
    }

    /**
     * Register a java UDF class
     * */
    @Synchronized
    fun registerUdfs(session: SessionInterface, udfClass: Class<out Any>) {
        return registerUdfs(session, udfClass.kotlin)
    }

    /**
     * Register a kotlin UDF class
     * */
    private fun registerUdfs(session: SessionInterface, udfClass: KClass<out Any>) {
        val group = udfClass.annotations.first { it is UDFGroup } as UDFGroup
        val namespace = group.namespace

        // register shortcuts if required
        udfClass.members
                .filter { it.annotations.any { a -> a is UDFunction && a.hasShortcut } }
                .forEach { registerUdf(session, udfClass, it.name) }

        // register udfs
        udfClass.members
                .filter { it.annotations.any { a -> a is UDFunction } }
                .forEach { registerUdf(session, udfClass, it.name, namespace) }
    }

    /**
     * Register user defined aggregation functions
     * We support arbitrary "_" in a UDF name, for example, the following functions are the same:
     * some_fun_(), _____some_fun_(), some______fun()
     */
    fun registerUdaf(session: SessionInterface, udafClass: KClass<out org.h2.api.Aggregate>) {
        val group = udafClass.annotations.first { it is UDFGroup } as UDFGroup
        val aggregation = udafClass.annotations.first { it is UDAggregation } as UDAggregation
        val namespace = group.namespace
        val name = aggregation.name

        var aggregateName = if (namespace.isEmpty()) name else namespace + "_" + name
        aggregateName = aggregateName.replace("_", "").toUpperCase()

        var sql = "DROP AGGREGATE IF EXISTS $aggregateName"
        var command = session.prepareCommand(sql, Int.MAX_VALUE)
        command.executeUpdate(null)

        // Notice : can not use session.prepare(sql) here, which causes a call cycle
        sql = "CREATE AGGREGATE IF NOT EXISTS $aggregateName FOR \"${udafClass.qualifiedName}\""
        command = session.prepareCommand(sql, Int.MAX_VALUE)
        command.executeUpdate(null)

        totalUdas.incrementAndGet()

        if (log.isTraceEnabled) {
            log.trace(sql)
        }
    }

    /**
     * Register user defined functions
     * We support arbitrary "_" in a UDF name, for example, the following functions are the same:
     * some_fun_(), _____some_fun_(), some______fun()
     */
    private fun registerUdf(session: SessionInterface, udfClass: KClass<out Any>, method: String, namespace: String = "") {
        var alias = if (namespace.isEmpty()) method else namespace + "_" + method

        // All underscores are ignored
        alias = alias.replace("_", "").toUpperCase()

        var sql = "DROP ALIAS IF EXISTS $alias"
        println(sql)
        // Notice : can not use session.prepare(sql) here, which causes a call cycle
        var command = session.prepareCommand(sql, Int.MAX_VALUE)
        command.executeUpdate(null)

        // Notice : can not use session.prepare(sql) here, which causes a call cycle
        sql = "CREATE ALIAS IF NOT EXISTS $alias FOR \"${udfClass.qualifiedName}.$method\""
        command = session.prepareCommand(sql, Int.MAX_VALUE)
        command.executeUpdate(null)

        totalUdfs.incrementAndGet()

        if (log.isTraceEnabled) {
            log.trace(sql)
        }
    }
}
