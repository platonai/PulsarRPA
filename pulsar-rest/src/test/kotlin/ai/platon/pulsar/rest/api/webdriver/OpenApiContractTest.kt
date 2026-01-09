package ai.platon.pulsar.rest.api.webdriver

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.yaml.snakeyaml.Yaml
import java.nio.charset.StandardCharsets
import org.springframework.web.servlet.mvc.method.RequestMappingInfo

/**
 * Light-weight contract checks to prevent OpenAPI spec drifting away from Spring MVC mappings.
 *
 * Contract:
 * - Source of truth: `classpath:static/openapi.yaml` (served by `OpenApiController`).
 * - Every `(method, path)` documented under `paths:` must exist in Spring MVC handler mappings.
 */
@SpringBootTest
class OpenApiContractTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `openapi yaml should be parseable and contain paths`() {
        val spec = loadOpenApiYaml()
        val paths = (spec["paths"] as? Map<*, *>)
        assertTrue(!paths.isNullOrEmpty(), "openapi.yaml must define non-empty paths")
    }

    @Test
    fun `documented openapi paths should be implemented by spring mvc mappings`() {
        val spec = loadOpenApiYaml()
        val documented = extractDocumentedOperations(spec)
        val implemented = extractImplementedOperations()

        val missing = documented.filterNot { it in implemented }
        assertTrue(
            missing.isEmpty(),
            "OpenAPI documented operations missing in MVC mappings (count=${missing.size}):\n" +
                missing.sorted().joinToString("\n")
        )
    }

    private fun loadOpenApiYaml(): Map<String, Any?> {
        val resource = applicationContext.getResource("classpath:static/openapi.yaml")
        require(resource.exists()) { "classpath:static/openapi.yaml not found" }

        val text = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val yaml = Yaml()
        @Suppress("UNCHECKED_CAST")
        return yaml.load(text) as Map<String, Any?>
    }

    /**
     * Returns `METHOD /path` pairs from OpenAPI. Example: `POST /session/{sessionId}/url`.
     */
    private fun extractDocumentedOperations(spec: Map<String, Any?>): Set<String> {
        val paths = spec["paths"] as? Map<*, *> ?: return emptySet()
        val ops = linkedSetOf<String>()

        for ((rawPath, rawPathItem) in paths) {
            val path = rawPath?.toString() ?: continue
            val pathItem = rawPathItem as? Map<*, *> ?: continue

            for ((rawMethod, rawOperation) in pathItem) {
                val method = rawMethod?.toString()?.uppercase() ?: continue
                // Skip non-HTTP keys if any (OpenAPI 3.1 pathItem allows $ref/summary/description etc.)
                if (method !in setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE")) {
                    continue
                }
                if (rawOperation !is Map<*, *>) continue

                ops += "$method $path"
            }
        }

        return ops
    }

    /**
     * Returns `METHOD /path` pairs from Spring MVC. Example: `POST /session/{sessionId}/url`.
     */
    private fun extractImplementedOperations(): Set<String> {
        val mapping = applicationContext.getBean(RequestMappingHandlerMapping::class.java)
        val ops = linkedSetOf<String>()

        for ((info, _) in mapping.handlerMethods) {
            val methods = info.methodsCondition.methods
            val patterns = extractPatterns(info)

            // Some mappings might not constrain HTTP method; treat them as implemented for all documented ones.
            if (methods.isEmpty()) {
                for (p in patterns) {
                    ops += "* ${normalizePathTemplate(p)}"
                }
                continue
            }

            for (m in methods) {
                for (p in patterns) {
                    ops += "${m.name} ${normalizePathTemplate(p)}"
                }
            }
        }

        return ops
    }

    /**
     * Extract patterns from Spring's RequestMappingInfo, supporting both:
     * - Spring MVC PathPattern (Boot 2.6+/3.x): `pathPatternsCondition`
     * - Legacy AntPathMatcher: `patternsCondition`
     */
    private fun extractPatterns(info: RequestMappingInfo): Set<String> {
        val patterns = linkedSetOf<String>()

        // Spring 6 / Boot 3: PathPatternsCondition
        val pathPatternsCondition = info.pathPatternsCondition
        if (pathPatternsCondition != null) {
            for (p in pathPatternsCondition.patterns) {
                patterns += p.patternString
            }
        }

        // Spring 5 / Boot 2: PatternsRequestCondition
        val legacy = info.patternsCondition
        if (legacy != null) {
            patterns += legacy.patterns
        }

        return patterns
    }

    /**
     * Normalize path templates so comparisons are stable between OpenAPI and Spring.
     *
     * Today both sides use `{var}` placeholders, but we normalize anyway to avoid subtle differences.
     */
    private fun normalizePathTemplate(path: String): String {
        // Remove extra trailing slashes for consistency, but keep root '/'.
        val trimmed = if (path.length > 1) path.trimEnd('/') else path
        return trimmed
    }
}
