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
abstract class MultiSinkWriter(val conf: ImmutableConfig) : AutoCloseable {
    private val timeIdent get() = DateTimes.formatNow("MMdd")
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME]
    private val reportDir0 get() = AppPaths.REPORT_DIR.resolve(timeIdent)
    private val reportDir = if (jobIdent == null) reportDir0 else reportDir0.resolve(jobIdent)
    private val writers = ConcurrentHashMap<Path, MessageWriter>()
    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)
    }

    fun getPath(filename: String): Path {
        return reportDir.resolve(filename)
    }

    fun readAllLines(filename: String): List<String> {
        val path = getPath(filename)
        if (Files.exists(path)) {
            return Files.readAllLines(path)
        }
        return listOf()
    }

    fun write(message: String, filename: String): Path {
        val path = getPath(filename)
        write(message, path)
        return path
    }

    fun write(message: String, file: Path): Path {
        writers.computeIfAbsent(file.toAbsolutePath()) { MessageWriter(it) }.write(message)
        return file
    }

    fun writeLine(message: String, filename: String): Path {
        val path = getPath(filename)
        writeLine(message, path)
        return path
    }

    fun writeLine(message: String, file: Path): Path {
        val writer = writers.computeIfAbsent(file.toAbsolutePath()) { MessageWriter(it) }
        writer.write(message)
        writer.write("\n")
        return file
    }

    fun closeWriter(filename: String) {
        writers[getPath(filename)]?.close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            writers.values.forEach { it.close() }
        }
    }
}
