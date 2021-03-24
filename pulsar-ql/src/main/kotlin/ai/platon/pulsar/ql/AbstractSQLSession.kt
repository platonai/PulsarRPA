package ai.platon.pulsar.ql

import ai.platon.pulsar.AbstractPulsarSession
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.annotation.UDAggregation
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2MemoryDb
import ai.platon.pulsar.ql.h2.udas.GroupCollect
import ai.platon.pulsar.ql.h2.udas.GroupFetch
import ai.platon.pulsar.ql.h2.udfs.CommonFunctions
import ai.platon.pulsar.ql.types.ValueDom
import com.google.common.reflect.ClassPath
import org.h2.engine.Constants
import org.h2.engine.SessionInterface
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

abstract class AbstractSQLSession(
    context: AbstractPulsarContext,
    val sessionDelegate: SessionDelegate,
    config: SessionConfig
) : AbstractPulsarSession(context, config, sessionDelegate.id), SQLSession {
    private val log = LoggerFactory.getLogger(AbstractSQLSession::class.java)

    private var totalUdfs = AtomicInteger()
    private var totalUdas = AtomicInteger()

    val registeredAllUdfClasses = mutableListOf<Class<out Any>>()
    val registeredAdminUdfClasses
        get() = registeredAllUdfClasses.filter {
            it.annotations.any { it is UDFGroup && it.namespace == "ADMIN" }
        }
    val registeredUdfClasses
        get() = registeredAllUdfClasses.filterNot {
            it in registeredAdminUdfClasses
        }

    private val db = H2MemoryDb()
    private val randomConnection get() = db.takeIf { isActive }?.getRandomConnection()
    private val connectionPool = ArrayBlockingQueue<Connection>(1000)

    init {
        if (sessionDelegate.implementation is org.h2.engine.Session) {
            synchronized(AbstractSQLSession::class.java) {
                registerDefaultUdfs(sessionDelegate.implementation)
                registerUdaf(sessionDelegate.implementation, GroupCollect::class)
                registerUdaf(sessionDelegate.implementation, GroupFetch::class)
            }
        }
    }

    override fun parseValueDom(page: WebPage): ValueDom {
        return ValueDom.get(parse(page))
    }

    override fun isColumnRetrieval(conn: Connection): Boolean {
        return Constants.CONN_URL_COLUMNLIST in conn.metaData.url
    }

    /**
     * Register user defined functions into database
     */
    @Synchronized
    override fun registerDefaultUdfs(session: SessionInterface) {
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

    override fun execute(sql: String): ResultSet? {
        val conn = connectionPool.poll() ?: randomConnection ?: return null
        if (conn.isClosed) {
            return null
        }

        try {
            val st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
            return st.executeQuery(sql)
        } catch (e: SQLException) {
            throw e
        } finally {
            if (!conn.isClosed) {
                connectionPool.add(conn)
            }
        }
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
    private fun registerUdf(
        session: SessionInterface,
        udfClass: KClass<out Any>,
        method: String,
        namespace: String = ""
    ) {
        var alias = if (namespace.isEmpty()) method else namespace + "_" + method

        // All underscores are ignored
        alias = alias.replace("_", "").toUpperCase()

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
