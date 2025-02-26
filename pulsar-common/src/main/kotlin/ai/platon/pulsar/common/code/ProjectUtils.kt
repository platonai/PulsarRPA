package ai.platon.pulsar.common.code

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists
import kotlin.jvm.optionals.getOrNull

object ProjectUtils {
    /**
     * Find project root directory. It must contain a file named `VERSION`.
     * */
    fun findProjectRootDir(): Path? = findProjectRootDir(Paths.get(".").toAbsolutePath().normalize())

    /**
     * Find project root directory. It must contain a file named `VERSION`.
     * */
    fun findProjectRootDir(startDir: Path): Path? {
        var projectRootDir: Path? = startDir

        while (projectRootDir != null && projectRootDir.resolve("VERSION").notExists()) {
            projectRootDir = projectRootDir.parent
        }

        return projectRootDir
    }

    /**
     * Walk from the base directory to find a file.
     *
     * @param fileName The file name to find.
     * @param baseDir The base directory to start from.
     * @return The file path if found, otherwise null.
     * */
    fun walkToFindFile(fileName: String, baseDir: Path): Path? {
        return Files.walk(baseDir)
            .filter { it.fileName.toString() == fileName }
            .findFirst().getOrNull()
    }

    /**
     * Find the project root directory, and then walk to find a file.
     *
     * @param fileName The file name to find.
     * @return The file path if found, otherwise null.
     * */
    fun findFile(fileName: String): Path? {
        val projectRootDir = findProjectRootDir()
        return if (projectRootDir != null) {
            walkToFindFile(fileName, projectRootDir)
        } else null
    }
}
