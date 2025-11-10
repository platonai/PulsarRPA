package ai.platon.pulsar.skeleton.common.llm

import ai.platon.pulsar.common.code.ProjectUtils
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.io.path.notExists

object LLMUtils {

    fun copyWebDriverAsResource() {
        val file = ProjectUtils.findFile("WebDriver.kt")
        if (file != null) {
            ProjectUtils.copyFileAsCodeResource(file)
        }
    }

    fun copyWebDriverFile(dest: Path) {
        val file = ProjectUtils.findFile("WebDriver.kt")
        if (file != null) {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun copyPulsarSessionFile(dest: Path) {
        val file = ProjectUtils.findFile("PulsarSession.kt")
        if (file != null) {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING)
            return
        }
    }

    /**
     * Check if we should copy the web driver file to the destination
     *
     * 1. If the destination file does not exist, copy it
     * 2. If the destination exists and the file size is less than 100 bytes, copy it
     * 3. If the destination exists, and it's older than 1 day, copy it
     *
     * @param dest The destination directory
     * */
    private fun shouldCopyFile(dest: Path): Boolean {
        return dest.notExists() || Files.size(dest) < 100
                || Files.getLastModifiedTime(dest).toInstant() < Instant.now().minus(1, ChronoUnit.HOURS)
    }
}
