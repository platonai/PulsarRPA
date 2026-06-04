package ai.platon.pulsar.common

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * Multiple sink message writer. Messages from different source are write to different files.
 */
open class MultiSinkMessageWriter(
    val baseDir: Path = AppPaths.REPORT_DIR.resolve(DateTimes.formatNow("MMdd"))
) : AutoCloseable {
    companion object {
        private val _writers = ConcurrentHashMap<Path, MessageWriter>()
    }

    private val logger = getLogger(MultiSinkMessageWriter::class)
    private val closed = AtomicBoolean()

    val writers: Map<Path, MessageWriter> get() = _writers

    init {
        Files.createDirectories(baseDir)
    }

    fun getPath(filename: String): Path = pathOf(filename)

    fun pathOf(filename: String): Path = baseDir.resolve(filename)

    fun readAllLines(filename: String): List<String> {
        val path = getPath(filename)
        if (Files.exists(path)) {
            return Files.readAllLines(path)
        }
        return listOf()
    }

    fun write(value: Any, filename: String): Path {
        return writeTo(value, getPath(filename))
    }

    fun writeTo(value: Any, path: Path): Path {
        _writers.computeIfAbsent(path.toAbsolutePath()) { MessageWriter(it) }.write(value)
        closeIdleWriters()
        return path
    }

    fun close(filename: String) {
        val path = getPath(filename)
        val writer = _writers.remove(path)
        writer?.close()
    }

    fun flush() {
        _writers.values.forEach { it.flush() }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            _writers.forEach {
                runCatching { it.value.close() }.onFailure { warnForClose(this, it) }
            }
        }
    }

    private fun closeIdleWriters() {
        try {
            val idleWriters = _writers.filter { it.value.isIdle }
            idleWriters.forEach { _writers.remove(it.key) }
            idleWriters.forEach {
                runCatching { it.value.close() }.onFailure { warnForClose(this, it) }
            }
        } catch (e: Exception) {
            logger.warn("Exception", e)
        }
    }
}
