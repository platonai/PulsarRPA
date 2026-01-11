package ai.platon.pulsar.agentic.common

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AgentFileSystemTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var fs: AgentFileSystem

    @BeforeEach
    fun setUp() {
        fs = AgentFileSystem(tempDir, createDefaultFiles = false)
    }

    @AfterEach
    fun tearDown() {
        // Cleanup handled by TempDir
    }

    // --- Basic file operations ---

    @Test
    fun `writeString creates new file`() = runBlocking {
        val result = fs.writeString("test.txt", "Hello, World!")
        assertTrue(result.contains("successfully"))
        assertEquals(listOf("test.txt"), fs.listFiles())
    }

    @Test
    fun `readString returns file content`() = runBlocking {
        fs.writeString("test.txt", "Hello, World!")
        val result = fs.readString("test.txt")
        assertTrue(result.contains("Hello, World!"))
        assertTrue(result.contains("<content>"))
    }

    @Test
    fun `readString returns error for non-existent file`() = runBlocking {
        val result = fs.readString("nonexistent.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `append adds content to existing file`() = runBlocking {
        fs.writeString("test.txt", "Line 1\n")
        fs.append("test.txt", "Line 2\n")
        val result = fs.readString("test.txt")
        assertTrue(result.contains("Line 1"))
        assertTrue(result.contains("Line 2"))
    }

    @Test
    fun `append returns error for non-existent file`() = runBlocking {
        val result = fs.append("nonexistent.txt", "content")
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `replaceContent replaces string in file`() = runBlocking {
        fs.writeString("test.txt", "Hello, World!")
        val result = fs.replaceContent("test.txt", "World", "Universe")
        assertTrue(result.contains("Successfully"))
        val content = fs.readString("test.txt")
        assertTrue(content.contains("Universe"))
        assertFalse(content.contains("World"))
    }

    @Test
    fun `replaceContent returns error for empty oldStr`() = runBlocking {
        fs.writeString("test.txt", "Hello, World!")
        val result = fs.replaceContent("test.txt", "", "new")
        assertTrue(result.contains("Cannot replace empty string"))
    }

    // --- New file operations ---

    @Test
    fun `fileExists returns exists for existing file`() = runBlocking {
        fs.writeString("test.txt", "content")
        val result = fs.fileExists("test.txt")
        assertTrue(result.contains("exists"))
        assertFalse(result.contains("does not"))
    }

    @Test
    fun `fileExists returns not exists for missing file`() = runBlocking {
        val result = fs.fileExists("nonexistent.txt")
        assertTrue(result.contains("does not exist"))
    }

    @Test
    fun `getFileInfo returns file metadata`() = runBlocking {
        fs.writeString("test.txt", "Line 1\nLine 2\nLine 3")
        val result = fs.getFileInfo("test.txt")
        assertTrue(result.contains("Size:"))
        assertTrue(result.contains("Lines: 3"))
        assertTrue(result.contains("Extension: txt"))
    }

    @Test
    fun `getFileInfo returns error for non-existent file`() = runBlocking {
        val result = fs.getFileInfo("nonexistent.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `deleteFile removes file`() = runBlocking {
        fs.writeString("test.txt", "content")
        assertTrue(fs.listFiles().contains("test.txt"))
        
        val result = fs.deleteFile("test.txt")
        assertTrue(result.contains("deleted successfully"))
        assertFalse(fs.listFiles().contains("test.txt"))
    }

    @Test
    fun `deleteFile returns error for non-existent file`() = runBlocking {
        val result = fs.deleteFile("nonexistent.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `copyFile creates copy with same content`() = runBlocking {
        fs.writeString("source.txt", "Original content")
        val result = fs.copyFile("source.txt", "dest.txt")
        assertTrue(result.contains("copied"))
        
        // Both files should exist
        assertTrue(fs.listFiles().contains("source.txt"))
        assertTrue(fs.listFiles().contains("dest.txt"))
        
        // Content should be the same
        val sourceContent = fs.readString("source.txt")
        val destContent = fs.readString("dest.txt")
        assertTrue(sourceContent.contains("Original content"))
        assertTrue(destContent.contains("Original content"))
    }

    @Test
    fun `copyFile returns error for non-existent source`() = runBlocking {
        val result = fs.copyFile("nonexistent.txt", "dest.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `copyFile returns error when source equals dest`() = runBlocking {
        fs.writeString("test.txt", "content")
        val result = fs.copyFile("test.txt", "test.txt")
        assertTrue(result.contains("must be different"))
    }

    @Test
    fun `moveFile moves file to new name`() = runBlocking {
        fs.writeString("old.txt", "Content to move")
        val result = fs.moveFile("old.txt", "new.txt")
        assertTrue(result.contains("moved"))
        
        // Old file should not exist, new file should
        assertFalse(fs.listFiles().contains("old.txt"))
        assertTrue(fs.listFiles().contains("new.txt"))
        
        // Content should be preserved
        val content = fs.readString("new.txt")
        assertTrue(content.contains("Content to move"))
    }

    @Test
    fun `moveFile returns error for non-existent source`() = runBlocking {
        val result = fs.moveFile("nonexistent.txt", "dest.txt")
        assertTrue(result.contains("not found"))
    }

    @Test
    fun `moveFile returns error when source equals dest`() = runBlocking {
        fs.writeString("test.txt", "content")
        val result = fs.moveFile("test.txt", "test.txt")
        assertTrue(result.contains("must be different"))
    }

    @Test
    fun `listFilesInfo returns formatted file list`() = runBlocking {
        fs.writeString("file1.txt", "Content 1")
        fs.writeString("file2.md", "# Markdown content")
        
        val result = fs.listFilesInfo()
        assertTrue(result.contains("2 files"))
        assertTrue(result.contains("file1.txt"))
        assertTrue(result.contains("file2.md"))
        assertTrue(result.contains("bytes"))
        assertTrue(result.contains("lines"))
    }

    @Test
    fun `listFilesInfo returns empty message when no files`() = runBlocking {
        val result = fs.listFilesInfo()
        assertTrue(result.contains("No files"))
    }

    // --- File extension validation ---

    @Test
    fun `writeString rejects invalid extension`() = runBlocking {
        val result = fs.writeString("test.exe", "content")
        assertTrue(result.contains("Invalid"))
    }

    @Test
    fun `supports all valid extensions`() = runBlocking {
        val extensions = listOf("md", "txt", "json", "jsonl", "csv")
        for (ext in extensions) {
            val result = fs.writeString("test.$ext", "content")
            assertTrue(result.contains("successfully"), "Failed for extension: $ext")
        }
    }

    @Test
    fun `rejects filenames with special characters`() = runBlocking {
        val invalidNames = listOf("test file.txt", "test/path.txt", "test..txt")
        for (name in invalidNames) {
            val result = fs.writeString(name, "content")
            assertTrue(result.contains("Invalid") || result.contains("Error"), "Should reject: $name")
        }
    }

    // --- Edge cases ---

    @Test
    fun `handles empty file content`() = runBlocking {
        fs.writeString("empty.txt", "")
        val info = fs.getFileInfo("empty.txt")
        assertTrue(info.contains("Lines: 0"))
    }

    @Test
    fun `handles multi-line content correctly`() = runBlocking {
        val multiLine = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
        fs.writeString("multiline.txt", multiLine)
        val info = fs.getFileInfo("multiline.txt")
        assertTrue(info.contains("Lines: 5"))
    }

    @Test
    fun `copyFile can change extension`() = runBlocking {
        fs.writeString("source.txt", "Content")
        val result = fs.copyFile("source.txt", "dest.md")
        assertTrue(result.contains("copied"))
        assertTrue(fs.listFiles().contains("dest.md"))
    }

    @Test
    fun `moveFile can change extension`() = runBlocking {
        fs.writeString("source.txt", "Content")
        val result = fs.moveFile("source.txt", "dest.md")
        assertTrue(result.contains("moved"))
        assertTrue(fs.listFiles().contains("dest.md"))
        assertFalse(fs.listFiles().contains("source.txt"))
    }

    // --- Concurrent access tests ---

    @Test
    fun `handles multiple concurrent writes`() = runBlocking {
        val files = (1..10).map { "file$it.txt" }
        files.forEach { fs.writeString(it, "Content for $it") }
        
        assertEquals(10, fs.listFiles().size)
        files.forEach { assertTrue(fs.listFiles().contains(it)) }
    }
}
