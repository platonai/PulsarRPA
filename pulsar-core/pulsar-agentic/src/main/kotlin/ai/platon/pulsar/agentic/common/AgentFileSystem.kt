package ai.platon.pulsar.agentic.common

import ai.platon.pulsar.common.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private const val INVALID_FILENAME_ERROR_MESSAGE =
    "Error: Invalid fileName format. Must be alphanumeric with supported extension."
private const val DEFAULT_FILE_SYSTEM_PATH = "fs"

/** Custom exception for file system operations that should be shown to LLM */
class FileSystemError(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Base class for all file types */
sealed class BaseFile(
    open val name: String,
    open var content: String = ""
) {
    /** File extension (e.g. "txt", "md") */
    abstract val extension: String

    val fullName: String get() = "$name.$extension"

    fun writeFileContent(newContent: String) {
        updateContent(newContent)
    }

    fun appendFileContent(append: String) {
        updateContent(content + append)
    }

    protected fun updateContent(newContent: String) {
        content = newContent
    }

    // Align method names with java.nio.file.Files for a more idiomatic Kotlin/Java feel
    @Throws(IOException::class)
    open fun writeString(baseDir: Path): Path {
        val filePath = baseDir.resolve(fullName)
        try {
            Files.createDirectories(filePath.parent)
            Files.writeString(filePath, content, StandardCharsets.UTF_8)

            return filePath
        } catch (e: Exception) {
            throw FileSystemError("Error: Could not write to file '$fullName'. ${e.message}", e)
        }
    }

    open suspend fun writeStringAsync(dataDir: Path) = withContext(Dispatchers.IO) { writeString(dataDir) }

    suspend fun writeString(newContent: String, dataDir: Path): Path {
        writeFileContent(newContent)
        return writeStringAsync(dataDir)
    }

    suspend fun appendString(append: String, dataDir: Path): Path {
        appendFileContent(append)
        return writeStringAsync(dataDir)
    }

    open fun content(): String = content

    val size: Int get() = content.length
    val lineCount: Int get() = content.split("\n").size
}

class MarkdownFile(override val name: String, override var content: String = "") : BaseFile(name, content) {
    override val extension: String get() = "md"
}

class TxtFile(override val name: String, override var content: String = "") : BaseFile(name, content) {
    override val extension: String get() = "txt"
}

class JsonFile(override val name: String, override var content: String = "") : BaseFile(name, content) {
    override val extension: String get() = "json"
}

class CsvFile(override val name: String, override var content: String = "") : BaseFile(name, content) {
    override val extension: String get() = "csv"
}

class JsonlFile(override val name: String, override var content: String = "") : BaseFile(name, content) {
    override val extension: String get() = "jsonl"
}

/** Serializable state of the file system */
data class FileStateEntry(val type: String, val name: String, val content: String)

data class FileSystemState(
    val files: Map<String, FileStateEntry> = emptyMap(), // full fileName -> file data
    val baseDir: String,
    val extractedContentCount: Int = 0
)

/** Enhanced file system with in-memory storage and multiple file type support */
class AgentFileSystem constructor(
    private val baseDir: Path = Paths.get("target"),
    createDefaultFiles: Boolean = true
) {
    companion object {
        const val DISPLAY_CHARS = 400

        val DEFAULT_FILES = listOf("todolist.md")
    }

    private val logger = getLogger(this)

    private val dataDir: Path = baseDir.resolve(DEFAULT_FILE_SYSTEM_PATH)

    private val fileFactories: Map<String, (String, String) -> BaseFile> = mapOf(
        "md" to { name, c -> MarkdownFile(name, c) },
        "txt" to { name, c -> TxtFile(name, c) },
        "json" to { name, c -> JsonFile(name, c) },
        "jsonl" to { name, c -> JsonlFile(name, c) },
        "csv" to { name, c -> CsvFile(name, c) },
    )

    private val files: MutableMap<String, BaseFile> = ConcurrentHashMap()
    private var extractedContentCount: Int = 0
    private val allowedExtensionsPattern: Pattern = run {
        val exts = fileFactories.keys.joinToString("|")
        Pattern.compile("^[a-zA-Z0-9_\\-]+\\.($exts)$")
    }

    init {
        // setup directories
        if (!baseDir.exists()) baseDir.createDirectories()
        if (dataDir.exists()) {
            // clean
            cleanDirectory(dataDir)
        }
        if (!dataDir.exists()) {
            dataDir.createDirectories()
        }

        if (createDefaultFiles) {
            for (full in DEFAULT_FILES) {
                val (name, ext) = parseFilename(full)
                val file = createFile(ext, name)
                files[full] = file
                file.writeString(dataDir)
            }
        }
    }

    fun getAllowedExtensions(): List<String> = fileFactories.keys.toList()

    private fun createFile(extension: String, name: String, content: String = ""): BaseFile {
        val factory = fileFactories[extension.lowercase()]
            ?: throw IllegalArgumentException("Error: Invalid file extension '$extension' for file '$name.$extension'.")
        return factory(name, content)
    }

    private fun isValidFilename(fileName: String): Boolean = allowedExtensionsPattern.matcher(fileName).matches()

    private fun parseFilename(fileName: String): Pair<String, String> {
        val idx = fileName.lastIndexOf('.')
        require(idx > 0 && idx < fileName.length - 1) { "Invalid fileName: $fileName" }
        val name = fileName.take(idx)
        val ext = fileName.substring(idx + 1).lowercase()
        return name to ext
    }

    fun getFile(fullFileName: String): BaseFile? {
        if (!isValidFilename(fullFileName)) return null
        return files[fullFileName]
    }

    fun listFiles(): List<String> = files.values.map { it.fullName }

    fun listOSFiles(): List<Path> = files.values.map { dataDir.resolve(it.fullName) }

    fun displayFile(fullFileName: String): String? {
        if (!isValidFilename(fullFileName)) return null
        val file = getFile(fullFileName) ?: return null
        return file.content()
    }

    suspend fun readString(fullFileName: String, externalFile: Boolean = false): String {
        if (externalFile) {
            return try {
                val ext = runCatching { parseFilename(fullFileName).second }.getOrElse {
                    return "Error: Invalid fileName format $fullFileName. Must be alphanumeric with a supported extension."
                }
                when (ext) {
                    "md", "txt", "json", "jsonl", "csv" -> {
                        val p = Path.of(fullFileName)
                        val content = withContext(Dispatchers.IO) {
                            Files.newBufferedReader(p, StandardCharsets.UTF_8).use { it.readText() }
                        }
                        "Read from file $fullFileName.\n<content>\n$content\n</content>"
                    }

                    else -> "Error: Cannot read file $fullFileName as $ext extension is not supported."
                }
            } catch (e: IOException) {
                "Error: Could not read file '$fullFileName'."
            } catch (_: SecurityException) {
                "Error: Permission denied to read file '$fullFileName'."
            }
        }

        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        val file = getFile(fullFileName) ?: return "File '$fullFileName' not found."

        return try {
            val content = file.content()
            "Read from file $fullFileName.\n<content>\n$content\n</content>"
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not read file '$fullFileName'."
        } catch (e: Exception) {
            "Error: Could not read file '$fullFileName'."
        }
    }

    suspend fun writeString(fullFileName: String, content: String): String {
        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        return try {
            val (name, ext) = parseFilename(fullFileName)
            val file = files[fullFileName] ?: createFile(ext, name).also { files[fullFileName] = it }
            val path = file.writeString(content, dataDir)

            // logger.info("Write to file | {}", path.toUri())

            "Data written to file $fullFileName successfully."
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not write to file '$fullFileName'."
        } catch (e: Exception) {
            "Error: Could not write to file '$fullFileName'. ${e.message ?: ""}".trim()
        }
    }

    suspend fun append(fullFileName: String, content: String): String {
        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        val file = getFile(fullFileName) ?: return "File '$fullFileName' not found."
        return try {
            file.appendString(content, dataDir)
            "Data appended to file $fullFileName successfully."
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not append to file '$fullFileName'."
        } catch (e: Exception) {
            "Error: Could not append to file '$fullFileName'. ${e.message ?: ""}".trim()
        }
    }

    suspend fun replaceContent(fullFileName: String, oldStr: String, newStr: String): String {
        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        if (oldStr.isEmpty()) return "Error: Cannot replace empty string. Please provide a non-empty string to replace."
        val file = getFile(fullFileName) ?: return "File '$fullFileName' not found."
        return try {
            val replaced = file.content().replace(oldStr, newStr)
            file.writeString(replaced, dataDir)
            "Successfully replaced all occurrences of \"$oldStr\" with \"$newStr\" in file $fullFileName"
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not replace string in file '$fullFileName'."
        } catch (e: Exception) {
            "Error: Could not replace string in file '$fullFileName'. ${e.message ?: ""}".trim()
        }
    }

    /**
     * Checks if a file exists in the agent file system.
     *
     * @param fullFileName The full file name including extension (e.g., "data.json")
     * @return A message indicating whether the file exists
     */
    fun fileExists(fullFileName: String): String {
        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        val exists = files.containsKey(fullFileName)
        return if (exists) {
            "File '$fullFileName' exists."
        } else {
            "File '$fullFileName' does not exist."
        }
    }

    /**
     * Returns information about a file including size and line count.
     *
     * @param fullFileName The full file name including extension (e.g., "data.json")
     * @return A message with file information or an error message
     */
    fun getFileInfo(fullFileName: String): String {
        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        val file = getFile(fullFileName) ?: return "File '$fullFileName' not found."
        return try {
            val content = file.content()
            val sizeBytes = content.toByteArray(StandardCharsets.UTF_8).size
            val lineCount = if (content.isEmpty()) 0 else content.split("\n").size
            val charCount = content.length
            """File info for '$fullFileName':
- Size: $sizeBytes bytes
- Characters: $charCount
- Lines: $lineCount
- Extension: ${file.extension}"""
        } catch (e: Exception) {
            "Error: Could not get info for file '$fullFileName'. ${e.message ?: ""}".trim()
        }
    }

    /**
     * Deletes a file from the agent file system.
     *
     * @param fullFileName The full file name including extension (e.g., "data.json")
     * @return A message indicating success or failure
     */
    suspend fun deleteFile(fullFileName: String): String {
        if (!isValidFilename(fullFileName)) return INVALID_FILENAME_ERROR_MESSAGE
        val file = files.remove(fullFileName) ?: return "File '$fullFileName' not found."
        return try {
            val filePath = dataDir.resolve(file.fullName)
            withContext(Dispatchers.IO) {
                if (filePath.exists()) {
                    Files.delete(filePath)
                }
            }
            "File '$fullFileName' deleted successfully."
        } catch (e: IOException) {
            // File removed from memory but OS file deletion failed
            "File '$fullFileName' removed from memory, but could not delete from disk: ${e.message}"
        } catch (e: Exception) {
            "Error: Could not delete file '$fullFileName'. ${e.message ?: ""}".trim()
        }
    }

    /**
     * Copies a file within the agent file system.
     *
     * @param sourceFileName The source file name (e.g., "source.txt")
     * @param destFileName The destination file name (e.g., "dest.txt")
     * @return A message indicating success or failure
     */
    suspend fun copyFile(sourceFileName: String, destFileName: String): String {
        if (!isValidFilename(sourceFileName)) return "Error: Invalid source fileName format. Must be alphanumeric with supported extension."
        if (!isValidFilename(destFileName)) return "Error: Invalid destination fileName format. Must be alphanumeric with supported extension."
        if (sourceFileName == destFileName) return "Error: Source and destination file names must be different."
        
        val sourceFile = getFile(sourceFileName) ?: return "Source file '$sourceFileName' not found."
        
        return try {
            val (destName, destExt) = parseFilename(destFileName)
            val newFile = createFile(destExt, destName, sourceFile.content())
            files[destFileName] = newFile
            newFile.writeString(dataDir)
            "File '$sourceFileName' copied to '$destFileName' successfully."
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not copy file."
        } catch (e: Exception) {
            "Error: Could not copy file '$sourceFileName' to '$destFileName'. ${e.message ?: ""}".trim()
        }
    }

    /**
     * Moves/renames a file within the agent file system.
     *
     * @param sourceFileName The source file name (e.g., "old.txt")
     * @param destFileName The destination file name (e.g., "new.txt")
     * @return A message indicating success or failure
     */
    suspend fun moveFile(sourceFileName: String, destFileName: String): String {
        if (!isValidFilename(sourceFileName)) return "Error: Invalid source fileName format. Must be alphanumeric with supported extension."
        if (!isValidFilename(destFileName)) return "Error: Invalid destination fileName format. Must be alphanumeric with supported extension."
        if (sourceFileName == destFileName) return "Error: Source and destination file names must be different."
        
        val sourceFile = files.remove(sourceFileName) ?: return "Source file '$sourceFileName' not found."
        
        return try {
            // Delete old file from disk
            val oldPath = dataDir.resolve(sourceFile.fullName)
            withContext(Dispatchers.IO) {
                if (oldPath.exists()) {
                    Files.delete(oldPath)
                }
            }
            
            // Create new file with same content
            val (destName, destExt) = parseFilename(destFileName)
            val newFile = createFile(destExt, destName, sourceFile.content())
            files[destFileName] = newFile
            newFile.writeString(dataDir)
            
            "File '$sourceFileName' moved to '$destFileName' successfully."
        } catch (e: FileSystemError) {
            // Restore source file on failure
            files[sourceFileName] = sourceFile
            e.message ?: "Error: Could not move file."
        } catch (e: Exception) {
            // Restore source file on failure
            files[sourceFileName] = sourceFile
            "Error: Could not move file '$sourceFileName' to '$destFileName'. ${e.message ?: ""}".trim()
        }
    }

    /**
     * Lists all files in the agent file system with their basic info.
     *
     * @return A formatted string listing all files
     */
    fun listFilesInfo(): String {
        if (files.isEmpty()) {
            return "No files in the file system."
        }
        
        val sb = StringBuilder()
        sb.appendLine("Files in agent file system (${files.size} files):")
        for ((fileName, file) in files) {
            val content = file.content()
            val sizeBytes = content.toByteArray(StandardCharsets.UTF_8).size
            val lineCount = if (content.isEmpty()) 0 else content.split("\n").size
            sb.appendLine("- $fileName (${sizeBytes} bytes, $lineCount lines)")
        }
        return sb.toString().trimEnd()
    }

    suspend fun saveExtractedContent(content: String): String {
        val initial = "extracted_content_$extractedContentCount"
        val fileName = "$initial.md"
        val file = MarkdownFile(initial)
        file.writeString(content, dataDir)
        files[fileName] = file
        extractedContentCount += 1
        return fileName
    }

    fun describe(): String {
        val sb = StringBuilder()
        for (file in files.values) {
            if (file.fullName == "todolist.md") continue
            val content = file.content()
            if (content.isEmpty()) {
                sb.append("<file>\n${file.fullName} - [empty file]\n</file>\n")
                continue
            }
            val lines = content.split("\n")
            val lineCount = lines.size
            val whole = "<file>\n${file.fullName} - $lineCount lines\n<content>\n$content\n</content>\n</file>\n"
            if (content.length < (1.5 * DISPLAY_CHARS).toInt()) {
                sb.append(whole)
                continue
            }
            val half = DISPLAY_CHARS / 2
            var chars = 0
            var startLineCount = 0
            val startPreview = StringBuilder()
            for (line in lines) {
                if (chars + line.length + 1 > half) break
                startPreview.append(line).append('\n')
                chars += line.length + 1
                startLineCount += 1
            }
            chars = 0
            var endLineCount = 0
            val endPreview = StringBuilder()
            for (line in lines.asReversed()) {
                if (chars + line.length + 1 > half) break
                endPreview.insert(0, line + '\n')
                chars += line.length + 1
                endLineCount += 1
            }
            val middle = lineCount - startLineCount - endLineCount
            if (middle <= 0) {
                sb.append(whole)
                continue
            }
            val start = startPreview.toString().trim('\n').trimEnd()
            val end = endPreview.toString().trim('\n').trimEnd()
            if (start.isEmpty() && end.isEmpty()) {
                sb.append("<file>\n${file.fullName} - $lineCount lines\n<content>\n$middle lines...\n</content>\n</file>\n")
            } else {
                sb.append("<file>\n${file.fullName} - $lineCount lines\n<content>\n$start\n")
                sb.append("... $middle more lines ...\n")
                sb.append("$end\n")
                sb.append("</content>\n</file>\n")
            }
        }
        return sb.toString().trimEnd('\n')
    }

    fun getTodoContents(): String = getFile("todolist.md")?.content() ?: ""

    fun getState(): FileSystemState {
        val map = files.mapValues { (_, f) -> FileStateEntry(f::class.simpleName ?: "", f.name, f.content) }
        return FileSystemState(files = map, baseDir = baseDir.toString(), extractedContentCount = extractedContentCount)
    }

    private fun cleanDirectory(dir: Path) {
        if (!dir.exists()) return
        if (!dir.isDirectory()) return
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach { p -> if (p != dir) p.toFile().delete() }
    }
}

suspend fun main() {
    val fs = AgentFileSystem()

    fs.writeString("todolist.md", "todolist.md")
    fs.listFiles().forEach { println(it) }
}
