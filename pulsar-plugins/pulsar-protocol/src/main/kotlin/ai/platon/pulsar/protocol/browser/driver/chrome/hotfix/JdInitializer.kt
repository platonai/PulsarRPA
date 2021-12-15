package ai.platon.pulsar.protocol.browser.driver.chrome.hotfix

import ai.platon.pulsar.common.LinkExtractors
import ai.platon.pulsar.common.sleepSeconds
import com.github.kklisura.cdt.protocol.commands.Page
import kotlin.random.Random

class JdInitializer {
    companion object {
        val categories = LinkExtractors.fromResource("hotfix/sites/jd/categories.txt").toList()
    }

    fun init(page: Page) {
        page.navigate("https://www.jd.com/")

        val randomIndex = Random.nextInt(categories.size)
        val randomCategoryUrl = categories[randomIndex]
        page.navigate(randomCategoryUrl)
        sleepSeconds(3)
        // go to about:blank
        page.navigate("about:blank")
    }
}
