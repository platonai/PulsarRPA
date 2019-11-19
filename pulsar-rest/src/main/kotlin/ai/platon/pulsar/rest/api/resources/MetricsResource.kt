package ai.platon.pulsar.rest.api.resources

import ai.platon.pulsar.common.config.PulsarConstants.METRICS_HOME_URL
import ai.platon.pulsar.persist.WebDb
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metrics")
class MetricsResource @Autowired
constructor(private val webDb: WebDb) {

    @GetMapping
    fun list(@PathVariable("limit") limit: Int): String {
        val page = webDb.getOrNil(METRICS_HOME_URL)
        return if (page.isNil || page.liveLinks.isEmpty()) {
            "[]"
        } else {
            "[\n" + page.liveLinks.values.take(limit).joinToString { it.toString() }
        }
    }
}
