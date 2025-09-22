package ai.platon.pulsar.test.server.ec

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EcommerceControllerTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var rest: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    private fun assertHtml(resp: ResponseEntity<String>, expected: HttpStatus) {
        assertThat(resp.statusCode).isEqualTo(expected)
        assertThat(resp.headers.contentType?.includes(MediaType.TEXT_HTML)).isTrue()
        assertThat(resp.body).isNotBlank()
    }

    @Test
    @DisplayName("GET /ec/ home page lists categories")
    fun home() {
        val resp = rest.getForEntity(url("/ec/"), String::class.java)
        assertHtml(resp, HttpStatus.OK)
        assertThat(resp.body).contains("id=\"category-list\"")
        // check one known category id present
        assertThat(resp.body).contains("cat-link-1292115012")
    }

    @Test
    fun homeWithoutTrailingSlash() {
        val resp = rest.getForEntity(url("/ec"), String::class.java)
        assertHtml(resp, HttpStatus.OK)
        assertThat(resp.body).contains("id=\"category-list\"")
    }

    @Test
    fun categoryPage() {
        val resp = rest.getForEntity(url("/ec/b?node=1292115012"), String::class.java)
        assertHtml(resp, HttpStatus.OK)
        assertThat(resp.body).contains("data-category-id=\"1292115012\"")
        assertThat(resp.body).contains("product-")
    }

    @Test
    fun productPage() {
        val resp = rest.getForEntity(url("/ec/dp/B0E000001"), String::class.java)
        assertHtml(resp, HttpStatus.OK)
        assertThat(resp.body).contains("data-product-id=\"B0E000001\"")
        assertThat(resp.body).contains("product-price")
    }


    @Test
    fun missingCategoryParam() {
        val resp = rest.getForEntity(url("/ec/b"), String::class.java)
        assertHtml(resp, HttpStatus.BAD_REQUEST)
        assertThat(resp.body).contains("Error 400")
    }

    @Test
    fun invalidCategory() {
        val resp = rest.getForEntity(url("/ec/b?node=DOES_NOT_EXIST"), String::class.java)
        assertHtml(resp, HttpStatus.NOT_FOUND)
        assertThat(resp.body).contains("Error 404")
    }

    @Test
    fun invalidProduct() {
        val resp = rest.getForEntity(url("/ec/dp/DOES_NOT_EXIST"), String::class.java)
        assertHtml(resp, HttpStatus.NOT_FOUND)
        assertThat(resp.body).contains("Error 404")
    }
}
