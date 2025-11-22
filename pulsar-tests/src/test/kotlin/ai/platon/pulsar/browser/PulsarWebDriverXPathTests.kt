package ai.platon.pulsar.browser

import ai.platon.pulsar.WebDriverTestBase
import org.junit.jupiter.api.Disabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PulsarWebDriverXPathTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    @Test
    fun `test selectFirstTextOrNull by xpath with id`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        driver.waitForSelector("#preferencesSection")
        val text = driver.selectFirstTextOrNull("//*[@id='preferencesSection']/h2")
        assertEquals("""📊 Preferences""", text)
    }

    @Test
    fun `test selectFirstTextOrNull by xpath with data-testid`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[@data-testid='calculatorSection']/h2")
        assertEquals("🧮 Quick Calculator", text)
    }

    @Test
    fun `test selectFirstTextOrNull by xpath with contains`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[contains(@class, 'section-toggle')]/h2")
        assertEquals("🎯 Dynamic Toggle", text)
    }

    @Test
    fun `test selectFirstTextOrNull by xpath with text content`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//h2[contains(text(), 'Email Validation')]")
        assertEquals("✉️ Email Validation", text)
    }

    @Test
    fun `test selectFirstAttributeOrNull by xpath`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val placeholder = driver.selectFirstAttributeOrNull("//input[@id='name']", "placeholder")
        assertEquals("Type here...", placeholder)
    }

    @Test
    fun `test selectFirstAttributeOrNull by xpath with data attribute`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val dataComponent = driver.selectFirstAttributeOrNull("//input[@data-testid='nameInput']", "data-component")
        assertEquals("name-input", dataComponent)
    }

    @Test
    fun `test selectFirstAttributeOrNull by xpath for button`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val ariaLabel = driver.selectFirstAttributeOrNull("//button[@id='addButton']", "aria-label")
        assertEquals("Add the two numbers", ariaLabel)
    }

    @Disabled("NOT IMPLEMENTED")
    @Test
    fun `test selectTextAll by xpath with multiple elements`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val texts = driver.selectTextAll("//section/h2")
        assertTrue(texts.size >= 7)
        assertTrue(texts.contains("📋 User Information"))
        assertTrue(texts.contains("📊 Preferences"))
        assertTrue(texts.contains("🧮 Quick Calculator"))
    }

    @Test
    fun `test selectFirstTextOrNull by xpath with descendant`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[@id='emailValidationSection']//h2")
        assertEquals("✉️ Email Validation", text)
    }

    @Disabled("MAY NOT SUPPORTED BY CDP")
    @Test
    fun `test selectFirstTextOrNull by xpath with parent`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//input[@id='email2']/parent::section/h2")
        assertEquals("""🔒 Contact Us""", text)
    }

    @Disabled("MAY NOT SUPPORTED BY CDP")
    @Test
    fun `test selectFirstTextOrNull by xpath with following-sibling`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//h1/following-sibling::p")
        assertEquals(text?.contains("demonstrates various interactive elements"), true)
    }

    @Test
    fun `test selectFirstAttributeOrNull by xpath with multiple conditions`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val dataRole = driver.selectFirstAttributeOrNull(
            "//button[@data-testid='toggleMessageButton' and @data-component='toggle-message-button']",
            "data-role"
        )
        assertEquals("button", dataRole)
    }

    @Disabled("NOT IMPLEMENTED")
    @Test
    fun `test selectAttributeAll by xpath for href attributes`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val hrefs = driver.selectAttributeAll("//a[contains(@href, 'example.com')]", "href")
        assertTrue(hrefs.isNotEmpty())
        assertTrue(hrefs.all { it.contains("example.com") })
    }

    @Disabled("ONLY XPATH START WITH // SUPPORTED")
    @Test
    fun `test selectFirstTextOrNull by xpath with position`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("(//section/h2)[1]")
        assertEquals("\uD83C\uDFAF Dynamic Toggle", text)
    }

//    @Test
//    fun `test selectFirstAttributeOrNull by xpath with ancestor`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
//        val sectionId = driver.selectFirstAttributeOrNull(
//            "//button[@id='toggleMessageButton']/ancestor::section",
//            "id"
//        )
//        assertEquals("toggleSection", sectionId)
//    }

    @Disabled("NOT IMPLEMENTED")
    @Test
    fun `test selectTextAll by xpath with specific class pattern`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val texts = driver.selectTextAll("//section[@class and contains(@class, 'section-')]/h2")
        assertTrue(texts.size >= 5)
    }

    @Test
    fun `test selectFirstAttributeOrNull by xpath for select element`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val ariaLabel = driver.selectFirstAttributeOrNull("//select[@id='colorSelect']", "aria-label")
        assertEquals("Favorite color selector", ariaLabel)
    }

    @Disabled("NOT IMPLEMENTED")
    @Test
    fun `test selectAttributeAll by xpath for all data-testid`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val testIds = driver.selectAttributeAll("//section[@data-testid]", "data-testid", start = 0, limit = 20)
        assertTrue(testIds.isNotEmpty())
        assertTrue(testIds.contains("userInformationSection"))
        assertTrue(testIds.contains("preferencesSection"))
    }

    @Test
    fun `test selectFirstTextOrNull by xpath with not condition`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val text = driver.selectFirstTextOrNull("//section[not(@class='hidden')]/h2[1]")
        assertNotNull(text)
        assertTrue(text.isNotEmpty())
    }
}
