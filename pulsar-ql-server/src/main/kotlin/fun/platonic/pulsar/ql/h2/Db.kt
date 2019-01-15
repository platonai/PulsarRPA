package `fun`.platonic.pulsar.ql.h2

import `fun`.platonic.pulsar.ql.h2.DbConfig.baseDir
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * The base class for all tests.
 */
object Db {

    init {
        System.setProperty("h2.sessionFactory", H2QueryEngine::class.java.name)
    }

    fun addOption(url: String, option: String, value: String): String {
        var u = url
        if (u.indexOf(";$option=") < 0) {
            u += ";$option=$value"
        }
        return u
    }

    @Throws(SQLException::class)
    fun getConnectionInternal(url: String, user: String, password: String): Connection {
        println("H2 Connection: " + url)

        org.h2.Driver.load()
        return DriverManager.getConnection(url, user, password)
    }

    /**
     * Get the file password (only required if file encryption is used).
     *
     * @return the file password
     */
    val filePassword: String
        get() = "filePassword"

    /**
     * Get the login password. This is usually the user password. If file
     * encryption is used it is combined with the file password.
     *
     * @return the login password
     */
    val password: String
        get() = getPassword("sa")

    val user: String
        get() = "sa"

    /**
     * Get the classpath list used to execute java -cp ...
     *
     * @return the classpath list
     */
    val classPath: String
        get() = Paths.get("bin", "temp", ".").toString()

    /**
     * Open a database connection in admin mode. The default user name and
     * password is used.
     *
     * @param name the database name
     * @return the connection
     */
    fun getConnection(name: String): Connection {
        return getConnectionInternal(getURL(name, true), user, password)
    }

    /**
     * Open a database connection.
     *
     * @param name the database name
     * @param user the user name to use
     * @param password the password to use
     * @return the connection
     */
    fun getConnection(name: String, user: String, password: String): Connection {
        return getConnectionInternal(getURL(name, false), user, password)
    }

    /**
     * Get the password to use to login for the given user password. The file
     * password is added if required.
     *
     * @param userPassword the password of this user
     * @return the login password
     */
    fun getPassword(userPassword: String): String {
        return if (DbConfig.cipher == null)
            userPassword
        else
            filePassword + " " + userPassword
    }

    /**
     * Get the base directory for tests.
     * If a special file system is used, the prefix is prepended.
     *
     * @return the directory, possibly including file system prefix
     */
    fun getBaseDir(): String {
        var dir = baseDir

        if (DbConfig.reopen) {
            dir = "rec:memFS:" + dir
        }

        if (DbConfig.splitFileSystem) {
            dir = "split:16:" + dir
        }

        return dir
    }

    /**
     * Get the database URL for the given database name using the current
     * configuration options.
     *
     * @param name the database name
     * @param admin true if the current user is an admin
     * @return the database URL
     */
    fun getURL(name_: String, admin: Boolean): String {
        var name = name_
        var url: String
        if (name.startsWith("jdbc:")) {
            if (DbConfig.mvStore) {
                name = addOption(name, "MV_STORE", "true")
                // name = addOption(name, "MVCC", "true");
            }
            return name
        }
        if (admin) {
            name = addOption(name, "RETENTION_TIME", "10");
            name = addOption(name, "WRITE_DELAY", "10");
        }
        val idx = name.indexOf(':')
        if (idx == -1 && DbConfig.memory) {
            name = "mem:" + name
        } else {
            if (idx < 0 || idx > 10) {
                // index > 10 if in options
                name = getBaseDir() + "/" + name
            }
        }
        if (DbConfig.networked) {
            val port = DbConfig.port

            if (DbConfig.ssl) {
                url = "ssl://localhost:$port/$name"
            } else {
                url = "tcp://localhost:$port/$name"
            }
        } else if (DbConfig.googleAppEngine) {
            url = "gae://" + name +
                    ";FILE_LOCK=NO;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE"
        } else {
            url = name
        }
        if (DbConfig.mvStore) {
            url = addOption(url, "MV_STORE", "true")
            // url = addOption(url, "MVCC", "true");
        } else {
            url = addOption(url, "MV_STORE", "false")
        }
        if (!DbConfig.memory) {
            if (DbConfig.smallLog && admin) {
                url = addOption(url, "MAX_LOG_SIZE", "1")
            }
        }
        if (DbConfig.traceSystemOut) {
            url = addOption(url, "TRACE_LEVEL_SYSTEM_OUT", "2")
        }
        if (DbConfig.traceLevelFile > 0 && admin) {
            url = addOption(url, "TRACE_LEVEL_FILE", "" + DbConfig.traceLevelFile)
            url = addOption(url, "TRACE_MAX_FILE_SIZE", "8")
        }
        url = addOption(url, "LOG", "1")
        if (DbConfig.throttleDefault > 0) {
            url = addOption(url, "THROTTLE", "" + DbConfig.throttleDefault)
        } else if (DbConfig.throttle > 0) {
            url = addOption(url, "THROTTLE", "" + DbConfig.throttle)
        }
        url = addOption(url, "LOCK_TIMEOUT", "" + DbConfig.lockTimeout)
        if (DbConfig.diskUndo && admin) {
            url = addOption(url, "MAX_MEMORY_UNDO", "3")
        }
        if (DbConfig.lazy) {
            url = addOption(url, "LAZY_QUERY_EXECUTION", "1")
        }
        if (DbConfig.diskResult && admin) {
            url = addOption(url, "MAX_MEMORY_ROWS", "100")
            url = addOption(url, "CACHE_SIZE", "0")
        }
        if (DbConfig.defrag) {
            url = addOption(url, "DEFRAG_ALWAYS", "TRUE")
        }

        url = addOption(url, "MODE", "SIGMA")
        return "jdbc:h2:$url"
    }

    /**
     * Delete all database files for this database.
     *
     * @param name the database name
     */
    fun deleteDb(name: String) {
        deleteDb(getBaseDir(), name)
    }

    /**
     * Delete all database files for a database.
     *
     * @param dir the directory where the database files are located
     * @param name the database name
     */
    fun deleteDb(dir: String, name: String) {
        org.h2.tools.DeleteDbFiles.execute(dir, name, true)
    }
}
