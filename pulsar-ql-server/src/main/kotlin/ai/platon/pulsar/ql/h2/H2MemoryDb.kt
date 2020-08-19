package ai.platon.pulsar.ql.h2

import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.math.abs

/**
 * The base class for all tests.
 */
class H2MemoryDb(val conf: H2DbConfig = H2DbConfig()) {

    /**
     * Open a database with a random named connection in admin mode.
     * The default user name and password is used.
     *
     * @param name the database name
     * @return the connection
     */
    fun getRandomConnection() = getConnection(generateTempDbName())

    /**
     * Open a database connection in admin mode. The default user name and
     * password is used.
     *
     * @param name the database name
     * @return the connection
     */
    fun getConnection(name: String) = getConnection0(buildURL(name, true), conf.user, conf.password)

    private fun generateTempDbName(): String {
        return "" + System.currentTimeMillis() + "_" + abs(Random().nextInt());
    }

    /**
     * Get the database URL for the given database name using the current
     * confuration options.
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
        return DriverManager.getConnection(url, user, password)
    }
}
