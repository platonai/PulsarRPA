package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.getLogger
import org.h2.jdbc.JdbcSQLException
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * The H2 memory DB
 */
class H2MemoryDb(val conf: H2DbConfig = H2DbConfig()): AutoCloseable {

    private val connections = ConcurrentHashMap<String, Connection>()

    /**
     * Open a database with a random named connection in admin mode.
     * The default username and password is used.
     *
     * @return the connection
     */
    @Throws(JdbcSQLException::class)
    fun getRandomConnection() = getConnection(generateTempDbName())

    /**
     * Open a database with a random named connection in admin mode.
     * The default user name and password is used.
     *
     * @return the connection
     */
    @Throws(JdbcSQLException::class)
    fun getRandomConnectionOrNull() = getConnectionOrNull(generateTempDbName())

    /**
     * Open a database connection in admin mode. The default user name and
     * password is used.
     *
     * @param name the database name
     * @return the connection
     */
    @Throws(JdbcSQLException::class)
    fun getConnection(name: String) = getConnection0(buildURL(name, true), conf.user, conf.password)

    /**
     * Open a database connection in admin mode. The default username and
     * password is used.
     *
     * @param name the database name
     * @return the connection
     */
    @Throws(JdbcSQLException::class)
    fun getConnectionOrNull(name: String) = kotlin.runCatching { getConnection(name) }
            .onFailure { getLogger(H2MemoryDb::class).warn(it.brief()) }
            .getOrNull()

    override fun close() {
        connections.values.forEach { it.close() }
        connections.clear()
    }

    private fun generateTempDbName(): String {
        return "" + System.currentTimeMillis() + "_" + abs(Random().nextInt());
    }

    /**
     * Get the database URL for the given database name using the current
     * configuration options.
     *
     * @param name the database name
     * @param admin true if the current user is an admin
     * @return the database URL
     */
    private fun buildURL(name: String, admin: Boolean): String {
        return "jdbc:h2:mem:$name"
    }

    private fun getConnection0(url: String, user: String, password: String): Connection {
        org.h2.Driver.load()
        val connection = DriverManager.getConnection(url, user, password)
        val key = "$url$user$password"
        connections[key] = connection
        return connection
    }
}
