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

class TmpFile(val fileName: String) {
    val path = AppPaths.getProcTmpTmpDirectory(fileName)
}

/**
 * A simple log system
 */
class MessageWriter(
    val filePath: Path,
    var level: Int = DEFAULT_LOG_LEVEL
): AutoCloseable {

    companion object {
        @Suppress("unused")
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

        fun writeOnce(path: Path, content: Any, level: Int = DEFAULT_LOG_LEVEL) {
            MessageWriter(path, level).use { it.write(content) }
        }

        fun writeOnce(file: TmpFile, content: Any, level: Int = DEFAULT_LOG_LEVEL) {
            MessageWriter(file, level).use { it.write(content) }
        }
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

    var closeCount = 0

    constructor(file: TmpFile, level: Int = DEFAULT_LOG_LEVEL): this(file.path, level)

    fun write(s: Any) = write(s.toString())

    fun write(s: String) {
        // Do not write if the writer has been closed explicitly
        when {
            closed.get() -> return
            else -> writeFile(s)
        }
    }

    fun write(level: Int, clazz: KClass<*>, s: String, t: Throwable? = null) {
        // Do not write if the writer has been closed explicitly
        when {
            closed.get() -> return
            level > this.level -> return
            else -> write(level, clazz.simpleName ?: "", s, t)
        }
    }

    fun write(level: Int, module: String, s: String, t: Throwable? = null) {
        // Do not write if the writer has been closed explicitly
        when {
            closed.get() -> return
            level > this.level -> return
            else -> writeFile(format(module, s), t)
        }
    }

    fun flush() {
        if (closed.get()) return
        lastActiveTime = Instant.now()
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
            // update activity time as early as possible
            lastActiveTime = Instant.now()

            val threshold = if (CHECK_SIZE_EACH_WRITES <= 0) 1 else CHECK_SIZE_EACH_WRITES
            if (++checkSize >= threshold) {
                checkSize = 0
                closeWriter("rotate file")

                // Determine next rotation index safely (match baseName or baseName.N with numeric N)
                val baseName = filePath.fileName.toString()
                val dir = filePath.parent ?: Paths.get(".")
                val pattern = Regex("^" + Regex.escape(baseName) + "\\.(\\d+)$")
                var maxIndex = 0
                try {
                    Files.list(dir).use { stream ->
                        stream
                            .filter { it.isRegularFile() }
                            .map { it.fileName.toString() }
                            .forEach { name ->
                                val m = pattern.matchEntire(name)
                                if (m != null) {
                                    val idx = m.groupValues[1].toIntOrNull() ?: 0
                                    if (idx > maxIndex) maxIndex = idx
                                }
                            }
                    }
                } catch (_: Exception) {
                    // ignore listing errors, fall back to index 0
                }
                val nextIndex = maxIndex + 1

                if (maxFileSize > 0 && Files.exists(filePath)) {
                    try {
                        if (Files.size(filePath) > maxFileSize) {
                            val rotated = Paths.get("$filePath.$nextIndex")
                            Files.move(filePath, rotated, StandardCopyOption.REPLACE_EXISTING)
                        }
                    } catch (_: Exception) {
                        // ignore rotation errors, keep writing to current file
                    }
                }
            }

            openWriter()?.also {
                it.println(s)
                t?.printStackTrace(it)
                // ensure stacktraces are flushed too
                it.flush()
                lastActiveTime = Instant.now()
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
                val parent = filePath.parent
                if (parent != null) {
                    Files.createDirectories(parent)
                }
                // println("Create printer writer to $path")
                fileWriter = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
                printWriter = PrintWriter(fileWriter!!, true)
                lastActiveTime = Instant.now()
            } catch (e: Exception) {
                logWritingError(e)
                return null
            }
        }

        return printWriter
    }

    @Synchronized
    private fun closeWriter(message: String) {
        if (closeCount++ < 20) {
            logger.debug("Closing writer #{} | idle={} | {} | {}", id, idleTime, message, filePath)
        }

        printWriter?.flush()
        printWriter?.close()
        fileWriter?.close()

        printWriter = null
        fileWriter = null
    }
}
