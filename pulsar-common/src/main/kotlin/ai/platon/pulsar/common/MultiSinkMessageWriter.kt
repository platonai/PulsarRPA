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
abstract class MultiSinkMessageWriter(val conf: ImmutableConfig) : AutoCloseable {
    private val timeIdent = DateTimes.formatNow("MMdd")
    private val jobIdent = conf[CapabilityTypes.PARAM_JOB_NAME, DateTimes.now("HHmm")]
    private val reportDir = AppPaths.REPORT_DIR.resolve(timeIdent).resolve(jobIdent)
    private val writers = ConcurrentHashMap<Path, MessageWriter>()
    private val closed = AtomicBoolean()

    init {
        Files.createDirectories(reportDir)
    }

    fun readAllLines(filename: String): List<String> {
        val path = reportDir.resolve(filename)
        if (Files.exists(path)) {
            return Files.readAllLines(path)
        }
        return listOf()
    }

    fun write(message: String, filename: String) {
        write(message, reportDir.resolve(filename))
    }

    fun write(message: String, file: Path) {
        writers.computeIfAbsent(file.toAbsolutePath()) { MessageWriter(it) }.write(message)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            writers.values.forEach { it.close() }
        }
    }
}
