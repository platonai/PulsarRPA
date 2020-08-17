package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.common.AppPaths
import org.h2.engine.Constants
import java.nio.file.Path

/**
 * The h2 db config
 */
data class H2DbConfig(

        val sessionFactory: String = System.getProperty("h2.sessionFactory", "ai.platon.pulsar.ql.h2.H2SessionFactory"),

        /**
         * The base directory to write test databases.
         */
        var baseDir: Path = AppPaths.DATA_DIR.resolve("h2"),

        /**
         * Get the file password (only required if file encryption is used).
         *
         * @return the file password
         */
        var filePassword: String = "filePassword",

        /**
         * Get the login password. This is usually the user password. If file
         * encryption is used it is combined with the file password.
         *
         * @return the login password
         */
        var user: String = "sa",

        var password: String = "sa",

        /**
         * Whether the MVStore storage is used.
         */
        var mvStore: Boolean = Constants.VERSION_MINOR >= 4,

        /**
         * If the test should run with many rows.
         */
        var big: Boolean = false,

        /**
         * If remote database connections should be used.
         */
        var networked: Boolean = false,

        /**
         * If in-memory databases should be used.
         */
        var memory: Boolean = false,

        /**
         * If the multi-threaded mode should be used.
         */
        var multiThreaded: Boolean = false,

        /**
         * If lazy queries should be used.
         */
        var lazy: Boolean = false,

        /**
         * The cipher to use (null for unencrypted).
         */
        var cipher: String? = null,

        /**
         * The file trace level value to use.
         */
        var traceLevelFile: Int = 0,

        /**
         * If test trace information should be written (for debugging only).
         */
        var traceTest: Boolean = false,

        /**
         * If a small cache and a low number for MAX_MEMORY_ROWS should be used.
         */
        var diskResult: Boolean = false,

        /**
         * Test using the recording file system.
         */
        var reopen: Boolean = false,

        /**
         * Test the split file system.
         */
        var splitFileSystem: Boolean = false,

        /**
         * The lock timeout to use
         */
        var lockTimeout: Int = 50,

        /**
         * If the transaction log should be kept small (that is, the log should be
         * switched early).
         */
        var smallLog: Boolean = false,

        /**
         * If SSL should be used for remote connections.
         */
        var ssl: Boolean = false,

        /**
         * If MAX_MEMORY_UNDO=3 should be used.
         */
        var diskUndo: Boolean = false,

        /**
         * If TRACE_LEVEL_SYSTEM_OUT should be set to 2 (for debugging only).
         */
        var traceSystemOut: Boolean = false,

        /**
         * The THROTTLE value to use.
         */
        var throttle: Int = 0,

        /**
         * The THROTTLE value to use by default.
         */
        var throttleDefault: Int = Integer.parseInt(System.getProperty("throttle", "0")),

        /**
         * If the database should always be defragmented when closing.
         */
        var defrag: Boolean = false,

        /**
         * Port for test server
         * */
        var port: Int = 19092
)
