package ai.platon.pulsar.common

import java.io.PrintWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * A simple log system
 */
class MessageWriter(
        val path: Path,
        var levelFile: Int = DEFAULT_LOG_LEVEL
): AutoCloseable {

    private var maxFileSize = DEFAULT_MAX_FILE_SIZE
    private var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ")
    private var fileWriter: Writer? = null
    private var printWriter: PrintWriter? = null
    private var checkSize: Int = 0
    private val closed = AtomicBoolean()
    private var writingErrorLogged: Boolean = false

    fun write(s: String) {
        writeFile(s)
    }

    fun write(level: Int, clazz: KClass<*>, s: String, t: Throwable? = null) {
        if (level > this.levelFile) return
        write(level, clazz.simpleName?:"", s, t)
    }

    fun write(level: Int, module: String, s: String, t: Throwable? = null) {
        if (level > this.levelFile) return
        writeFile(format(module, s), t)
    }

    @Synchronized
    private fun format(module: String, s: String): String {
        return dateFormat.format(System.currentTimeMillis()) + module + ": " + s
    }

    @Synchronized
    private fun writeFile(s: String, t: Throwable? = null) {
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

            openWriter()?.use {
                it.println(s)
                t?.printStackTrace(it)
                it.flush()
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
        e.printStackTrace()
        writingErrorLogged = false
    }

    private fun openWriter(): PrintWriter? {
        if (printWriter == null) {
            try {
                Files.createDirectories(path.parent)
                fileWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
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
        if (closed.compareAndSet(false, true)) {
            closeWriter()
        }
    }

    companion object {
        const val OFF = 0
        const val ERROR = 1
        const val WARN = 2
        const val INFO = 3
        const val DEBUG = 4
        /**
         * The default level for file log messages.
         */
        val DEFAULT_LOG_LEVEL = INFO
        /**
         * The default maximum trace file size. It is currently 64 MB. Additionally,
         * there could be a .old file of the same size.
         */
        private val DEFAULT_MAX_FILE_SIZE = 64 * 1024 * 1024

        private val CHECK_SIZE_EACH_WRITES = 4096
    }
}
