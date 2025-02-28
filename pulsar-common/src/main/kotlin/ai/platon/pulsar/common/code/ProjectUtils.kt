package ai.platon.pulsar.common.code

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists
import kotlin.jvm.optionals.getOrNull

/**
 * A utility class for project-related operations, such as locating the project root directory
 * or finding specific files within the project structure.
 */
object ProjectUtils {
    /**
     * Finds the project root directory by searching for a file named `VERSION` in the current directory
     * and its parent directories.
     *
     * @return The project root directory if found, otherwise null.
     */
    fun findProjectRootDir(): Path? = findProjectRootDir(Paths.get(".").toAbsolutePath().normalize())

    /**
     * Finds the project root directory by searching for a file named `VERSION` starting from the specified directory
     * and traversing up its parent directories.
     *
     * @param startDir The directory to start the search from.
     * @return The project root directory if found, otherwise null.
     */
    fun findProjectRootDir(startDir: Path): Path? {
        var projectRootDir: Path? = startDir

        while (projectRootDir != null && projectRootDir.resolve("VERSION").notExists()) {
            projectRootDir = projectRootDir.parent
        }

        return projectRootDir
    }

    /**
     * Walks through the directory tree starting from the specified base directory to find a file with the given name.
     *
     * @param fileName The name of the file to find.
     * @param baseDir The directory to start the search from.
     * @return The path to the file if found, otherwise null.
     */
    fun walkToFindFile(fileName: String, baseDir: Path): Path? {
        return Files.walk(baseDir)
            .filter { it.fileName.toString() == fileName }
            .findFirst().getOrNull()
    }

    /**
     * Finds the project root directory and then searches for a file with the specified name within the project structure.
     *
     * @param fileName The name of the file to find.
     * @return The path to the file if found, otherwise null.
     */
    fun findFile(fileName: String): Path? {
        val projectRootDir = findProjectRootDir()
        return if (projectRootDir != null) {
            walkToFindFile(fileName, projectRootDir)
        } else null
    }
}
