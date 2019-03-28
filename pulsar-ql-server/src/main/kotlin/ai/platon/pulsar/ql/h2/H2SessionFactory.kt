package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.ql.DbSession
import ai.platon.pulsar.ql.H2Config
import ai.platon.pulsar.ql.QueryEngine
import ai.platon.pulsar.ql.QuerySession
import org.h2.engine.ConnectionInfo
import org.h2.engine.Mode
import org.h2.engine.Session
import org.h2.engine.SysProperties
import org.h2.message.TraceSystem
import org.h2.util.JdbcUtils
import org.h2.util.Utils
import org.slf4j.LoggerFactory

object H2SessionFactory : org.h2.engine.SessionFactory {

    private val log = LoggerFactory.getLogger(H2SessionFactory::class.java)

    init {
        H2Config.config()
        SysProperties.serializeJavaObject = true
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

    /**
     * Create a h2 session and a associated query session, the h2 session is returned to h2 runtime
     *
     * @param ci The h2 connection info
     * @return The h2 session
     */
    @Synchronized
    override fun createSession(ci: ConnectionInfo): Session {
        val h2session = org.h2.engine.Engine.getInstance().createSession(ci)
        h2session.database.mode = Mode.getInstance(Mode.SIGMA)

        val h2Log = LoggerFactory.getLogger("org.h2")
        if (h2Log.isTraceEnabled) {
            h2session.trace.setLevel(TraceSystem.ADAPTER)
        }

        val querySession = QueryEngine.createQuerySession(DbSession(h2session.id, h2session))
        require(querySession.id == h2session.id)

        log.info("QuerySession {} is created for h2session <{}>, connection: <{}>",
                querySession, h2session, ci.url)

        return h2session
    }

    @Synchronized
    fun getSession(sessionId: Int): QuerySession {
        return QueryEngine.getSession(sessionId)
    }

    @Synchronized
    override fun closeSession(sessionId: Int) {
        QueryEngine.closeSession(sessionId)
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
