package ai.platon.pulsar.test.server.ec

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

/** Meta information for the catalog */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CatalogMeta(
    val version: Int = 1,
    val generatedAt: String = Instant.now().toString(),
    val seed: Long = 0L,
)

/** Category */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Category(
    val id: String,
    val name: String,
    val slug: String,
)

/** Inventory info */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Inventory(
    val inStock: Boolean = true,
    val qty: Int = 0,
)

/** Product */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Product(
    val id: String,
    val name: String,
    val categoryId: String,
    val price: Double,
    val currency: String = "USD",
    val image: String = "/ec/static/img/placeholder.png",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val badges: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val description: String = "",
    val specs: Map<String, String> = emptyMap(),
    val inventory: Inventory = Inventory(true, 0),
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString(),
)

/** Root catalog object */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Catalog(
    val meta: CatalogMeta = CatalogMeta(),
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
)

/** In-memory immutable indexes */
class CatalogIndex(val catalog: Catalog) {
    val categoryMap: Map<String, Category> = catalog.categories.associateBy { it.id }
    val productMap: Map<String, Product> = catalog.products.associateBy { it.id }
    val productsByCategory: Map<String, List<Product>> = catalog.products.groupBy { it.categoryId }
}
