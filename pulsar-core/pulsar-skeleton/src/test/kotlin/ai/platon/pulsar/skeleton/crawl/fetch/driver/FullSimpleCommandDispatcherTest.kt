import ai.platon.pulsar.skeleton.crawl.fetch.driver.ToolCallExecutor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

@Ignore("Full simple command dispatcher not implemented yet")
class FullSimpleCommandDispatcherTest {

    @Test
    fun testParseSimpleFunctionCall_validInput() {
        val input = "driver.open(\"https://t.tt\")"
        val result = ToolCallExecutor.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://t.tt"), result?.third)
    }


}
