package ai.platon.pulsar.agentic.tools.executors

import ai.platon.pulsar.agentic.ToolCall
import ai.platon.pulsar.agentic.common.AgentFileSystem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FileSystemToolExecutorTest {

    private lateinit var fs: AgentFileSystem
    private lateinit var executor: FileSystemToolExecutor

    @BeforeEach
    fun setUp() {
        fs = mockk(relaxed = true)
        executor = FileSystemToolExecutor()
    }

    @Test
    fun `writeString calls fs writeString with correct args`() = runBlocking {
        coEvery { fs.writeString(any(), any()) } returns "success"
        
        val tc = ToolCall(
            domain = "fs",
            method = "writeString",
            arguments = mutableMapOf("filename" to "test.txt", "content" to "Hello")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.writeString("test.txt", "Hello") }
    }

    @Test
    fun `writeString allows empty content`() = runBlocking {
        coEvery { fs.writeString(any(), any()) } returns "success"
        
        val tc = ToolCall(
            domain = "fs",
            method = "writeString",
            arguments = mutableMapOf("filename" to "test.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.writeString("test.txt", "") }
    }

    @Test
    fun `readString calls fs readString with correct args`() = runBlocking {
        coEvery { fs.readString(any(), any()) } returns "content"
        
        val tc = ToolCall(
            domain = "fs",
            method = "readString",
            arguments = mutableMapOf("filename" to "test.txt", "external" to "true")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.readString("test.txt", true) }
    }

    @Test
    fun `readString defaults external to false`() = runBlocking {
        coEvery { fs.readString(any(), any()) } returns "content"
        
        val tc = ToolCall(
            domain = "fs",
            method = "readString",
            arguments = mutableMapOf("filename" to "test.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.readString("test.txt", false) }
    }

    @Test
    fun `append calls fs append with correct args`() = runBlocking {
        coEvery { fs.append(any(), any()) } returns "success"
        
        val tc = ToolCall(
            domain = "fs",
            method = "append",
            arguments = mutableMapOf("filename" to "test.txt", "content" to "new content")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.append("test.txt", "new content") }
    }

    @Test
    fun `replaceContent calls fs replaceContent with correct args`() = runBlocking {
        coEvery { fs.replaceContent(any(), any(), any()) } returns "success"
        
        val tc = ToolCall(
            domain = "fs",
            method = "replaceContent",
            arguments = mutableMapOf("filename" to "test.txt", "oldStr" to "old", "newStr" to "new")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.replaceContent("test.txt", "old", "new") }
    }

    @Test
    fun `fileExists calls fs fileExists with correct args`() = runBlocking {
        coEvery { fs.fileExists(any()) } returns "exists"
        
        val tc = ToolCall(
            domain = "fs",
            method = "fileExists",
            arguments = mutableMapOf("filename" to "test.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.fileExists("test.txt") }
    }

    @Test
    fun `getFileInfo calls fs getFileInfo with correct args`() = runBlocking {
        coEvery { fs.getFileInfo(any()) } returns "info"
        
        val tc = ToolCall(
            domain = "fs",
            method = "getFileInfo",
            arguments = mutableMapOf("filename" to "test.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.getFileInfo("test.txt") }
    }

    @Test
    fun `deleteFile calls fs deleteFile with correct args`() = runBlocking {
        coEvery { fs.deleteFile(any()) } returns "deleted"
        
        val tc = ToolCall(
            domain = "fs",
            method = "deleteFile",
            arguments = mutableMapOf("filename" to "test.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.deleteFile("test.txt") }
    }

    @Test
    fun `copyFile calls fs copyFile with correct args`() = runBlocking {
        coEvery { fs.copyFile(any(), any()) } returns "copied"
        
        val tc = ToolCall(
            domain = "fs",
            method = "copyFile",
            arguments = mutableMapOf("source" to "src.txt", "dest" to "dst.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.copyFile("src.txt", "dst.txt") }
    }

    @Test
    fun `moveFile calls fs moveFile with correct args`() = runBlocking {
        coEvery { fs.moveFile(any(), any()) } returns "moved"
        
        val tc = ToolCall(
            domain = "fs",
            method = "moveFile",
            arguments = mutableMapOf("source" to "old.txt", "dest" to "new.txt")
        )
        
        executor.execute(tc, fs)
        coVerify { fs.moveFile("old.txt", "new.txt") }
    }

    @Test
    fun `listFiles calls fs listFilesInfo`() = runBlocking {
        coEvery { fs.listFilesInfo() } returns "files list"
        
        val tc = ToolCall(
            domain = "fs",
            method = "listFiles",
            arguments = mutableMapOf()
        )
        
        executor.execute(tc, fs)
        coVerify { fs.listFilesInfo() }
    }

    @Test
    fun `unsupported method returns exception`() = runBlocking {
        val tc = ToolCall(
            domain = "fs",
            method = "unsupportedMethod",
            arguments = mutableMapOf()
        )
        
        val result = executor.execute(tc, fs)
        assertNotNull(result.exception)
        assertTrue(result.exception?.cause?.message?.contains("Unsupported") == true)
    }

    @Test
    fun `missing required parameter returns exception`() = runBlocking {
        val tc = ToolCall(
            domain = "fs",
            method = "append",
            arguments = mutableMapOf("filename" to "test.txt")
            // missing "content" parameter
        )
        
        val result = executor.execute(tc, fs)
        assertNotNull(result.exception)
        assertTrue(result.exception?.cause?.message?.contains("content") == true)
    }

    @Test
    fun `wrong domain returns exception`() = runBlocking {
        val tc = ToolCall(
            domain = "wrong",
            method = "writeString",
            arguments = mutableMapOf("filename" to "test.txt", "content" to "test")
        )
        
        val result = executor.execute(tc, fs)
        assertNotNull(result.exception)
    }
}
