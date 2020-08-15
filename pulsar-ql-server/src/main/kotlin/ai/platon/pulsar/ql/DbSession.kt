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

    override fun hashCode() = id

    override fun toString() = name

    override fun equals(other: Any?) = other is DbSession && other.id == id
}
