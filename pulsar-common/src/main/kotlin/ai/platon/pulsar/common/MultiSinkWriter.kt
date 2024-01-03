package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 16-10-12.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * Multiple sink message writer. Messages from different source are write to different files or database.
 */
open class MultiSinkWriter : AutoCloseable {
    private val logger = getLogger(MultiSinkWriter::class)
    private val _writers = ConcurrentHashMap<Path, MessageWriter>()
    private val closed = AtomicBoolean()

    private val timeIdent get() = DateTimes.formatNow("MMdd")
    val reportDir = AppPaths.REPORT_DIR.resolve(timeIdent)
    val writers: Map<Path, MessageWriter> get() = _writers

    init {
        Files.createDirectories(reportDir)
    }

    fun getPath(filename: String) = pathOf(filename)

    fun pathOf(filename: String) = reportDir.resolve(filename)

    fun readAllLines(filename: String): List<String> {
        val path = getPath(filename)
        if (Files.exists(path)) {
            return Files.readAllLines(path)
        }
        return listOf()
    }

    fun write(message: String, filename: String) {
        write(message, getPath(filename))
    }

    fun write(message: String, file: Path) {
        _writers.computeIfAbsent(file.toAbsolutePath()) { MessageWriter(it) }.write(message)
        closeIdleWriters()
    }

    fun writeLine(message: String, filename: String) {
        writeLine(message, getPath(filename))
    }

    fun writeLine(message: String, file: Path) {
        val writer = _writers.computeIfAbsent(file.toAbsolutePath()) { MessageWriter(it) }
        writer.write(message)
        writer.write("\n")

        closeIdleWriters()
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
                runCatching { it.value.close() }.onFailure { logger.warn(it.stringify()) }
            }
        }
    }

    private fun closeIdleWriters() {
        try {
            val idleWriters = _writers.filter { it.value.isIdle }
            idleWriters.forEach { _writers.remove(it.key) }
            idleWriters.forEach {
                runCatching { it.value.close() }.onFailure { logger.warn(it.stringify()) }
            }
        } catch (e: Exception) {
            logger.warn(e.stringify())
        }
    }
}
