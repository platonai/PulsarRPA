package ai.platon.pulsar.ql.context

import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.SessionDelegate
import org.h2.engine.SessionInterface
import java.sql.Connection
import java.sql.ResultSet
import kotlin.Throws

interface SQLContext: PulsarContext {
    
    @Throws(Exception::class)
    fun createSession(sessionDelegate: SessionDelegate): SQLSession
    
    @Throws(Exception::class)
    fun getSession(sessionInterface: SessionInterface): SQLSession

    fun sessionCount(): Int
    
    @Throws(Exception::class)
    fun getSession(sessionId: Int): SQLSession
    
    @Throws(Exception::class)
    fun closeSession(sessionId: Int)

    @Throws(Exception::class)
    fun execute(sql: String)

    @Throws(Exception::class)
    fun executeQuery(sql: String): ResultSet
    
    @Throws(Exception::class)
    fun run(block: (Connection) -> Unit)

    @Throws(Exception::class)
    fun runQuery(block: (Connection) -> ResultSet): ResultSet
}
