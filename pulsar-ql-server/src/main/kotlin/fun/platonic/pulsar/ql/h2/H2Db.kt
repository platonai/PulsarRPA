package `fun`.platonic.pulsar.ql.h2

import `fun`.platonic.pulsar.common.PulsarPaths
import org.h2.store.FileLister
import org.h2.tools.DeleteDbFiles
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.*

/**
 * The base class for all tests.
 */
class H2Db(sessionFactory: String = H2QueryEngine::class.java.name) {

    init {
        System.setProperty("h2.sessionFactory", sessionFactory)
    }

    val config = H2DbConfig()

    /**
     * The base directory.
     */
    val baseTestDir = PulsarPaths.get(PulsarPaths.testDir, "test", "h2")

    /**
     * The temporary directory.
     */
    val tmpDir = PulsarPaths.get(PulsarPaths.tmpDir, "h2")

    /**
     * The time when the test was started.
     */
    val startTime: Instant = Instant.now()

    /**
     * The base directory to write test databases.
     */
    private var baseDir = getTestDir("")

    /**
     * Get the file password (only required if file encryption is used).
     *
     * @return the file password
     */
    val filePassword: String = "filePassword"

    /**
     * Get the login password. This is usually the user password. If file
     * encryption is used it is combined with the file password.
     *
     * @return the login password
     */
    val password: String = getPassword("sa")

    val user: String = "sa"

    fun generateTempDbName(): String {
        return "" + System.currentTimeMillis() + "_" + Math.abs(Random().nextInt());
    }

    /**
     * Open a database connection in admin mode. The default user name and
     * password is used.
     *
     * @param name the database name
     * @return the connection
     */
    fun getConnection(name: String): Connection {
        return getConnectionInternal(buildURL("$name;MODE=sigma", true), user, password)
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
        return getConnectionInternal(buildURL(name, false), user, password)
    }

    /**
     * Get the password to use to login for the given user password. The file
     * password is added if required.
     *
     * @param userPassword the password of this user
     * @return the login password
     */
    fun getPassword(userPassword: String): String {
        return if (config.cipher == null)
            userPassword
        else
            "$filePassword $userPassword"
    }

    /**
     * Get the base directory for tests.
     * If a special file system is used, the prefix is prepended.
     *
     * @return the directory, possibly including file system prefix
     */
    fun buildBaseDir(): String {
        var dir = baseDir.toString()
        if (config.reopen) {
            dir = "rec:memFS:$dir"
        }
        if (config.splitFileSystem) {
            dir = "split:16:$dir"
        }
        // return "split:nioMapped:" + baseDir;
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
    fun buildURL(name_: String, admin: Boolean): String {
        var name = name_
        var url: String
        if (name.startsWith("jdbc:")) {
            if (config.mvStore) {
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
        if (idx == -1 && config.memory) {
            name = "mem:$name"
        } else {
            if (idx < 0 || idx > 10) {
                // index > 10 if in options
                name = buildBaseDir() + "/" + name
            }
        }

        if (config.networked) {
            val port = config.port

            if (config.ssl) {
                url = "ssl://localhost:$port/$name"
            } else {
                url = "tcp://localhost:$port/$name"
            }
        } else if (config.googleAppEngine) {
            url = "gae://$name;FILE_LOCK=NO;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE"
        } else {
            url = name
        }

        url = if (config.mvStore) {
            addOption(url, "MV_STORE", "true")
            // url = addOption(url, "MVCC", "true");
        } else {
            addOption(url, "MV_STORE", "false")
        }

        if (!config.memory) {
            if (config.smallLog && admin) {
                url = addOption(url, "MAX_LOG_SIZE", "1")
            }
        }

        if (config.traceSystemOut) {
            url = addOption(url, "TRACE_LEVEL_SYSTEM_OUT", "2")
        }

        if (config.traceLevelFile > 0 && admin) {
            url = addOption(url, "TRACE_LEVEL_FILE", "" + config.traceLevelFile)
            url = addOption(url, "TRACE_MAX_FILE_SIZE", "8")
        }

        url = addOption(url, "log", "1")
        if (config.throttleDefault > 0) {
            url = addOption(url, "THROTTLE", "" + config.throttleDefault)
        } else if (config.throttle > 0) {
            url = addOption(url, "THROTTLE", "" + config.throttle)
        }

        url = addOption(url, "LOCK_TIMEOUT", "" + config.lockTimeout)
        if (config.diskUndo && admin) {
            url = addOption(url, "MAX_MEMORY_UNDO", "3")
        }

        if (config.big && admin) {
            // force operations to disk
            url = addOption(url, "MAX_OPERATION_MEMORY", "1")
        }

        if (config.mvcc) {
            url = addOption(url, "MVCC", "TRUE")
        }

        if (config.multiThreaded) {
            url = addOption(url, "MULTI_THREADED", "TRUE")
        }

        if (config.lazy) {
            url = addOption(url, "LAZY_QUERY_EXECUTION", "1")
        }

        if (config.diskResult && admin) {
            url = addOption(url, "MAX_MEMORY_ROWS", "100")
            url = addOption(url, "CACHE_SIZE", "0")
        }

        if (config.defrag) {
            url = addOption(url, "DEFRAG_ALWAYS", "TRUE")
        }

        return "jdbc:h2:$url"
    }

    /**
     * Delete all database files for this database.
     *
     * @param name the database name
     */
    fun deleteDb(name: String) {
        deleteDb(buildBaseDir(), name)
    }

    /**
     * Delete all database files for a database.
     *
     * @param dir the directory where the database files are located
     * @param name the database name
     */
    fun deleteDb(dir: String, name: String) {
        DeleteDbFiles.execute(dir, name, true)
         val list = FileLister.getDatabaseFiles(baseDir.toString(), name, true);
         if (list.isNotEmpty()) {
            System.out.println("Not deleted: $list");
         }
    }

    /**
     * Get the test directory for this test.
     *
     * @param name the directory name suffix
     * @return the test directory
     */
    fun getTestDir(name: String): Path {
        return PulsarPaths.get(baseTestDir, name)
    }

    private fun addOption(url: String, option: String, value: String): String {
        var u = url
        if (u.indexOf(";$option=") < 0) {
            u += ";$option=$value"
        }
        return u
    }

    private fun getConnectionInternal(url: String, user: String, password: String): Connection {
        println("H2 Connection: $url")

        org.h2.Driver.load()
        return DriverManager.getConnection(url, user, password)
    }
}
