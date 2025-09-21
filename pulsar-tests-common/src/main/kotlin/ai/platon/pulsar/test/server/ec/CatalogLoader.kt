package ai.platon.pulsar.test.server.ec

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.charset.StandardCharsets

@Configuration
class CatalogLoader {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    @Bean
    fun catalogIndex(): CatalogIndex {
        val path = "/static/generated/mock-amazon/data/products.json"
        val resource = javaClass.getResource(path)
            ?: error("Catalog JSON not found at $path")
        val json = resource.readText(StandardCharsets.UTF_8)
        val catalog: Catalog = objectMapper.readValue(json)
        require(catalog.categories.size == 20) { "Exactly 20 categories required" }
        val countsPerCategory = catalog.products.groupBy { it.categoryId }.mapValues { it.value.size }
        countsPerCategory.values.forEach { require(it in 5..12) { "Each category must have 5..12 products" } }
        require(catalog.products.size >= 100) { "Total products must be >= 100" }
        val index = CatalogIndex(catalog)
        log.info("Loaded mock EC catalog categories={}, products={}, seed={}", catalog.categories.size, catalog.products.size, catalog.meta.seed)
        return index
    }
}

