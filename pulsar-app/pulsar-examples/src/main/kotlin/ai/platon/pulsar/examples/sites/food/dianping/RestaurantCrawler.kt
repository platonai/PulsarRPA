package ai.platon.pulsar.examples.sites.food.dianping

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import java.nio.file.Files
import java.util.*

class WebDriverExt(
    val page: WebPage,
    val driver: WebDriver
) {

    suspend fun screenshotNoExcept(name: String, selector: String) {
        kotlin.runCatching { screenshot(name, selector) }.onFailure { it.printStackTrace() }
    }

    suspend fun screenshot(name: String, selector: String) {
        val screenshot = driver.captureScreenshot(selector) ?: return

        val path = AppPaths.WEB_CACHE_DIR
            .resolve("screenshot")
            .resolve(AppPaths.fileId(page.url))
            .resolve("$name.jpg")
        AppFiles.saveTo(Base64.getDecoder().decode(screenshot), path, true)

        val metadataPath = path.resolveSibling("_metadata.txt")
        val exportPath = page.getVar(Name.ORIGINAL_EXPORT_PATH.name)
        val metadata = "url:\t${page.url}\nexportPath:\t$exportPath"
        Files.writeString(metadataPath, metadata)
    }
}

fun main() {
    val url = "https://www.dianping.com/shop/Enk0gTkqu0Cyj7Ch"
    val args = "-i 1s -ignoreFailure"

    val session = PulsarContexts.createSession()

    val fieldSelectors = mapOf(
        "title" to ".basic-info h2",
        "score" to ".basic-info .brief-info .mid-score",
        "reviewCount" to "#reviewCount",
        "avgPrice" to "#avgPriceTitle",
        "commentScores" to "#comment_score",
        "address" to "#address",
        "tel" to ".tel",
    )

    val options = session.options(args)
    options.ensureEventHandler().simulateEventHandler.onAfterComputeFeature.addLast { page, driver ->
        fieldSelectors.entries.asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
            WebDriverExt(page, driver).screenshotNoExcept(name, selector)
        }
    }
    session.load(url, options)

    val fields = session.scrape(url, args, fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
