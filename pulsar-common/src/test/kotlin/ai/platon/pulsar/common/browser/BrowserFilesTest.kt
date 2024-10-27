package ai.platon.pulsar.common.browser

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BrowserFilesTest {
    private val executor = Executors.newWorkStealingPool()

    @AfterEach
    fun tearDown() {
        // BrowserFiles.deleteTemporaryUserDataDirWithLock(AppPaths.CONTEXT_TMP_DIR, Duration.ofSeconds(0))
    }
    
    @Test
    fun computeRandomTmpContextDir() {
        repeat(20) {
            executor.submit {
                val path = BrowserFiles.computeRandomTmpContextDir()
                println(path)
                Files.createDirectory(path)
            }
        }
        
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }
}