package ai.platon.pulsar.common

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.file.*
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

    private val log = LoggerFactory.getLogger(MessageWriter::class.java)

    private var fileWriter: Writer? = null
    private var printWriter: PrintWriter? = null
    private val closed = AtomicBoolean()

    var maxFileSize = DEFAULT_MAX_FILE_SIZE
    var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ")

    var checkSize: Int = 0
    var writingError: Boolean = false

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
                    Files.move(path, old, StandardCopyOption.REPLACE_EXISTING)
                }
            }

            openWriter()?.also {
                it.println(s)
                t?.printStackTrace(it)
            }
        } catch (e: Exception) {
            logWritingError(e)
        }
    }

    private fun logWritingError(e: Exception) {
        if (writingError) {
            return
        }
        writingError = true
        e.printStackTrace()
        writingError = false
    }

    private fun openWriter(): PrintWriter? {
        if (printWriter == null) {
            try {
                Files.createDirectories(path.parent)
                // println("Create printer writer to $path")
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
        log.info("Closing writer | $path")
        printWriter?.flush()
        printWriter?.close()
        fileWriter?.close()

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
