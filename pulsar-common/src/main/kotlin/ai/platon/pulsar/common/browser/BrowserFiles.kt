package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.*
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.MonthDay
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isDirectory

object BrowserFiles {
    
    private val logger = getLogger(this)
    /**
     * The sequencer to generate temporary context directories.
     * */
    private val SEQUENCER = AtomicInteger()
    
    // The prefix for all temporary privacy contexts. System context, prototype context and default context are not
    // required to start with the prefix.
    const val CONTEXT_DIR_PREFIX = "cx."
    
    const val PID_FILE_NAME = "launcher.pid"
    
    val TEMPORARY_UDD_EXPIRY = Duration.ofHours(1)

    private val cleanedUserDataDirs = ConcurrentSkipListSet<Path>()
    
    @Throws(IOException::class)
    @Synchronized
    fun computeNextSequentialContextDir(): Path {
        return runWithFileLock { computeNextSequentialContextDir0() }
    }

    @Throws(IOException::class)
    fun cleanUpContextTmpDir(expiry: Duration) {
        val hasSiblingPidFile: (Path) -> Boolean = { path -> Files.exists(path.resolveSibling(PID_FILE_NAME)) }
        Files.walk(AppPaths.CONTEXT_TMP_DIR)
            .filter { it !in cleanedUserDataDirs }
            .filter { it.isDirectory() && hasSiblingPidFile(it) }.forEach { path ->
                deleteTemporaryUserDataDirWithLock(path, expiry)
            }
    }
    
    @Throws(IOException::class)
    fun deleteTemporaryUserDataDirWithLock(userDataDir: Path, expiry: Duration) {
        runWithFileLock { deleteTemporaryUserDataDir0(userDataDir, expiry) }
    }
    
    @Throws(IOException::class)
    @Synchronized
    private fun <T> runWithFileLock(supplier: () -> T): T {
        val lockFile = AppPaths.BROWSER_TMP_DIR_LOCK
        // Opens or creates a file, returning a file channel to access the file.
        val channel = FileChannel.open(lockFile, StandardOpenOption.APPEND)
        channel.use {
            it.lock()
            return supplier()
        }
    }

    @Throws(IOException::class)
    private fun deleteTemporaryUserDataDir0(userDataDir: Path, expiry: Duration) {
        val dirToDelete = userDataDir
        
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
            logger.error("Could not delete browser data | {}", dirToDelete)
        } else {
            cleanedUserDataDirs.add(dirToDelete)
        }
    }

    @Throws(IOException::class)
    private fun computeNextSequentialContextDir0(): Path {
        val prefix = CONTEXT_DIR_PREFIX
        val monthDay = MonthDay.now()
        val monthValue = monthDay.monthValue
        val dayOfMonth = monthDay.dayOfMonth
        val baseDir = AppPaths.CONTEXT_TMP_DIR.resolve("$monthValue")
        Files.createDirectories(baseDir)
        val sequence = SEQUENCER.incrementAndGet()
        val rand = RandomStringUtils.randomAlphanumeric(5)
        val contextCount = 1 + Files.list(baseDir)
            .filter { Files.isDirectory(it) }
            .filter { it.toString().contains(prefix) }
            .count()
        val fileName = String.format("%s%02d%02d%s%s%s",
            prefix, monthValue, dayOfMonth, sequence, rand, contextCount)
        return baseDir.resolve(fileName)
    }
}
