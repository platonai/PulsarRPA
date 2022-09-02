package ai.platon.pulsar.crawl.event

import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.crawl.AbstractWebDriverHandler
import ai.platon.pulsar.crawl.AbstractWebPageWebDriverHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import kotlinx.coroutines.delay
import java.time.Duration

open class LoginHandler(
    val loginUrl: String,
    val usernameSelector: String,
    val username: String,
    val passwordSelector: String,
    val password: String,
    val submitSelector: String,
    val warnUpUrl: String? = null,
    val activateSelector: String? = null,
    val activateTimeout: Duration = Duration.ofMinutes(3),
) : AbstractWebPageWebDriverHandler() {

    private val logger = getLogger(this)

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        logger.info("Navigating to login page ... | {}", loginUrl)

        warnUpUrl?.let {
            driver.navigateTo(it)
            driver.waitForNavigation(Duration.ofSeconds(10))
        }

        driver.navigateTo(loginUrl)
        driver.waitForNavigation(Duration.ofSeconds(10))

        if (activateSelector != null) {
            logger.info("Waiting for login panel ... | {}", activateSelector)

            val time = driver.waitForSelector(activateSelector, activateTimeout)
            if (time <= 0) {
                logger.info("Can not active login panel in {}", activateTimeout)
                return null
            }
            driver.bringToFront()
            driver.click(activateSelector)
        }

        driver.bringToFront()
        driver.type(usernameSelector, username)
        driver.type(passwordSelector, password)
        driver.click(submitSelector, count = 2)

        logger.info("Cookies before login: {}", driver.getCookies())
        driver.waitForNavigation()
        logger.info("Cookies after login: {}", driver.getCookies())

        return null
    }
}

class CloseMaskLayerHandler(
    val closeSelector: String
) : AbstractWebPageWebDriverHandler() {
    private val logger = getLogger(this)

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        logger.info("Closing mask layer... | {}", closeSelector)
        var n = 5
        while (n-- > 0 && driver.exists(closeSelector)) {
            driver.bringToFront()
            driver.click(closeSelector)
            delay(1_000)
        }
        return null
    }
}
