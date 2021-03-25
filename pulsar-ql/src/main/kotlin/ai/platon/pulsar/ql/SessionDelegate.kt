package ai.platon.pulsar.ql

/**
 * DbSession is a wrapper for underlying database session, it is a bridge between database session and pulsar query
 * session
 */
abstract class SessionDelegate(val id: Int, val implementation: Any) {
    open val sqlSequence: Int = 0
    open val name: String = "(session delegate)"

    override fun hashCode() = id

    override fun toString() = name

    override fun equals(other: Any?) = other is SessionDelegate && other.id == id
}
