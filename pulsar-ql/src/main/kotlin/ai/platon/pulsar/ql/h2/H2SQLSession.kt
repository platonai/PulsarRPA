package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.ql.AbstractSQLSession
import ai.platon.pulsar.ql.SessionConfig
import ai.platon.pulsar.ql.common.annotation.UDAggregation
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.h2.udas.GroupCollect
import ai.platon.pulsar.ql.h2.udas.GroupFetch
import ai.platon.pulsar.ql.h2.udfs.CommonFunctions
import com.google.common.reflect.ClassPath
import org.h2.api.Aggregate
import org.h2.engine.Constants
import org.h2.engine.SessionInterface
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class H2SQLSession(
    context: AbstractPulsarContext,
    sessionDelegate: H2SessionDelegate,
    config: SessionConfig
) : AbstractSQLSession(context, sessionDelegate, config) {
    private val log = LoggerFactory.getLogger(H2SQLSession::class.java)

    private var totalUdfs = AtomicInteger()
    private var totalUdas = AtomicInteger()
    private val closed = AtomicBoolean()

    init {
        synchronized(AbstractSQLSession::class.java) {
            udfClassSamples.add(CommonFunctions::class)

            registerDefaultUdfs(sessionDelegate.h2session)
            registerUdaf(sessionDelegate.h2session, GroupCollect::class)
            registerUdaf(sessionDelegate.h2session, GroupFetch::class)
        }
    }

    override fun isColumnRetrieval(conn: Connection): Boolean {
        return Constants.CONN_URL_COLUMNLIST in conn.metaData.url
    }

    /**
     * Register user defined functions into database
     */
    @Synchronized
    override fun registerDefaultUdfs(session: SessionInterface) {
        val udfClasses = udfClassSamples.flatMap { loadTopLevelClasses(it) }
            .filter { it.annotations.any { it is UDFGroup } }

        registeredAllUdfClasses.addAll(udfClasses)
        registeredAllUdfClasses.forEach { registerUdfs(session, it.kotlin) }

        if (totalUdfs.get() > 0) {
            log.debug("Added total {} new UDFs for session {}", totalUdfs, session)
        }
    }

    @Synchronized
    override fun registerUdfsInPackage(session: SessionInterface, classLoader: ClassLoader, packageName: String) {
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
    override fun registerUdfs(session: SessionInterface, udfClass: Class<out Any>) {
        return registerUdfs(session, udfClass.kotlin)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                val h2session = sessionDelegate.implementation as org.h2.engine.Session
                h2session.close()
            } catch (t: Throwable) {
                warnForClose(this, t)
            }
        }
    }

    private fun <T: Any> loadTopLevelClasses(clazz: KClass<T>): List<Class<*>> {
        return ClassPath.from(clazz.java.classLoader)
            .getTopLevelClasses(clazz.java.`package`.name)
            .map { it.load() }
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
    fun registerUdaf(session: SessionInterface, udafClass: KClass<out Aggregate>) {
        val group = udafClass.annotations.first { it is UDFGroup } as UDFGroup
        val aggregation = udafClass.annotations.first { it is UDAggregation } as UDAggregation
        val namespace = group.namespace
        val name = aggregation.name

        var aggregateName = if (namespace.isEmpty()) name else namespace + "_" + name
        aggregateName = aggregateName.replace("_", "").uppercase(Locale.getDefault())

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
    private fun registerUdf(
        session: SessionInterface,
        udfClass: KClass<out Any>,
        method: String,
        namespace: String = ""
    ) {
        var alias = if (namespace.isEmpty()) method else namespace + "_" + method

        // All underscores are ignored
        alias = alias.replace("_", "").uppercase(Locale.getDefault())

        var sql = "DROP ALIAS IF EXISTS $alias"
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
