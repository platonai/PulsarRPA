package ai.platon.pulsar.common

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class UrlExtractorTests {

    @TempDir
    lateinit var tempDir: Path
    lateinit var tempFile: Path

    @BeforeEach
    fun setUp() {
        tempFile = Files.createTempFile(tempDir, "testFile", ".txt")
    }

    @AfterEach
    fun tearDown() {
        if (Files.exists(tempFile)) {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `extract should return first URL from line`() {
        val urlExtractor = UrlExtractor()
        val line = "Check out this link: http://example.com and this one: https://another-example.com"
        val result = urlExtractor.extract(line)
        kotlin.test.assertEquals("http://example.com", result)
    }

    @Test
    fun `extract should return null if no URL found in line`() {
        val urlExtractor = UrlExtractor()
        val line = "No URLs here"
        val result = urlExtractor.extract(line)
        kotlin.test.assertEquals(null, result)
    }

    @Test
    fun `extractTo should add all URLs from line to set`() {
        val urlExtractor = UrlExtractor()
        val line = "Check out these links: http://example.com and https://another-example.com"
        val urls = mutableSetOf<String>()
        urlExtractor.extractTo(line, urls)
        kotlin.test.assertEquals(setOf("http://example.com", "https://another-example.com"), urls)
    }

    @Test
    fun `extractTo should not add anything to set if no URL found in line`() {
        val urlExtractor = UrlExtractor()
        val line = "No URLs here"
        val urls = mutableSetOf<String>()
        urlExtractor.extractTo(line, urls)
        kotlin.test.assertTrue(urls.isEmpty())
    }

    @Test
    fun `extract from file with URLs should extract URLs`() {
        Files.write(tempFile, "http://example.com\nhttps://another-example.com".toByteArray())
        val urls = LinkExtractors.fromFile(tempFile)
        kotlin.test.assertEquals(setOf("http://example.com", "https://another-example.com"), urls)
    }

    @Test
    fun `extract from non-existent file should return empty set`() {
        val nonExistentFile = Paths.get("non_existent_file.txt")
        val urls = LinkExtractors.fromFile(nonExistentFile)
        kotlin.test.assertTrue(urls.isEmpty())
    }

    @Test
    fun `extract from directory with files containing URLs should extract URLs`() {
        val file1 = Files.createTempFile(tempDir, "file1", ".txt")
        val file2 = Files.createTempFile(tempDir, "file2", ".txt")
        Files.write(file1, "http://example.com\n".toByteArray())
        Files.write(file2, "https://another-example.com\n".toByteArray())

        val urls = LinkExtractors.fromDirectory(tempDir)
        kotlin.test.assertEquals(setOf("http://example.com", "https://another-example.com"), urls)
    }

    @Test
    fun `extract from non-existent directory should return empty set`() {
        val nonExistentDir = Paths.get("non_existent_dir")
        val urls = LinkExtractors.fromDirectory(nonExistentDir)
        kotlin.test.assertTrue(urls.isEmpty())
    }

    @Test
    fun `extract and filter then return filtered URLs`() {
        val urlExtractor = UrlExtractor()
        val line = "Check out these links: http://example.com and https://amazon.com"
        val filteredUrls = urlExtractor.extractAll(line) { it.contains("example") }
        kotlin.test.assertEquals(setOf("http://example.com"), filteredUrls)
    }
}