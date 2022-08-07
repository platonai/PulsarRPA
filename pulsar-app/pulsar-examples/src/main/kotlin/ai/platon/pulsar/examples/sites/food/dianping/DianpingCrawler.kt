package ai.platon.pulsar.examples.sites.food.dianping

import ai.platon.pulsar.context.PulsarContexts
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn

fun main() {
    val portalUrl = "https://www.dianping.com/beijing/ch10/g110"
    val args = "-i 1s -ii 5d -ol \"#shop-all-list .tit a[href~=shop]\" -ignoreFailure"

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

    val fields = session.scrapeOutPages(portalUrl, options, fieldSelectors)
    println(GsonBuilder().setPrettyPrinting().create().toJson(fields))
}
