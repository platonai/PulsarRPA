package `fun`.platonic.pulsar.ql.h2

import `fun`.platonic.pulsar.ql.DbSession
import `fun`.platonic.pulsar.ql.H2Config
import `fun`.platonic.pulsar.ql.QueryEngine
import `fun`.platonic.pulsar.ql.QuerySession
import org.h2.engine.ConnectionInfo
import org.h2.engine.Mode
import org.h2.engine.Session
import org.h2.message.TraceSystem
import org.h2.util.JdbcUtils
import org.h2.util.Utils
import org.slf4j.LoggerFactory

object H2QueryEngine : org.h2.engine.SessionFactory {

    val LOG = LoggerFactory.getLogger(H2QueryEngine::class.java)

    init {
        H2Config.config()
        JdbcUtils.addClassFactory(ClassFactory())
    }

    /**
     * Required by h2 database runtime
     * */
    @Suppress("unused")
    @JvmStatic
    fun getInstance(): H2QueryEngine {
        return H2QueryEngine
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

        val querySession = QueryEngine.createQuerySession(DbSession(h2session))

        LOG.info("QuerySession {} is created for h2session <{}>, connection: <{}>",
                querySession, h2session, ci.url)

        return h2session
    }

    fun getSession(dbSession: DbSession): QuerySession {
        return QueryEngine.getSession(dbSession)
    }

    fun getSession(h2session: Session): QuerySession {
        return QueryEngine.getSession(DbSession(h2session))
    }

    override fun closeSession(sessionId: Int) {}
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
