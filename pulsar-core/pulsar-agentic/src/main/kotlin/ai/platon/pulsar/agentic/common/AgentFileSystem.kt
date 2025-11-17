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
    "Error: Invalid filename format. Must be alphanumeric with supported extension."
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
    val files: Map<String, FileStateEntry> = emptyMap(), // full filename -> file data
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

    private fun parseFilename(filename: String): Pair<String, String> {
        val idx = filename.lastIndexOf('.')
        require(idx > 0 && idx < filename.length - 1) { "Invalid filename: $filename" }
        val name = filename.take(idx)
        val ext = filename.substring(idx + 1).lowercase()
        return name to ext
    }

    fun getFile(fullFilename: String): BaseFile? {
        if (!isValidFilename(fullFilename)) return null
        return files[fullFilename]
    }

    fun listFiles(): List<String> = files.values.map { it.fullName }

    fun displayFile(fullFilename: String): String? {
        if (!isValidFilename(fullFilename)) return null
        val file = getFile(fullFilename) ?: return null
        return file.content()
    }

    suspend fun readString(fullFilename: String, externalFile: Boolean = false): String {
        if (externalFile) {
            return try {
                val ext = runCatching { parseFilename(fullFilename).second }.getOrElse {
                    return "Error: Invalid filename format $fullFilename. Must be alphanumeric with a supported extension."
                }
                when (ext) {
                    "md", "txt", "json", "jsonl", "csv" -> {
                        val p = Path.of(fullFilename)
                        val content = withContext(Dispatchers.IO) {
                            Files.newBufferedReader(p, StandardCharsets.UTF_8).use { it.readText() }
                        }
                        "Read from file $fullFilename.\n<content>\n$content\n</content>"
                    }

                    else -> "Error: Cannot read file $fullFilename as $ext extension is not supported."
                }
            } catch (e: IOException) {
                "Error: Could not read file '$fullFilename'."
            } catch (_: SecurityException) {
                "Error: Permission denied to read file '$fullFilename'."
            }
        }

        if (!isValidFilename(fullFilename)) return INVALID_FILENAME_ERROR_MESSAGE
        val file = getFile(fullFilename) ?: return "File '$fullFilename' not found."

        return try {
            val content = file.content()
            "Read from file $fullFilename.\n<content>\n$content\n</content>"
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not read file '$fullFilename'."
        } catch (e: Exception) {
            "Error: Could not read file '$fullFilename'."
        }
    }

    suspend fun writeString(fullFilename: String, content: String): String {
        if (!isValidFilename(fullFilename)) return INVALID_FILENAME_ERROR_MESSAGE
        return try {
            val (name, ext) = parseFilename(fullFilename)
            val file = files[fullFilename] ?: createFile(ext, name).also { files[fullFilename] = it }
            val path = file.writeString(content, dataDir)

            logger.info("Write to file | {}", path.toUri())

            "Data written to file $fullFilename successfully."
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not write to file '$fullFilename'."
        } catch (e: Exception) {
            "Error: Could not write to file '$fullFilename'. ${e.message ?: ""}".trim()
        }
    }

    suspend fun append(fullFilename: String, content: String): String {
        if (!isValidFilename(fullFilename)) return INVALID_FILENAME_ERROR_MESSAGE
        val file = getFile(fullFilename) ?: return "File '$fullFilename' not found."
        return try {
            file.appendString(content, dataDir)
            "Data appended to file $fullFilename successfully."
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not append to file '$fullFilename'."
        } catch (e: Exception) {
            "Error: Could not append to file '$fullFilename'. ${e.message ?: ""}".trim()
        }
    }

    suspend fun replaceContent(fullFilename: String, oldStr: String, newStr: String): String {
        if (!isValidFilename(fullFilename)) return INVALID_FILENAME_ERROR_MESSAGE
        if (oldStr.isEmpty()) return "Error: Cannot replace empty string. Please provide a non-empty string to replace."
        val file = getFile(fullFilename) ?: return "File '$fullFilename' not found."
        return try {
            val replaced = file.content().replace(oldStr, newStr)
            file.writeString(replaced, dataDir)
            "Successfully replaced all occurrences of \"$oldStr\" with \"$newStr\" in file $fullFilename"
        } catch (e: FileSystemError) {
            e.message ?: "Error: Could not replace string in file '$fullFilename'."
        } catch (e: Exception) {
            "Error: Could not replace string in file '$fullFilename'. ${e.message ?: ""}".trim()
        }
    }

    suspend fun saveExtractedContent(content: String): String {
        val initial = "extracted_content_$extractedContentCount"
        val filename = "$initial.md"
        val file = MarkdownFile(initial)
        file.writeString(content, dataDir)
        files[filename] = file
        extractedContentCount += 1
        return filename
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
