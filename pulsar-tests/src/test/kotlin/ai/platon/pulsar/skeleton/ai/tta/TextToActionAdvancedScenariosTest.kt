package ai.platon.pulsar.skeleton.ai.tta

import ai.platon.pulsar.agentic.ai.tta.ActionDescription
import ai.platon.pulsar.common.printlnPro
import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.util.server.EnableMockServerApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Advanced scenario tests for TextToAction.generateWebDriverAction() method
 * Testing complex scenarios, error conditions, and edge cases
 */
@Tag("ExternalServiceTest")
@Tag("TimeConsumingTest")
@SpringBootTest(
    classes = [EnableMockServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class TextToActionAdvancedScenariosTest : TextToActionTestBase() {

    @BeforeEach
    fun setUp() {
        // Setup is handled by parent class
    }

    // ======== MULTI-STEP COMPLEX COMMANDS ========

    @Test
    fun `When given multi-step workflow commands then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo(ttaUrl2)
        driver.waitForSelector("body")

        val complexCommands = listOf(
            "填写姓名为张三，选择语言为Python，然后点击显示摘要",
            "输入名字John，勾选订阅，调整字体大小到20",
            "先选择Kotlin语言，再输入姓名李四，最后显示摘要",
            "fill name with Alice, select JavaScript, show summary"
        )

        complexCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertTrue(
                actionDescription.cssFriendlyExpressions.size <= 1,
                "Should generate at most one action for complex: $command"
            )

            printlnPro("Complex workflow command: $command")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")
            printlnPro("Model response: ${actionDescription.modelResponse?.content}")

            // Should handle complex instructions in some way

            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    @Test
    fun `When given conditional commands then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val conditionalCommands = listOf(
            "如果存在添加按钮就点击它",
            "如果有姓名输入框就输入张三",
            "当页面加载完成后选择颜色",
            "if the toggle button exists then click it"
        )

        conditionalCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertTrue(
                actionDescription.cssFriendlyExpressions.size <= 1,
                "Should generate at most one action for conditional: $command"
            )

            printlnPro("Conditional command: $command")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")

            // Should handle conditional logic

            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    // ======== CONTEXT-AWARE COMMANDS ========

    @Test
    fun `When given context-specific commands then use appropriate context`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val contextCommands = listOf(
            "在用户区域点击保存" to "user context",
            "在产品部分点击编辑" to "product context",
            "在账单地址填写姓名" to "billing context",
            "在右上角点击菜单" to "position context",
            "click save in user management section" to "user context"
        )

        contextCommands.forEach { (command, expectedContext) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.cssFriendlyExpressions.size,
                "Should generate exactly one action for valid command: $command"
            )

            printlnPro("Context-aware command: $command (expected: $expectedContext)")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")

            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    @Test
    fun `When given spatial reference commands then use position context`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val spatialCommands = listOf(
            "点击左上角的按钮" to "top-left",
            "选择右下角的元素" to "bottom-right",
            "点击顶部的菜单" to "top",
            "选择底部的保存按钮" to "bottom",
            "click the button in the top right" to "top-right"
        )

        spatialCommands.forEach { (command, expectedPosition) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertEquals(
                1,
                actionDescription.cssFriendlyExpressions.size,
                "Should generate exactly one action for valid command: $command"
            )

            printlnPro("Spatial command: $command (expected: $expectedPosition)")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")

            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    // ======== ERROR RECOVERY TESTS ========

    @Test
    fun `When given malformed commands then handle gracefully`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val malformedCommands = listOf(
            "点击   多个   空格   按钮",
            "输入@#$%^&*()特殊字符",
            "点击按钮点击按钮点击按钮",
            "",
            "   ",
            "点击",
            "输入",
            "选择"
        )

        malformedCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertTrue(
                actionDescription.cssFriendlyExpressions.size <= 1,
                "Should handle malformed command: '$command'"
            )

            printlnPro("Malformed command: '$command'")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")
            printlnPro("Model response state: ${actionDescription.modelResponse?.state}")

            // Should not crash and should return some response
            assertNotNull(actionDescription.modelResponse, "Should have model response")
        }
    }

    @Test
    fun `When given contradictory commands then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val contradictoryCommands = listOf(
            "同时勾选和取消勾选订阅",
            "输入姓名但是又删除它",
            "选择Python但是不选择任何语言",
            "click and unclick the checkbox"
        )

        contradictoryCommands.forEach { command ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertTrue(
                actionDescription.cssFriendlyExpressions.size <= 1,
                "Should handle contradictory command: $command"
            )

            printlnPro("Contradictory command: $command")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")

            // Should handle contradictions in some way

            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    // ======== EDGE CASE TESTS ========

    @Test
    fun `When given extremely long commands then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val veryLongCommand = """
            请帮我在这个页面的用户信息区域首先输入我的完整姓名叫做'亚历山大·史密斯·约翰逊·威廉姆斯·布朗·琼斯·加西亚·米勒·戴维斯·罗德里格斯·马丁内斯·赫尔南德斯·洛佩兹·冈萨雷斯·威尔逊·安德森·托马斯·泰勒·摩尔·杰克逊'
            然后选择我最喜欢的编程语言是Python因为Python是一种非常强大而且易于学习的编程语言它有很多优秀的库和框架
            接着我要勾选订阅通讯复选框因为我想要接收最新的技术资讯和更新
            之后我需要调整文本大小滑块到24像素这样我可以更清楚地看到页面内容
            最后我要点击显示摘要按钮来查看我所有输入的信息是否正确显示出来
            另外如果页面加载比较慢的话请等待所有元素都加载完成再执行这些操作谢谢
        """.trimIndent()

        val actionDescription = textToAction.generate(veryLongCommand, driver)

        assertNotNull(actionDescription)
        assertTrue(actionDescription.cssFriendlyExpressions.size <= 1, "Should handle very long command")

        printlnPro("Very long command length: ${veryLongCommand.length}")
        printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")
        printlnPro("Model response length: ${actionDescription.modelResponse?.content?.length}")

        // Should handle long commands without crashing
        assertNotNull(actionDescription.modelResponse, "Should have model response")
    }

    @Test
    fun `When given commands with special Unicode characters then handle appropriately`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo(ttaUrl1)
            driver.waitForSelector("body")

            val unicodeCommands = listOf(
                "输入表情符号🙂😊😎",
                "输入数学符号∑∏∫∆",
                "输入货币符号$€£¥",
                "输入箭头符号←→↑↓",
                "type emoji 😀 in name field"
            )

            unicodeCommands.forEach { command ->
                val actionDescription = textToAction.generate(command, driver)

                assertNotNull(actionDescription)
                assertTrue(
                    actionDescription.cssFriendlyExpressions.size <= 1,
                    "Should handle Unicode command: $command"
                )

                printlnPro("Unicode command: $command")
                printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")

                // Should handle Unicode characters

                val expression = actionDescription.cssFriendlyExpressions.first()
                assertTrue(expression.isNotBlank(), "Should generate non-empty action")
            }
        }

    @Test
    fun `When given commands in different languages then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-2.html")
        driver.waitForSelector("body")

        val multilingualCommands = listOf(
            "输入姓名如'José García'" to "Spanish",
            "输入名字'François Müller'" to "French/German",
            "输入'Александр Иванов'" to "Russian",
            "输入'山田太郎'" to "Japanese",
            "输入'김민수'" to "Korean",
            "type name 'Nguyễn Văn A'" to "Vietnamese"
        )

        multilingualCommands.forEach { (command, language) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertTrue(actionDescription.cssFriendlyExpressions.size <= 1, "Should handle $language command: $command")

            printlnPro("$language command: $command")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")
            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    // ======== PERFORMANCE AND STRESS TESTS ========

    @Test
    fun `When given rapid sequential commands then maintain consistency`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val baseCommand = "点击添加按钮"
        val results = mutableListOf<ActionDescription>()

        // Execute the same command multiple times rapidly
        repeat(5) {
            val actionDescription = textToAction.generate(baseCommand, driver)
            results.add(actionDescription)
        }

        results.forEachIndexed { index, result ->
            printlnPro("Rapid execution ${index + 1}: ${result.cssFriendlyExpressions}")
            assertNotNull(result, "Should have result for execution ${index + 1}")
            assertTrue(result.cssFriendlyExpressions.size <= 1, "Should generate at most one action")
        }

        // Results should be consistent
        val firstResult = results.first()
        results.forEach { result ->
            assertEquals(
                firstResult.cssFriendlyExpressions.size, result.cssFriendlyExpressions.size,
                "Should be consistent in number of actions"
            )
        }
    }

    @Test
    fun `When given commands with timing requirements then handle appropriately`() =
        runEnhancedWebDriverTest { driver ->
            driver.navigateTo("$ttaBaseURL/interactive-2.html")
            driver.waitForSelector("body")

            val timingCommands = listOf(
                "等待2秒后输入姓名张三",
                "选择语言后等待1秒再显示摘要",
                "先等待页面完全加载再勾选订阅",
                "wait 3 seconds then fill name with John"
            )

            timingCommands.forEach { command ->
                val actionDescription = textToAction.generate(command, driver)

                assertNotNull(actionDescription)
                assertTrue(actionDescription.cssFriendlyExpressions.size <= 1, "Should handle timing command: $command")

                printlnPro("Timing command: $command")
                printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")
                val expression = actionDescription.cssFriendlyExpressions.first()
                assertTrue(expression.isNotBlank(), "Should generate non-empty action")
            }
        }

    // ======== VALIDATION AND ASSERTION TESTS ========

    @Test
    fun `When given validation-related commands then handle appropriately`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo("$ttaBaseURL/interactive-ambiguity.html")
        driver.waitForSelector("body")

        val validationCommands = listOf(
            "验证保存按钮是否存在" to "validation",
            "检查用户区域的编辑按钮是否可见" to "visibility-check",
            "确认账单地址表单已加载" to "form-check",
            "verify that the save button is clickable" to "clickability-check"
        )

        validationCommands.forEach { (command, expectedType) ->
            val actionDescription = textToAction.generate(command, driver)

            assertNotNull(actionDescription)
            assertTrue(actionDescription.cssFriendlyExpressions.size <= 1, "Should handle validation command: $command")

            printlnPro("Validation command: $command (type: $expectedType)")
            printlnPro("Generated action: ${actionDescription.cssFriendlyExpressions}")
            val expression = actionDescription.cssFriendlyExpressions.first()
            assertTrue(expression.isNotBlank(), "Should generate non-empty action")
        }
    }

    // ======== RESPONSE ANALYSIS TESTS ========

    @Test
    fun `When analyzing responses then extract meaningful information`() = runEnhancedWebDriverTest { driver ->
        driver.navigateTo(ttaUrl1)
        driver.waitForSelector("body")

        val testCommand = "点击添加按钮"
        val actionDescription = textToAction.generate(testCommand, driver)

        // Analyze the response structure
        printlnPro("=== RESPONSE ANALYSIS ===")
        printlnPro("Command: $testCommand")
        printlnPro("Function calls: ${actionDescription.cssFriendlyExpressions}")
        printlnPro("Number of function calls: ${actionDescription.cssFriendlyExpressions.size}")
        printlnPro("Model response state: ${actionDescription.modelResponse?.state}")
        printlnPro("Model response content length: ${actionDescription.modelResponse?.content?.length}")


        // Validate response structure
        assertNotNull(actionDescription.cssFriendlyExpressions, "Function calls should not be null")
        assertNotNull(actionDescription.modelResponse, "Model response should not be null")
        assertTrue(actionDescription.cssFriendlyExpressions.size <= 1, "Should have at most one function call")

        // If there are function calls, validate their format
        actionDescription.cssFriendlyExpressions.forEach { functionCall ->
            assertTrue(
                functionCall.startsWith("driver.") || functionCall.contains("driver."),
                "Function call should reference driver: $functionCall"
            )
            assertTrue(
                functionCall.contains("(") && functionCall.contains(")"),
                "Function call should have proper syntax: $functionCall"
            )
        }

        // Validate model response
        assertTrue(
            actionDescription.modelResponse!!.content.isNotBlank() ||
                    actionDescription.modelResponse == ModelResponse.LLM_NOT_AVAILABLE,
            "Model response should have content or be LLM_NOT_AVAILABLE"
        )
    }
}

