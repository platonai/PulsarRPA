package ai.platon.pulsar.common

import java.io.PrintWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import kotlin.math.max

/**
 * A simple log system
 */
class SimpleLogger(val path: Path, var levelFile: Int = DEFAULT_LOG_LEVEL): AutoCloseable {

    private var systemOutLevel = DEFAULT_LOG_LEVEL_SYSTEM_OUT
    var sysOut = System.out
    private var levelMax: Int = INFO
    private var maxFileSize = DEFAULT_MAX_FILE_SIZE
    private var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ")
    private var fileWriter: Writer? = null
    private var printWriter: PrintWriter? = null
    private var checkSize: Int = 0
    private var closed: Boolean = false
    private var writingErrorLogged: Boolean = false

    init {
        updateLevel()
    }

    fun write(level: Int, clazz: Class<*>, s: String, t: Throwable? = null) {
        val name = clazz.simpleName
        write(level, name, s, t)
    }

    fun write(level: Int, module: String, s: String, t: Throwable? = null) {
        if (level <= systemOutLevel) {
            // level <= levelSystemOut: the system out level is set higher
            // level > this.level: the level for this module is set higher
            sysOut.println(format(module, s))
            if (systemOutLevel == DEBUG) {
                t?.printStackTrace(sysOut)
            }
        }

        if (level <= this.levelFile) {
            writeFile(format(module, s), t)
        }
    }

    private fun updateLevel() {
        levelMax = max(systemOutLevel, this.levelFile)
    }

    @Synchronized
    private fun format(module: String, s: String): String {
        return dateFormat.format(System.currentTimeMillis()) + module + ": " + s
    }

    @Synchronized
    private fun writeFile(s: String, t: Throwable?) {
        try {
            if (checkSize++ >= CHECK_SIZE_EACH_WRITES) {
                checkSize = 0
                closeWriter()
                if (maxFileSize > 0 && Files.size(path) > maxFileSize) {
                    val old = Paths.get("$path.old")
                    Files.delete(old)
                    Files.move(path, old)
                }
            }

            val pr = openWriter()?:return
            pr.println(s)
            t?.printStackTrace(pr)
            pr.flush()

            if (closed) {
                closeWriter()
            }
        } catch (e: Exception) {
            logWritingError(e)
        }
    }

    private fun logWritingError(e: Exception) {
        if (writingErrorLogged) {
            return
        }
        writingErrorLogged = true
        // print this error only once
        sysOut.println(e)
        e.printStackTrace()
    }

    private fun openWriter(): PrintWriter? {
        if (printWriter == null) {
            try {
                Files.createDirectories(path.parent)
                fileWriter = Files.newBufferedWriter(path)
                printWriter = PrintWriter(fileWriter!!, true)
            } catch (e: Exception) {
                logWritingError(e)
                return null
            }
        }

        return printWriter
    }

    @Synchronized
    private fun closeWriter() {
        printWriter?.flush()
        printWriter?.use { it.close() }
        fileWriter?.use { it.close() }

        printWriter = null
        fileWriter = null
    }

    override fun close() {
        closeWriter()
        closed = true
    }

    companion object {
        const val OFF = 0
        const val ERROR = 1
        const val WARN = 2
        const val INFO = 3
        const val DEBUG = 4

        /**
         * The default level for system out log messages.
         */
        val DEFAULT_LOG_LEVEL_SYSTEM_OUT = OFF

        /**
         * The default level for file log messages.
         */
        val DEFAULT_LOG_LEVEL = ERROR

        /**
         * The default maximum trace file size. It is currently 64 MB. Additionally,
         * there could be a .old file of the same size.
         */
        private val DEFAULT_MAX_FILE_SIZE = 64 * 1024 * 1024

        private val CHECK_SIZE_EACH_WRITES = 4096
    }
}
