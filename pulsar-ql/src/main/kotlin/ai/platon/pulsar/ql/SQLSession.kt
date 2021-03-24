package ai.platon.pulsar.ql

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.types.ValueDom
import org.h2.engine.SessionInterface
import java.sql.Connection
import java.sql.ResultSet

interface SQLSession : PulsarSession {

    fun parseValueDom(page: WebPage): ValueDom

    fun isColumnRetrieval(conn: Connection): Boolean

    fun registerDefaultUdfs(session: SessionInterface)

    fun registerUdfsInPackage(session: SessionInterface, classLoader: ClassLoader, packageName: String)

    fun registerUdfs(session: SessionInterface, udfClass: Class<out Any>)

    fun execute(sql: String): ResultSet?
}
