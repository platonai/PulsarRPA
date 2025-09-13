package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.ql.SessionDelegate

/**
 * DbSession is a wrapper for underlying database session, it is a bridge between database session and pulsar query
 * session
 */
class H2SessionDelegate(
    id: Int,
    val h2session: org.h2.engine.Session
) : SessionDelegate(id, h2session) {
    override val sqlSequence: Int
        get() = h2session.commandSequence
    override var name = h2session.toString()
}
