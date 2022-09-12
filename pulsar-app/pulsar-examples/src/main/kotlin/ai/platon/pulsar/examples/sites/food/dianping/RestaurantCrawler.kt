package ai.platon.pulsar.examples.sites.food.dianping

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.session.PulsarSession
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO

class Screenshot(
    val page: WebPage,
    val driver: WebDriver
) {
    companion object {
        val OCR = "OCR-"
    }

    private val logger = getLogger(this)

    suspend fun screenshot(name: String, selector: String): String? {
        val screenshot = driver.captureScreenshot(selector)
        if (screenshot == null) {
            logger.info("Failed to take screenshot for {}", selector)
            return null
        }

        val path = AppPaths.WEB_CACHE_DIR
            .resolve("screenshot")
            .resolve(AppPaths.fileId(page.url))
            .resolve("$name.jpg")
        val bytes = Base64.getDecoder().decode(screenshot)
        AppFiles.saveTo(bytes, path, true)

        return path.toString()
    }

    private fun getScaledImage(srcImg: BufferedImage, w: Int, h: Int): BufferedImage {
        val resizedImg = BufferedImage(w, h, Transparency.TRANSLUCENT)
        val g2 = resizedImg.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(srcImg, 0, 0, w, h, null)
        g2.dispose()
        return resizedImg
    }
}

class RestaurantCrawler(
    val session: PulsarSession = PulsarContexts.createSession()
) {
    val commentSelectors = IntRange(1, 10)
        .associate { i -> "comment-$i" to "#reviewlist-wrapper li.comment-item:nth-child($i) p.desc.J-desc" }

    val fieldSelectors = mutableMapOf(
        "title" to ".basic-info h2",
        "score" to ".basic-info .brief-info .mid-score",
        "reviewCount" to "#reviewCount",
        "avgPrice" to "#avgPriceTitle",
        "commentScores" to "#comment_score",
        "address" to "#address",
        "tel" to ".tel",
    ).also { it.putAll(commentSelectors) }

    fun options(args: String): LoadOptions {
        val options = session.options(args)
        val eh = options.ensureEventHandler()

        val seh = eh.simulateEventHandler
        seh.onWillComputeFeature.addLast { page, driver ->
            commentSelectors.entries.mapIndexed { i, _ ->
                "#reviewlist-wrapper .comment-item:nth-child($i) .more"
            }.asFlow().flowOn(Dispatchers.IO).collect { selector ->
                driver.click(selector)
                delay(500)
            }
        }

        seh.onFeatureComputed.addLast { page, driver ->
            fieldSelectors.entries.asFlow().flowOn(Dispatchers.IO).collect { (name, selector) ->
                delay(1500)

                Screenshot(page, driver).runCatching { screenshot(name, selector) }
                    .onFailure { it.printStackTrace() }.getOrNull()
            }
        }

        return options
    }
}

/**
 * Running the program directly in the IDE may crash the system, use command line instead:
 *
java -Xmx10g -Xms2G -cp pulsar-examples*.jar \
-D"loader.main=ai.platon.pulsar.examples.sites.food.dianping.RestaurantCrawlerKt" \
org.springframework.boot.loader.PropertiesLauncher
 * */
fun main() {
    val url = "https://www.dianping.com/shop/Enk0gTkqu0Cyj7Ch"
    val args = "-i 1s -ignoreFailure -parse"

    val session = PulsarContexts.createSession()
    val crawler = RestaurantCrawler(session)

    val fields = session.scrape(url, crawler.options(args), crawler.fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))

    readLine()
}
