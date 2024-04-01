package ai.platon.pulsar.common

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.Writer
import java.nio.file.*
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.isRegularFile
import kotlin.reflect.KClass

/**
 * A simple log system
 */
class MessageWriter(
    val filePath: Path,
    var levelFile: Int = DEFAULT_LOG_LEVEL
): AutoCloseable {

    companion object {
        const val OFF = 0
        const val ERROR = 1
        const val WARN = 2
        const val INFO = 3
        const val DEBUG = 4
        /**
         * The default level for file log messages.
         */
        var DEFAULT_LOG_LEVEL = INFO
        /**
         * The default maximum trace file size. It is currently 512 MB. Additionally,
         * there could be a .1, .2, ... file of the same size.
         */
        var DEFAULT_MAX_FILE_SIZE = 512 * 1024 * 1024

        var CHECK_SIZE_EACH_WRITES = 4096

        var IDLE_TIMEOUT = Duration.ofMinutes(5)
        
        private val ID_SUPPLIER = AtomicLong()
    }

    private val logger = LoggerFactory.getLogger(MessageWriter::class.java)

    private var fileWriter: Writer? = null
    private var printWriter: PrintWriter? = null
    private val closed = AtomicBoolean()

    val id = ID_SUPPLIER.incrementAndGet()
    
    var lastActiveTime = Instant.now()
        private set
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())
    var idleTimeout = IDLE_TIMEOUT
    val isIdle get() = DateTimes.isExpired(lastActiveTime, idleTimeout)
    var maxFileSize = DEFAULT_MAX_FILE_SIZE

    var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    var checkSize: Int = 0
    var writingError: Boolean = false

    fun write(s: String) {
        writeFile(s)
    }

    fun write(level: Int, clazz: KClass<*>, s: String, t: Throwable? = null) {
        if (level > this.levelFile) {
            return
        }
        write(level, clazz.simpleName?:"", s, t)
    }

    fun write(level: Int, module: String, s: String, t: Throwable? = null) {
        if (level > this.levelFile) {
            return
        }
        writeFile(format(module, s), t)
    }
    
    fun flush() {
        printWriter?.flush()
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeWriter("close writer")
        }
    }

    @Synchronized
    private fun format(module: String, s: String): String {
        return dateFormat.format(System.currentTimeMillis()) + " " + module + ": " + s
    }

    @Synchronized
    private fun writeFile(s: String, t: Throwable? = null) {
        try {
            if (checkSize++ >= CHECK_SIZE_EACH_WRITES) {
                checkSize = 0
                closeWriter("rotate file")
                val count = Files.list(filePath.parent)
                    .filter { it.isRegularFile() }
                    .filter { it.fileName.toString().contains(filePath.fileName.toString()) }
                    .count()
                if (maxFileSize > 0 && Files.size(filePath) > maxFileSize) {
                    val old = Paths.get("$filePath.$count")
                    Files.move(filePath, old, StandardCopyOption.REPLACE_EXISTING)
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
                Files.createDirectories(filePath.parent)
                // println("Create printer writer to $path")
                fileWriter = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
                printWriter = PrintWriter(fileWriter!!, true)
            } catch (e: Exception) {
                logWritingError(e)
                return null
            }
        }

        return printWriter
    }

    @Synchronized
    private fun closeWriter(message: String) {
        logger.info("Closing writer #$id | ${idleTime.readable()} | $message | $filePath")
        printWriter?.flush()
        printWriter?.close()
        fileWriter?.close()

        printWriter = null
        fileWriter = null
    }
}
