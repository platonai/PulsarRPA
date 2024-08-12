
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.skeleton.crawl.filter.SCOPE_DEFAULT
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.io.FileReader
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.*

@SpringJUnitConfig
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestRegexUrlNormalizer {
    @Autowired
    private lateinit var normalizer: RegexUrlNormalizer
    private val testData = HashMap<String, List<NormalizedURL>>()
    
    @BeforeTest
    @Throws(IOException::class, URISyntaxException::class)
    fun setUp() {
        val sampleDir = ResourceLoader.getPath("normregex/sample")
        val configFiles = sampleDir.listDirectoryEntries("regex-normalize-*.xml")
        for (file in configFiles) {
            try {
                val reader = FileReader(file.toFile())
                var name = file.name
                name = name.substring(16, name.indexOf(".xml"))
                normalizer.setConfiguration(reader, name)
                val urls = readTestFile(name)
                testData[name] = urls
            } catch (e: Exception) {
                LOG.warn("Could load config from '$file': $e")
            }
        }
    }
    
    @Test
    @Throws(Exception::class)
    fun testNormalizerDefault() {
        normalizeTest(testData[SCOPE_DEFAULT]!!, SCOPE_DEFAULT)
    }
    
    @Test
    @Throws(Exception::class)
    fun testNormalizerScope() {
        for (scope in testData.keys) {
            normalizeTest(testData[scope]!!, scope)
        }
    }
    
    private fun normalizeTest(urls: List<NormalizedURL>, scope: String) {
        for (url1 in urls) {
            val url = url1.url
            val normalized = normalizer.normalize(url1.url, scope)
            val expected = url1.expectedURL
            LOG.info("scope: $scope url: $url | normalized: $normalized | expected: $expected")
            assertEquals(url1.expectedURL, normalized)
        }
    }
    
    @Throws(IOException::class, URISyntaxException::class)
    private fun readTestFile(scope: String): List<NormalizedURL> {
        val SAMPLE_DIR = ResourceLoader.getPathOrNull("normregex/sample")
        val testFile = Paths.get(SAMPLE_DIR.toString(), "regex-normalize-$scope.test")
        return Files.readAllLines(testFile).stream().map { obj: String -> obj.trim { it <= ' ' } }
            .filter { l: String -> l.isNotEmpty() }.filter { l: String -> !l.startsWith("#") }
            .map { line: String -> NormalizedURL(line) }.collect(Collectors.toList())
    }
    
    private class NormalizedURL(line: String) {
        var url: String
        var expectedURL: String
        
        init {
            val fields = line.split("\\s+".toRegex()).toTypedArray()
            url = fields[0]
            expectedURL = fields[1]
        }
    }
    
    companion object {
        private val LOG = LoggerFactory.getLogger(TestRegexUrlNormalizer::class.java)
    }
}
