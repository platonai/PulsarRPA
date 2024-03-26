package ai.platon.pulsar.t

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.emulator.Defaults
import com.google.common.collect.Iterators
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicInteger

class Twitter {
    private val session = PulsarContexts.createSession()
    private val interactSessionId = AtomicInteger()
    
    private val keywords = listOf("Facebook", "Google", "Amazon", "Microsoft", "Apple", "Netflix", "Tesla", "Alibaba", "Tencent", "Baidu")
    private val iterator = Iterators.cycle(keywords)
    
    fun visit() {
        val args = "-i 7s -ii 7s -ignoreFailure"
        
        val url = "https://twitter.com/home"
        val options = session.options(args)
        val be = options.event.browseEvent
        
        be.onDidScroll.addLast { page, driver ->
            interact(page, driver)
        }
        
        session.load(url, options)
    }
    
    private suspend fun checkPreference() {
        val conf = ImmutableConfig()
        val driverPoolManager = session.context.getBeanOrNull(WebDriverPoolManager::class)
            ?: Defaults(conf).driverPoolManager
        val fetchTaskTimeout = driverPoolManager.driverSettings.fetchTaskTimeout
        println(fetchTaskTimeout)
    }
    
    private suspend fun interact(page: WebPage, driver: WebDriver) {
        interact0(page, driver)
    }
    
    private suspend fun interact0(page: WebPage, driver: WebDriver) {
        // login
        if (page.url.contains("login")) {
//            driver.findElementByCssSelector("input[name='session[username_or_email]']").sendKeys("username")
//            driver.findElementByCssSelector("input[name='session[password]']").sendKeys("password")
//            driver.findElementByCssSelector("div[data-testid='LoginForm_Login_Button']").click()
            
            val username = System.getProperty("TWITTER_USERNAME")
            val password = System.getProperty("TWITTER_PASSWORD")
            if (driver.visible("")) {
                driver.type("input[name='session[username_or_email]']", username)
                driver.type("input[name='session[username_or_email]']", password)
                driver.click("div[data-testid='LoginForm_Login_Button']")
            }
            
            driver.waitForNavigation()
        }

//            scheduledExecutor.scheduleAtFixedRate({ monitor(page, driver) },
//                1000, 2000, TimeUnit.SECONDS)
        
        supervisorScope {
            launch { interact1(page, driver) }
        }
    }

    private suspend fun interact1(page: WebPage, driver: WebDriver) {
        val id = interactSessionId.incrementAndGet()
        val browser = driver.browser
        val drivers = browser.listDrivers()

        drivers.forEach { driver1 ->
            println(String.format("%d. %s", id, driver1.currentUrl()))
        }

        val twitterDrivers = drivers.filter { it.currentUrl().contains("twitter.com") }

        twitterDrivers.forEach { driver1 ->
            println(driver1.currentUrl())
            delay(1000)

            val selector = "input[placeholder*=搜索], input[placeholder*=Search]"
            if (driver1.exists(selector)) {
                driver1.fill(selector, iterator.next())
                driver1.press(selector, "Space")
                "Email".uppercase().forEach { driver1.press(selector, "Key$it") }
                driver1.press(selector, "Enter")
            }
        }

        delay(2000)

        supervisorScope {
            launch { interact1(page, driver) }
        }
    }
}

fun main() {
    BrowserSettings.withSystemDefaultBrowser().withGUI().withSPA()

    Twitter().visit()
    readlnOrNull()
}
