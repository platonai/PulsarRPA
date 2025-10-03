package ai.platon.pulsar.common.browser

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.test.assertTrue

class BrowserFilesTest {
    private val executor = Executors.newWorkStealingPool()

    @AfterEach
    fun tearDown() {
        // BrowserFiles.deleteTemporaryUserDataDirWithLock(AppPaths.CONTEXT_TMP_DIR, Duration.ofSeconds(0))
    }
    
    @Test
    fun computeRandomTmpContextDir() {
        val paths = ConcurrentHashMap<String, Path>()
        repeat(20) {
            executor.submit {
                val path = BrowserFiles.computeRandomTmpContextDir()
                // println(path)
                paths[path.fileName.toString()] = path
                Files.createDirectory(path)
            }
        }

        executor.awaitTermination(10, TimeUnit.SECONDS)

        paths.values.forEach {
            assertTrue { it.exists() }
            assertTrue { it.fileName.toString().startsWith("cx.") }
        }
    }
}