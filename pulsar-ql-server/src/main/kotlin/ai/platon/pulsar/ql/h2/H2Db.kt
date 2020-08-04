package ai.platon.pulsar.ql.h2

import org.h2.store.FileLister
import org.h2.tools.DeleteDbFiles
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.math.abs

/**
 * The base class for all tests.
 */
class H2Db(
        sessionFactory: String = System.getProperty("h2.sessionFactory", "ai.platon.pulsar.ql.h2.H2SessionFactory"),
        val conf: H2DbConfig = H2DbConfig()
) {
    init {
        System.setProperty("h2.sessionFactory", sessionFactory)
    }

    fun generateTempDbName(): String {
        return "" + System.currentTimeMillis() + "_" + abs(Random().nextInt());
    }

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

    /**
     * Open a database connection.
     *
     * @param name the database name
     * @param user the user name to use
     * @param password the password to use
     * @return the connection
     */
    fun getConnection(name: String, user: String, password: String) =
            getConnection0(buildURL(name, false), user, password)

    /**
     * Get the password to use to login for the given user password. The file
     * password is added if required.
     *
     * @param userPassword the password of this user
     * @return the login password
     */
    fun getPassword(userPassword: String): String {
        return if (conf.cipher == null)
            userPassword
        else
            "${conf.filePassword} $userPassword"
    }

    /**
     * Get the base directory for tests.
     * If a special file system is used, the prefix is prepended.
     *
     * @return the directory, possibly including file system prefix
     */
    fun buildBaseDir(): String {
        var dir = conf.baseDir.toString()
        if (conf.reopen) {
            dir = "rec:memFS:$dir"
        }
        if (conf.splitFileSystem) {
            dir = "split:16:$dir"
        }
        // return "split:nioMapped:" + baseDir;
        return dir
    }

    /**
     * Get the database URL for the given database name using the current
     * confuration options.
     *
     * @param name the database name
     * @param admin true if the current user is an admin
     * @return the database URL
     */
    fun buildURL(name: String, admin: Boolean): String {
        var name0 = name
        var url: String
        if (name0.startsWith("jdbc:")) {
            name0 = if (conf.mvStore) {
                addOption(name0, "MV_STORE", "true")
            } else {
                addOption(name0, "MV_STORE", "false")
            }
            return name0
        }
        if (admin) { // name = addOption(name, "RETENTION_TIME", "10");
            // name = addOption(name, "WRITE_DELAY", "10");
        }
        val idx = name0.indexOf(':')
        if (idx == -1 && conf.memory) {
            name0 = "mem:$name0"
        } else {
            if (idx < 0 || idx > 10) { // index > 10 if in options
                name0 = "${conf.baseDir}/$name0"
            }
        }
        url = if (conf.networked) {
            if (conf.ssl) {
                "ssl://localhost:" + conf.port + "/" + name0
            } else {
                "tcp://localhost:" + conf.port + "/" + name0
            }
        } else {
            name0
        }
        if (conf.mvStore) {
            url = addOption(url, "MV_STORE", "true")
            url = addOption(url, "MAX_COMPACT_TIME", "0") // to speed up tests
        } else {
            url = addOption(url, "MV_STORE", "false")
        }
        if (!conf.memory) {
            if (conf.smallLog && admin) {
                url = addOption(url, "MAX_LOG_SIZE", "1")
            }
        }
        if (conf.traceSystemOut) {
            url = addOption(url, "TRACE_LEVEL_SYSTEM_OUT", "2")
        }
        if (conf.traceLevelFile > 0 && admin) {
            url = addOption(url, "TRACE_LEVEL_FILE", "" + conf.traceLevelFile)
            url = addOption(url, "TRACE_MAX_FILE_SIZE", "8")
        }
        url = addOption(url, "LOG", "1")
        if (conf.throttleDefault > 0) {
            url = addOption(url, "THROTTLE", "" + conf.throttleDefault)
        } else if (conf.throttle > 0) {
            url = addOption(url, "THROTTLE", "" + conf.throttle)
        }
        url = addOption(url, "LOCK_TIMEOUT", "" + conf.lockTimeout)
        if (conf.diskUndo && admin) {
            url = addOption(url, "MAX_MEMORY_UNDO", "3")
        }
        if (conf.big && admin) { // force operations to disk
            url = addOption(url, "MAX_OPERATION_MEMORY", "1")
        }
        if (conf.lazy) {
            url = addOption(url, "LAZY_QUERY_EXECUTION", "1")
        }
//        if (conf.cacheType != null && admin) {
//            url = addOption(url, "CACHE_TYPE", conf.cacheType)
//        }
        if (conf.diskResult && admin) {
            url = addOption(url, "MAX_MEMORY_ROWS", "100")
            url = addOption(url, "CACHE_SIZE", "0")
        }
//        if (conf.cipher != null) {
//            url = addOption(url, "CIPHER", conf.cipher)
//        }
        if (conf.defrag) {
            url = addOption(url, "DEFRAG_ALWAYS", "TRUE")
        }
//        if (conf.collation != null) {
//            url = addOption(url, "COLLATION", conf.collation)
//        }
        return "jdbc:h2:$url"
    }

    /**
     * Delete all database files for this database.
     *
     * @param name the database name
     */
    fun deleteDb(name: String) = deleteDb(buildBaseDir(), name)

    /**
     * Delete all database files for a database.
     *
     * @param dir the directory where the database files are located
     * @param name the database name
     */
    fun deleteDb(dir: String, name: String) {
        DeleteDbFiles.execute(dir, name, true)
         val list = FileLister.getDatabaseFiles(conf.baseDir.toString(), name, true);
         if (list.isNotEmpty()) {
            println("Not deleted: $list")
         }
    }

    private fun addOption(url: String, option: String, value: String): String {
        var u = url
        if (u.indexOf(";$option=") < 0) {
            u += ";$option=$value"
        }
        return u
    }

    private fun getConnection0(url: String, user: String, password: String): Connection {
        // println("Open H2 Connection: $url")
        org.h2.Driver.load()
        return DriverManager.getConnection(url, user, password)
    }
}
