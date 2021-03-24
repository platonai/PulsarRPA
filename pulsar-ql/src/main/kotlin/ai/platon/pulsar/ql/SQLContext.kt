package ai.platon.pulsar.ql

import ai.platon.pulsar.context.PulsarContext
import org.h2.engine.SessionInterface
import java.sql.ResultSet

interface SQLContext: PulsarContext {
    val isActive: Boolean

    fun createSession(sessionDelegate: SessionDelegate): SQLSession

    fun getSession(sessionInterface: SessionInterface): SQLSession

    fun sessionCount(): Int

    fun getSession(sessionId: Int): SQLSession

    fun closeSession(sessionId: Int)

    fun execute(sql: String)

    fun executeQuery(sql: String): ResultSet
}
