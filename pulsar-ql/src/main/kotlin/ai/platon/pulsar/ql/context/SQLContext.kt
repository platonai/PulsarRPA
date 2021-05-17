package ai.platon.pulsar.ql.context

import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.SessionDelegate
import org.h2.engine.SessionInterface
import java.sql.Connection
import java.sql.ResultSet
import kotlin.jvm.Throws

interface SQLContext: PulsarContext {
    val isActive: Boolean

    fun createSession(sessionDelegate: SessionDelegate): SQLSession

    fun getSession(sessionInterface: SessionInterface): SQLSession

    fun sessionCount(): Int

    fun getSession(sessionId: Int): SQLSession

    fun closeSession(sessionId: Int)

    fun execute(sql: String)

    @Throws(Exception::class)
    fun executeQuery(sql: String): ResultSet

    fun run(block: (Connection) -> Unit)

    @Throws(Exception::class)
    fun runQuery(block: (Connection) -> ResultSet): ResultSet
}
