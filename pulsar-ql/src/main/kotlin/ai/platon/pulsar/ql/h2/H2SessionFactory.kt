package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.ql.*
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.SQLSession
import org.h2.api.ErrorCode
import org.h2.engine.*
import org.h2.jdbc.JdbcConnection
import org.h2.message.DbException
import org.h2.message.TraceSystem
import org.h2.util.JdbcUtils
import org.h2.util.Utils
import org.slf4j.LoggerFactory
import java.sql.Connection

object H2SessionFactory : org.h2.engine.SessionFactory {

    private val log = LoggerFactory.getLogger(H2SessionFactory::class.java)!!

    private val sqlContext get() = SQLContexts.activate()

    init {
        H2Config.config()
        JdbcUtils.addClassFactory(ClassFactory())
    }

    /**
     * Required by h2 database runtime
     * */
    @Suppress("unused")
    @JvmStatic
    fun getInstance(): H2SessionFactory {
        return H2SessionFactory
    }

    fun isColumnRetrieval(conn: Connection): Boolean {
        return Constants.CONN_URL_COLUMNLIST in conn.metaData.url
    }

    /**
     * Create a h2 session and a associated query session, the h2 session is returned to h2 runtime
     *
     * @param ci The h2 connection info
     * @return The h2 session
     */
    @Synchronized
    override fun createSession(ci: ConnectionInfo): Session {
        if (!sqlContext.isActive) {
            throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, "SQL context is closed")
        }

        log.debug("Creating SQL session for h2 connection | {}", ci.url)

        val h2session = org.h2.engine.Engine.getInstance().createSession(ci)
        SysProperties.serializeJavaObject = ci.isPersistent

        val h2Log = LoggerFactory.getLogger("org.h2")
        if (h2Log.isTraceEnabled) {
            h2session.trace.setLevel(TraceSystem.ADAPTER)
        }

        val sqlSession = sqlContext.createSession(H2SessionDelegate(h2session.serialId, h2session))
        sqlSession.sessionConfig.set(CapabilityTypes.SCENT_EXTRACT_TABULATE_CELL_TYPE, "DATABASE")
        require(sqlSession.id == h2session.serialId)

        log.info("SQLSession {} is created for h2session <{}>, connection: <{}>",
            sqlSession, h2session, ci.url)

        return h2session
    }

    @Synchronized
    fun getSession(serialId: Int): SQLSession {
        return sqlContext.getSession(serialId)
    }

    @Synchronized
    fun getSession(connection: Connection): SQLSession {
        val conn = connection as JdbcConnection
        return getSession(conn.session)
    }

    @Synchronized
    fun getH2Session(connection: Connection): Session {
        val conn = connection as JdbcConnection
        val s = getSession(conn.session)
        return s.sessionDelegate.implementation as Session
    }

    @Synchronized
    fun getSession(sessionInterface: SessionInterface): SQLSession {
        val h2session = sessionInterface as Session
        // TODO: or just use hash code so no need to modify h2database to expose serialId
        return sqlContext.getSession(h2session.serialId)
    }

    @Synchronized
    override fun closeSession(serialId: Int) {
        sqlContext.closeSession(serialId)
    }

    @Synchronized
    fun shutdown() {
        sqlContext.close()
        PulsarContexts.shutdown()
    }

    /**
     * @see org.h2.engine.SessionRemote.shutdownSessionFactory
     * */
    @Synchronized
    fun shutdownNow() {
        sqlContext.close()
        PulsarContexts.shutdown()
    }
}

class ClassFactory : Utils.ClassFactory {
    override fun match(name: String): Boolean {
        return name.startsWith(this.javaClass.`package`.name)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        return this.javaClass.classLoader.loadClass(name)
    }
}
