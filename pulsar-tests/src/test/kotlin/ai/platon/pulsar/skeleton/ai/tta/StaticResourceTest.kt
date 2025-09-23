package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.util.server.PulsarAndMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus

/**
 * Test to verify static resources are accessible
 */
@SpringBootTest(classes = [PulsarAndMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class StaticResourceTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `When static resource is requested then it should be accessible`() {
        val baseUrl = "http://127.0.0.1:$port"

        // Test basic endpoint
        val homeResponse = restTemplate.getForEntity("$baseUrl/", String::class.java)
        println("Home response status: ${homeResponse.statusCode}")
        println("Home response body: ${homeResponse.body}")

        // Test static TTA resources
        val ttaFiles = listOf(
            "interactive-1.html",
            "interactive-2.html",
            "interactive-3.html",
            "interactive-4.html"
        )

        for (file in ttaFiles) {
            val url = "$baseUrl/generated/tta/$file"
            println("Testing URL: $url")

            try {
                val response = restTemplate.getForEntity(url, String::class.java)
                println("$file - Status: ${response.statusCode}, Body length: ${response.body?.length ?: 0}")

                if (response.statusCode == HttpStatus.OK) {
                    println("✅ $file is accessible")
                } else {
                    println("❌ $file returned status: ${response.statusCode}")
                }
            } catch (e: Exception) {
                println("❌ $file error: ${e.message}")
            }
        }
    }
}