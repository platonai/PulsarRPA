package ai.platon.pulsar.protocol.browser.driver.playwright

import ai.platon.pulsar.common.Runtimes
import com.microsoft.playwright.Playwright
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue

open class PlaywrightTestBase {

    companion object {
        lateinit var playwright: Playwright

        @JvmStatic
        @BeforeAll
        fun installPlaywright() {
            try {
                // 尝试安装 Playwright
                // Runtimes.exec("npx playwright install")
                playwright = Playwright.create()
                Assumptions.assumeTrue(true) // 如果安装成功，继续执行测试
            } catch (e: Exception) {
                Assumptions.assumeTrue(false) // 如果安装失败，跳过所有测试
            }
        }

        @JvmStatic
        @AfterAll
        fun cleanUp() {
            playwright.close()
        }
    }

    @BeforeEach
    fun checkNodeJSAvailability() {
        var output = Runtimes.exec("node --version")
        // a typical version is v16.15.1
        println(output.joinToString())
        Assumptions.assumeTrue({ output.size == 1 }, output.joinToString())
        Assumptions.assumeTrue({ output[0].startsWith("v") }, output.joinToString())

        output = Runtimes.exec("""node -e "console.log(1 + 1);" """)
        println(output.joinToString())
        // not all node version supports the expression
        // Assumptions.assumeTrue { output[0] == "2" }
    }

    @BeforeEach
    fun checkPlaywrightAvailability() {
        val output = Runtimes.exec("playwright --version")
        println(output.joinToString())
        Assumptions.assumeTrue { output.isNotEmpty() }
    }

    @Test
    fun testSomethingToEnsurePrecondition() {
        assertTrue { "You are a great creator".isNotEmpty() }
    }
}
