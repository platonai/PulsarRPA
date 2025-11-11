package ai.platon.pulsar.skeleton.common.llm

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.code.ProjectUtils
import kotlinx.io.files.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.io.path.notExists

object LLMUtils {

    const val CODE_MIRROR_DIR = "code-mirror"

    fun copyWebDriverAsResource() {
        copySourceFileAsResource("WebDriver.kt")
    }

    fun copySourceFileAsResource(filename: String) {
        if (ProjectUtils.findProjectRootDir() == null) {
            // we are not in a source code project
            return
        }

        val file = ProjectUtils.findFile(filename) ?: throw FileNotFoundException(filename)
        ProjectUtils.copySourceFileAsCodeResource(file)
    }

    fun readWebDriverFromResource(): String {
        copyWebDriverAsResource()
        val resource = "$CODE_MIRROR_DIR/WebDriver.kt"
        return ResourceLoader.readString(resource)
    }

    fun readSourceFileFromResource(fileName: String): String {
        copySourceFileAsResource(fileName)
        val resource = "$CODE_MIRROR_DIR/$fileName"
        return ResourceLoader.readString(resource)
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
