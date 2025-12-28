package ai.platon.pulsar.agentic.tools

import ai.platon.pulsar.agentic.ToolCallSpec
import ai.platon.pulsar.agentic.tools.executors.AbstractToolExecutor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ToolCallSpecificationRendererTest {

    private lateinit var registry: CustomToolRegistry

    @BeforeEach
    fun setUp() {
        registry = CustomToolRegistry.instance
        registry.clear()
    }

    @AfterEach
    fun tearDown() {
        registry.clear()
    }

    @Test
    fun `render should keep ToolSpecification verbatim and append custom tools`() {
        val executor = DbToolExecutor()
        val specs = listOf(
            ToolCallSpec(
                domain = "db",
                method = "query",
                arguments = listOf(ToolCallSpec.Arg("sql", "String")),
                returnType = "String",
                description = "Run a SQL query"
            )
        )

        registry.register(executor, specs)

        val rendered = ToolCallSpecificationRenderer.render(includeCustomDomains = true)

        // Built-in specification should be present verbatim
        assertTrue(rendered.contains("// domain: driver"), rendered)
        assertTrue(rendered.contains("driver.reload()"), rendered)

        // Custom section appended
        assertTrue(rendered.contains("// CustomTool"), rendered)
        assertTrue(rendered.contains("db.query("), rendered)
        assertTrue(rendered.contains("sql:"), rendered)
    }

    private class DbToolExecutor : AbstractToolExecutor() {
        override val domain: String = "db"
        override val targetClass: KClass<*> = Any::class

        override suspend fun execute(
            objectName: String,
            functionName: String,
            args: Map<String, Any?>,
            target: Any
        ): Any? {
            return null
        }
    }
}
