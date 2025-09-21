package ai.platon.pulsar.test.server.ec

import org.springframework.stereotype.Service

@Service
class CatalogService(private val catalogIndex: CatalogIndex) {
    fun allCategories(): List<Category> = catalogIndex.catalog.categories
    fun getCategory(id: String): Category? = catalogIndex.categoryMap[id]
    fun getProductsByCategory(id: String): List<Product> = catalogIndex.productsByCategory[id].orEmpty().sortedBy { it.id }
    fun getProduct(id: String): Product? = catalogIndex.productMap[id]
}

