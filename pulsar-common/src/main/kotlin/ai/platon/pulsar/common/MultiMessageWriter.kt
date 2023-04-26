package ai.platon.pulsar.common

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
abstract class MultiMessageWriter(
    val baseDir: Path
) : AutoCloseable {
    private val writers = ConcurrentHashMap<Path, MessageWriter>()
    private val closed = AtomicBoolean()

    fun getPath(filename: String): Path {
        return baseDir.resolve(filename)
    }

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

    fun write(message: String, path: Path) {
        writers.computeIfAbsent(path.toAbsolutePath()) { MessageWriter(it) }.write(message)
    }

    fun writeLine(message: String, filename: String) {
        writeLine(message, getPath(filename))
    }

    fun writeLine(message: String, path: Path) {
        val writer = writers.computeIfAbsent(path.toAbsolutePath()) { MessageWriter(it) }
        writer.write(message)
        writer.write("\n")
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
