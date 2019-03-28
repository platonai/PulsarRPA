package ai.platon.pulsar.ql

/**
 * DbSession is a wrapper for underlying database session, it is a bridge between database session and pulsar query
 * session
 */
class DbSession(val id: Int, val implementation: Any) {
    var sqlSequence: Int = 0
        get() {
            if (org.h2.engine.Session::class.java.isAssignableFrom(implementation.javaClass)) {
                val session = implementation as org.h2.engine.Session
                this.sqlSequence = session.commandSequence
            }

            return field
        }
    var name: String = ""
        private set

    init {
        if (org.h2.engine.Session::class.java.isAssignableFrom(implementation.javaClass)) {
            val session = implementation as org.h2.engine.Session
            name = session.toString()
        }
    }

    fun executeUpdate(sql: String): Int {
        if (org.h2.engine.Session::class.java.isAssignableFrom(implementation.javaClass)) {
            val session = implementation as org.h2.engine.Session
            val command = session.prepareCommand(sql)
            return command.executeUpdate()
        }

        return 0
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return name
    }

    override fun equals(obj: Any?): Boolean {
        return obj is ai.platon.pulsar.ql.DbSession && obj.id == id
    }
}
