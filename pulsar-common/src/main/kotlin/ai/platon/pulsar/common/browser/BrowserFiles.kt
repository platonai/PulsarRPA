package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.*
import com.google.common.collect.Iterators
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.MonthDay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.io.path.exists
import kotlin.io.path.notExists

internal class ContextGroup(val group: String) {
    
    class PathIterator(private val paths: Iterable<Path>): Iterator<Path> {
        private val iterator = Iterators.cycle(paths)
        
        override fun hasNext(): Boolean {
            return paths.iterator().hasNext()
        }
        
        override fun next(): Path {
            return iterator.next()
        }
    }
    
    private val paths = ConcurrentSkipListSet<Path>()
    
    val size: Int
        get() = paths.size
    
    val iterator = PathIterator(paths)
    
    fun add(path: Path) {
        paths.add(path)
    }
}

object BrowserFiles {
    
    private val logger = getLogger(this)
    
    // The prefix for all temporary privacy contexts. System context, prototype context and default context are not
    // required to start with the prefix.
    const val CONTEXT_DIR_PREFIX = "cx."
    
    const val PID_FILE_NAME = "launcher.pid"
    
    val TEMPORARY_UDD_EXPIRY = Duration.ofHours(12)

    private val contextGroups = ConcurrentHashMap<String, ContextGroup>()
    
    private val cleanedUserDataDirs = ConcurrentSkipListSet<Path>()
    
    @Throws(IOException::class)
    @Synchronized
    fun computeTestContextDir(fingerprint: Fingerprint = Fingerprint.DEFAULT, maxAgents: Int = 10): Path {
        val lockFile = AppPaths.BROWSER_TMP_DIR_LOCK
        return runWithFileLock(lockFile) { channel ->
            computeNextSequentialContextDir0("test", fingerprint, maxAgents, channel = channel)
        }
    }

    @Throws(IOException::class)
    @Synchronized
    fun computeNextSequentialContextDir(
        group: String = "default", fingerprint: Fingerprint = Fingerprint.DEFAULT, maxAgents: Int = 10): Path {
        val lockFile = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve("contex.group.lock")
        if (!Files.exists(lockFile)) {
            Files.createFile(lockFile)
        }
        return runWithFileLock(lockFile) { channel ->
            computeNextSequentialContextDir0(group, fingerprint, maxAgents, channel = channel)
        }
    }
    
    @Throws(IOException::class)
    @Synchronized
    fun computeRandomTmpContextDir(group: String = "default"): Path {
        val lockFile = AppPaths.BROWSER_TMP_DIR_LOCK
        return computeRandomContextDir0(group)
        // return runWithFileLock(lockFile) { channel -> computeRandomContextDir0(group, channel = channel) }
    }
    
    @Throws(IOException::class)
    fun cleanOldestContextTmpDirs(recentNToKeep: Int = 20) {
        // Remove directories that have too many context directories
        Files.walk(AppPaths.CONTEXT_TMP_DIR, 3)
            .filter { it !in cleanedUserDataDirs } // not processed
            .filter { it.toString().contains("cx.") } // context dir
            .toList()
            .toSet()
            .sortedByDescending { Files.getLastModifiedTime(it) }  // newest first
            .drop(recentNToKeep)  // drop the latest 20 context dirs
            .forEach { cleanUpContextDir(it, Duration.ofSeconds(30)) } // clean the rest
    }

    @Throws(IOException::class)
    fun cleanUpContextTmpDir(expiry: Duration) {
        Files.walk(AppPaths.CONTEXT_TMP_DIR, 3)
            .filter { it !in cleanedUserDataDirs }
            .filter { it.fileName.toString().startsWith("cx.") }
            .forEach { path -> cleanUpContextDir(path, expiry) }
        
        cleanOldestContextTmpDirs()
    }
    
    /**
     * Clear the browser's user data dir inside the given context path.
     * @param path The context path
     * @param expiry The expiry duration
     * */
    @Throws(IOException::class)
    fun cleanUpContextDir(path: Path, expiry: Duration) {
        if (!path.fileName.toString().startsWith("cx.")) {
            logger.info("Not a context directory | {}", path)
            return
        }
        if (path.resolve(PID_FILE_NAME).exists()) {
            // The directory is already cleaned
            return
        }
        
        deleteTemporaryUserDataDirWithLock(path.resolve("pulsar_chrome"), expiry)
    }
    
    @Throws(IOException::class)
    @Synchronized
    fun deleteTemporaryUserDataDirWithLock(userDataDir: Path, expiry: Duration) {
        val lockFile = AppPaths.BROWSER_TMP_DIR_LOCK
        runWithFileLock(lockFile) { channel -> deleteTemporaryUserDataDir0(userDataDir, expiry, channel) }
    }
    
    @Throws(IOException::class)
    @Synchronized
    private fun <T> runWithFileLock(lockFile: Path, supplier: (FileChannel) -> T): T {
        // Opens or creates a file, returning a file channel to access the file.
        val channel = FileChannel.open(lockFile, StandardOpenOption.APPEND)
        channel.use {
            val lock = it.tryLock()
            try {
                return supplier(it)
            } finally {
                lock?.release()
            }
        }
    }

    @Throws(IOException::class)
    private fun deleteTemporaryUserDataDir0(userDataDir: Path, expiry: Duration, channel: FileChannel) {
        require(channel.isOpen) { "The lock file channel is closed" }
        
        val dirToDelete = userDataDir
        
        if (!Files.exists(dirToDelete)) {
            // The directory has been deleted by other threads
            return
        }
        
        val cleanedUp = dirToDelete in cleanedUserDataDirs
        if (cleanedUp) {
            return
        }
        
        // Be careful, do not delete files by mistake, so delete files only inside AppPaths.CONTEXT_TMP_DIR
        // If it's in the context tmp dir, the user data dir can be deleted safely
        val isTemporary = dirToDelete.startsWith(AppPaths.CONTEXT_TMP_DIR)
        if (!isTemporary) {
            return
        }
        
        val lastModifiedTime = Files.getLastModifiedTime(dirToDelete).toInstant()
        val isExpired = DateTimes.isExpired(lastModifiedTime, expiry)
        if (!isExpired) {
            return
        }
        
        // Double check to ensure it's safe to delete the directory
        val hasSiblingPidFile = Files.exists(dirToDelete.resolveSibling(PID_FILE_NAME))
        if (!hasSiblingPidFile) {
            return
        }

        FileUtils.deleteQuietly(dirToDelete.toFile())

        if (Files.exists(dirToDelete)) {
            logger.error("Browser data dir not deleted | {}", dirToDelete)
        } else {
            cleanedUserDataDirs.add(dirToDelete)
        }
    }

    /**
     * Compute the next sequential context directory.
     * A typical context directory is like: /tmp/pulsar-vincent/context/group/default/cx.1
     * */
    @Throws(IOException::class)
    private fun computeNextSequentialContextDir0(group: String, fingerprint: Fingerprint, maxAgents: Int, channel: FileChannel): Path {
        require(channel.isOpen) { "The lock file channel is closed" }

        val prefix = CONTEXT_DIR_PREFIX
        val groupBaseDir = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve(group).resolve(fingerprint.browserType.name)

        val expectedContextPaths = IntRange(1, maxAgents)
            .map { String.format("%s%s", prefix, it) }
            .map { groupBaseDir.resolve(it) }
        expectedContextPaths.filter { it.notExists() }.forEach {
            Files.createDirectories(it)
        }

        val contextGroup = contextGroups.computeIfAbsent(group) { ContextGroup(group) }
        Files.list(groupBaseDir)
            .filter { Files.isDirectory(it) && it.fileName.toString().startsWith(prefix) }
            .forEach { contextGroup.add(it) }

        return contextGroup.iterator.next()
    }

    /**
     * Compute the next sequential context directory.
     * A typical context directory is like: /tmp/pulsar-vincent/context/group/default/cx.1
     * */
    @Throws(IOException::class)
    private fun computeNextSequentialContextDir0Old(group: String, fingerprint: Fingerprint, maxAgents: Int, channel: FileChannel): Path {
        require(channel.isOpen) { "The lock file channel is closed" }
        
        val prefix = CONTEXT_DIR_PREFIX
        val groupBaseDir = AppPaths.CONTEXT_GROUP_BASE_DIR.resolve(group).resolve(fingerprint.browserType.name)
        Files.createDirectories(groupBaseDir)
        val contextGroup = contextGroups.computeIfAbsent(group) { ContextGroup(group) }
        
        Files.list(groupBaseDir)
            .filter { Files.isDirectory(it) && it.fileName.toString().startsWith(prefix) }
            .forEach { contextGroup.add(it) }
        
        // logger.info("contextGroup.size: ${contextGroup.size} maxContexts: $maxContexts")
        
        if (contextGroup.size >= maxAgents) {
            return contextGroup.iterator.next()
        }
        
        val contextCount = computeContextCount(groupBaseDir, prefix, channel)
        
        val fileName = String.format("%s%s", prefix, contextCount)
        val path = groupBaseDir.resolve(fileName)
        Files.createDirectories(path)
        
        logger.info("New privacy context dir: $fileName contextGroup.size: ${contextGroup.size} maxAgents: $maxAgents")
        
        return path
    }

    /**
     * Compute a random context directory.
     * A typical context directory is like: /tmp/pulsar-vincent/context/tmp/01/cx.0109aNcTxq5
     * */
    @Throws(IOException::class)
    private fun computeRandomContextDir0(group: String, channel: FileChannel? = null): Path {
        if (channel != null) {
            require(channel.isOpen) { "The lock file channel is closed" }
        }
        
        val prefix = CONTEXT_DIR_PREFIX
        val monthDay = MonthDay.now()
        val monthValue = monthDay.monthValue
        val dayOfMonth = monthDay.dayOfMonth
        val baseDir = AppPaths.CONTEXT_TMP_DIR.resolve("$monthValue")
        Files.createDirectories(baseDir)
        val rand = RandomStringUtils.randomAlphanumeric(5)
        val contextCount = computeContextCount(baseDir, prefix, channel)
        val fileName = String.format("%s%02d%02d%s%s", prefix, monthValue, dayOfMonth, rand, contextCount)
        val path = baseDir.resolve(group).resolve(fileName)
        Files.createDirectories(baseDir)
        return path
    }
    
    private fun computeContextCount(baseDir: Path, prefix: String, channel: FileChannel? = null): Long {
        if (channel != null) {
            require(channel.isOpen) { "The lock file channel is closed" }
        }

        return 1 + Files.list(baseDir)
            .filter { Files.isDirectory(it) }
            .filter { it.toString().contains(prefix) }
            .count()
    }
}
