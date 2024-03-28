package ai.platon.pulsar.t

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.AppPaths
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
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class Twitter {
    private val session = PulsarContexts.createSession()
    private val interactSessionId = AtomicInteger()
    private val accountPath = AppPaths.DATA_DIR.resolve("accounts/enabled-accounts/twitter-accounts.txt")
    private val accounts = Files.readAllLines(accountPath)
        .map { it.split("\\s".toRegex()) }
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }
    
    private val keywords = listOf("Facebook", "Google", "Amazon", "Microsoft", "Apple", "Netflix", "Tesla", "Alibaba",
        "Tencent", "Baidu", "JD", "Pinduoduo", "Meituan", "ByteDance", "Huawei", "Xiaomi", "Oppo", "Vivo", "OnePlus",)
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
        loginIfNecessary(page, driver)
        
        supervisorScope {
            launch { interact1(page, driver) }
        }
    }
    
    private suspend fun loginIfNecessary(page: WebPage, driver: WebDriver) {
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
    }
    
    private suspend fun interact1(page: WebPage, driver: WebDriver) {
        val id = interactSessionId.incrementAndGet()
        val browser = driver.browser
        val drivers = browser.listDrivers()

        drivers.forEach { driver1 ->
            println(String.format("%d. %s", id, driver1.currentUrl()))
        }

        val twitterDrivers = drivers.filter { it.currentUrl().contains("twitter.com") }

        twitterDrivers.forEach { dr ->
            println(dr.currentUrl())
            delay(1000)
            interactWithPage(dr)
        }

        delay(2000)

        supervisorScope {
            launch { interact1(page, driver) }
        }
    }

    private suspend fun interactWithPage(driver: WebDriver) {
        // interact with the page
        val selector = "input[placeholder*=搜索], input[placeholder*=Search]"
        if (!driver.exists(selector)) {
            return
        }

        driver.fill(selector, iterator.next())
        driver.press(selector, "Space")
        "Email".uppercase().forEach { driver.press(selector, "Key$it") }
        driver.press(selector, "Enter")
    }
}

fun main() {
    BrowserSettings.withSystemDefaultBrowser().withGUI().withSPA()

    Twitter().visit()
    readlnOrNull()
}
