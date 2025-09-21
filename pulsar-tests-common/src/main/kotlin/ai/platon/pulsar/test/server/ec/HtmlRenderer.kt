package ai.platon.pulsar.test.server.ec

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class HtmlRenderer(private val catalogService: CatalogService) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val homeTemplate = loadTemplate("/static/generated/mock-amazon/ec-home.html")
    private val categoryTemplate = loadTemplate("/static/generated/mock-amazon/ec-category.html")
    private val productTemplate = loadTemplate("/static/generated/mock-amazon/ec-product.html")

    private fun loadTemplate(path: String): String {
        val res = javaClass.getResource(path) ?: error("Template not found: $path")
        return res.readText(StandardCharsets.UTF_8)
    }

    private fun esc(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }

    fun renderHome(): String {
        val links = catalogService.allCategories().joinToString("\n") { c ->
            """
            <li class=\"category-item\" data-category-id=\"${c.id}\">\n  <a id=\"cat-link-${c.id}\" href=\"/ec/b?node=${c.id}\">${esc(c.name)}</a>\n</li>
            """.trimIndent()
        }
        return homeTemplate
            .replace("<!--CATEGORY_LINKS-->", links)
            .replace("{{TITLE}}", "Mock EC Home")
    }

    private fun productBadges(p: Product) = if (p.badges.isEmpty()) "" else p.badges.joinToString("", prefix = "<div class=\"product-badges\">", postfix = "</div>") { "<span class=\"badge\">${esc(it)}</span>" }

    private fun productCard(p: Product): String = """
        <article class=\"product-card\" id=\"product-${p.id}\" data-category-id=\"${p.categoryId}\">\n  <a class=\"product-link\" href=\"/ec/dp/${p.id}\">\n    <img class=\"product-image\" src=\"${esc(p.image)}\" alt=\"${esc(p.name)}\" />\n    <h2 class=\"product-title\">${esc(p.name)}</h2>\n  </a>\n  <div class=\"product-meta\">\n    <span class=\"product-price\" id=\"product-price-${p.id}\" data-product-id=\"${p.id}\">${formatPrice(p)}</span>\n    <span class=\"product-rating\" id=\"product-rating-${p.id}\" data-rating=\"${p.rating}\">${String.format("%.1f", p.rating)} (${p.ratingCount})</span>\n  </div>\n  ${productBadges(p)}\n</article>
    """.trimIndent()

    private fun formatPrice(p: Product): String = "${'$'}" + String.format("%.2f", p.price)

    fun renderCategory(category: Category, products: List<Product>): String {
        val cards = products.joinToString("\n") { productCard(it) }
        return categoryTemplate
            .replace("{{CATEGORY_ID}}", esc(category.id))
            .replace("{{CATEGORY_NAME}}", esc(category.name))
            .replace("<!--PRODUCT_LIST-->", cards)
            .replace("{{TITLE}}", "Category: ${esc(category.name)}")
    }

    fun renderProduct(product: Product, category: Category): String {
        val features = if (product.features.isEmpty()) "" else product.features.joinToString("\n") { "<li>${esc(it)}</li>" }
        val featureBlock = if (features.isEmpty()) "" else "<ul id=\"product-features\">$features</ul>"
        val specsRows = if (product.specs.isEmpty()) "" else product.specs.entries.joinToString("\n") { "<tr><th>${esc(it.key)}</th><td>${esc(it.value)}</td></tr>" }
        val specsTable = if (specsRows.isEmpty()) "" else "<table id=\"product-specs\">$specsRows</table>"
        val badges = productBadges(product)
        return productTemplate
            .replace("{{PRODUCT_ID}}", esc(product.id))
            .replace("{{PRODUCT_NAME}}", esc(product.name))
            .replace("{{PRODUCT_PRICE}}", formatPrice(product))
            .replace("{{PRODUCT_RATING}}", String.format("%.1f", product.rating))
            .replace("{{PRODUCT_RATING_COUNT}}", product.ratingCount.toString())
            .replace("{{PRODUCT_IMAGE}}", esc(product.image))
            .replace("{{PRODUCT_CATEGORY_ID}}", esc(category.id))
            .replace("{{PRODUCT_CATEGORY_NAME}}", esc(category.name))
            .replace("<!--FEATURE_LIST-->", featureBlock)
            .replace("<!--SPECS_TABLE-->", specsTable)
            .replace("<!--BADGES-->", badges)
            .replace("{{TITLE}}", "Product: ${esc(product.name)}")
    }

    fun renderError(status: Int, message: String): String {
        return """
            <!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Error $status</title></head><body>
            <div id=\"error-page\" class=\"error-code-$status\"><h1>Error $status</h1><p>${esc(message)}</p></div>
            </body></html>
        """.trimIndent()
    }
}

