
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.common.ResourceLoader.getResource
import ai.platon.pulsar.skeleton.crawl.filter.SCOPE_DEFAULT
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.test.*

@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestRegexUrlNormalizer {
    @Autowired
    private val normalizer: RegexUrlNormalizer? = null
    private val testData = HashMap<String, List<NormalizedURL>>()
    
    @BeforeTest
    @Throws(IOException::class, URISyntaxException::class)
    fun setUp() {
        val SAMPLE_DIR = Paths.get(getResource("normregex/sample")!!.toURI())
        val configs = SAMPLE_DIR.toFile()
            .listFiles { f: File -> f.name.endsWith(".xml") && f.name.startsWith("regex-normalize-") }!!
        for (config in configs) {
            try {
                val reader = FileReader(config)
                var cname = config.name
                cname = cname.substring(16, cname.indexOf(".xml"))
                normalizer!!.setConfiguration(reader, cname)
                val urls = readTestFile(cname)
                testData[cname] = urls
            } catch (e: Exception) {
                LOG.warn("Could load config from '$config': $e")
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
            val normalized = normalizer!!.normalize(url1.url, scope)
            val expected = url1.expectedURL
            LOG.info("scope: $scope url: $url | normalized: $normalized | expected: $expected")
            assertEquals(url1.expectedURL, normalized)
        }
    }
    
    @Throws(IOException::class, URISyntaxException::class)
    private fun readTestFile(scope: String): List<NormalizedURL> {
        val SAMPLE_DIR = Paths.get(getResource("normregex/sample")!!.toURI())
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
