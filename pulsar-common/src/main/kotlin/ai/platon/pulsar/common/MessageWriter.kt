@file:Suppress("unused")

package ai.platon.pulsar.common

import ai.platon.pulsar.common.MessageWriter.Companion.CHECK_SIZE_EACH_WRITES
import ai.platon.pulsar.common.MessageWriter.Companion.DEFAULT_MAX_FILE_SIZE
import ai.platon.pulsar.common.serialize.json.Pson
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
 * A process-scoped temporary file reference.
 *
 * @param fileName The file name under the process temporary directory.
 * @property path The resolved path under [AppPaths.getProcTmpTmpDirectory].
 */
class TmpFile(val fileName: String) {
    val path = AppPaths.getProcTmpTmpDirectory(fileName)
}

/**
 * A small, file-backed message writer.
 *
 * It appends lines to [filePath] and can optionally prefix each message with a timestamp and module name.
 * This class is intentionally lightweight and used as a *best-effort* file trace facility in places where
 * the full logging pipeline isn't ideal.
 *
 * ## Level filtering
 * Calls to [write] that include a `level` are filtered by [level]. Plain [write] methods always write.
 *
 * ## File rotation
 * Rotation is size-based and checked every [CHECK_SIZE_EACH_WRITES] writes (so the file can temporarily
 * exceed [maxFileSize]). When the current file exceeds [maxFileSize], it is moved to `"<file>.N"`.
 *
 * ## Activity tracking
 * [lastActiveTime], [idleTime], [idleTimeout] and [isIdle] are exposed for external housekeeping.
 * This writer does **not** automatically close itself when idle.
 *
 * ## Thread-safety
 * This is not a fully thread-safe logger. Some internal operations are synchronized, but callers should
 * prefer single-threaded use per instance or add external synchronization when writing concurrently.
 *
 * ## Multi-process caveat
 * Rotation is not atomic across multiple processes writing the same [filePath]. Avoid sharing the same
 * output file across multiple JVM processes.
 *
 * @param filePath The target file to append messages to. Parent directories are created on demand.
 * @param level The maximum log level to write for the level-aware [write] overloads.
 */
class MessageWriter(
    val filePath: Path,
    var level: Int = DEFAULT_LOG_LEVEL
) : AutoCloseable {

    companion object {
        /** Disable level-aware logging. */
        const val OFF = 0

        /** Error level for level-aware logging. */
        const val ERROR = 1

        /** Warn level for level-aware logging. */
        const val WARN = 2

        /** Info level for level-aware logging. */
        const val INFO = 3

        /** Debug level for level-aware logging. */
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

        /**
         * How often to check disk file size (in number of writes) for rotation.
         *
         * A larger value reduces IO overhead, but can cause the trace file to temporarily exceed
         * [DEFAULT_MAX_FILE_SIZE] by more before it is rotated.
         */
        var CHECK_SIZE_EACH_WRITES = 4096

        /**
         * The default idle timeout used by [idleTimeout].
         */
        var IDLE_TIMEOUT: Duration = Duration.ofMinutes(5)

        private val ID_SUPPLIER = AtomicLong()

        /**
         * Writes [content] to [path] once and closes the writer.
         *
         * Content is appended to the file. If [content] is not a [String], it will be serialized using
         * `Pson.toJsonOrString()`.
         *
         * @return The same [path] for convenience.
         */
        fun writeOnce(path: Path, content: Any, level: Int = DEFAULT_LOG_LEVEL): Path {
            MessageWriter(path, level).use { it.write(content) }
            return path
        }

        /**
         * Writes [content] to the process temp [TmpFile] once and closes the writer.
         *
         * @return The resolved temp file path.
         */
        fun writeOnce(file: TmpFile, content: Any, level: Int = DEFAULT_LOG_LEVEL): Path {
            MessageWriter(file, level).use { it.write(content) }
            return file.path
        }
    }

    private val logger = LoggerFactory.getLogger(MessageWriter::class.java)

    private var fileWriter: Writer? = null
    private var printWriter: PrintWriter? = null
    private val closed = AtomicBoolean()

    /** A unique id for this writer instance (mainly for debugging). */
    val id = ID_SUPPLIER.incrementAndGet()

    /** The last time this writer successfully flushed/wrote (best effort). */
    var lastActiveTime = Instant.now()
        private set

    /** The time elapsed since [lastActiveTime]. */
    val idleTime get() = Duration.between(lastActiveTime, Instant.now())

    /**
     * The idle timeout used by [isIdle].
     *
     * Note: this is *activity metadata* only. This writer does not auto-close itself.
     */
    var idleTimeout = IDLE_TIMEOUT

    /** Returns `true` if the writer has been inactive for longer than [idleTimeout]. */
    val isIdle get() = DateTimes.isExpired(lastActiveTime, idleTimeout)

    /**
     * The maximum file size in bytes before rotation.
     *
     * Rotation is checked every [CHECK_SIZE_EACH_WRITES] writes.
     */
    var maxFileSize = DEFAULT_MAX_FILE_SIZE

    /** Date-time format used by [write] overloads that include a module name. */
    var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    /** Internal counter used to check size each [CHECK_SIZE_EACH_WRITES] writes. */
    var checkSize: Int = 0

    /** Internal guard to avoid printing repeated write errors in a tight loop. */
    var writingError: Boolean = false

    /** Internal counter for limiting close debug logs. */
    var closeCount = 0

    constructor(file: TmpFile, level: Int = DEFAULT_LOG_LEVEL) : this(file.path, level)

    /**
     * Writes [s] to the target file.
     * Content is appended to the file.
     * If [s] is not a [String], it will be serialized using `Pson.toJsonOrString()`.
     */
    fun write(s: Any) {
        when (s) {
            is String -> write(s)
            else -> write(Pson.toJsonOrString(s))
        }
    }

    /**
     * Writes a raw string to the underlying file.
     *
     * Content is appended to the file.
     *
     * This call is a no-op after [close] has been called.
     */
    fun write(s: String) {
        // Do not write if the writer has been closed explicitly
        when {
            closed.get() -> return
            else -> writeFile(s)
        }
    }

    /**
     * Writes a message with log [level], using [clazz] as the module name.
     *
     * Content is appended to the file.
     *
     * If `level > this.level`, the message is dropped.
     */
    fun write(level: Int, clazz: KClass<*>, s: String, t: Throwable? = null) {
        // Do not write if the writer has been closed explicitly
        when {
            closed.get() -> return
            level > this.level -> return
            else -> write(level, clazz.simpleName ?: "", s, t)
        }
    }

    /**
     * Writes a message with log [level], prefixing it with a timestamp and [module] name.
     *
     * Content is appended to the file.
     *
     * If `level > this.level`, the message is dropped.
     */
    fun write(level: Int, module: String, s: String, t: Throwable? = null) {
        // Do not write if the writer has been closed explicitly
        when {
            closed.get() -> return
            level > this.level -> return
            else -> writeFile(format(module, s), t)
        }
    }

    /**
     * Flushes buffered output if the writer is open.
     *
     * This call is a no-op after [close] has been called.
     */
    fun flush() {
        if (closed.get()) return
        lastActiveTime = Instant.now()
        printWriter?.flush()
    }

    /**
     * Closes the underlying file writer.
     *
     * This method is idempotent.
     */
    @Synchronized
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
