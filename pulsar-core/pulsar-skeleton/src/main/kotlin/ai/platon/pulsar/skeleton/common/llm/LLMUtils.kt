package ai.platon.pulsar.skeleton.common.llm

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.code.ProjectUtils
import ai.platon.pulsar.common.code.ProjectUtils.CODE_MIRROR_DIR
import kotlinx.io.files.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists

object LLMUtils {

    fun copySourceFileAsResource(filename: String) {
        if (ProjectUtils.findProjectRootDir() == null) {
            // we are not in a source code project
            return
        }

        val file = ProjectUtils.findFile(filename) ?: throw FileNotFoundException(filename)
        ProjectUtils.copySourceFileAsCodeResource(file)
    }

    fun readSourceFileFromResource(fileName: String): String {
        copySourceFileAsResource(fileName)

        val resource = "$CODE_MIRROR_DIR/$fileName.txt"
        return when {
            ResourceLoader.exists(resource) -> ResourceLoader.readString(resource)
            else -> ResourceLoader.readString("$CODE_MIRROR_DIR/$fileName") // fallback
        }
    }

    fun writeAsResource(fileName: String, content: String): Path? {
        val baseDir = ProjectUtils.findFile(CODE_MIRROR_DIR) ?: return null
        if (baseDir.notExists()) {
            return null
        }

        val path = baseDir.resolve(fileName)
        Files.writeString(path, content)
        return path
    }
}
