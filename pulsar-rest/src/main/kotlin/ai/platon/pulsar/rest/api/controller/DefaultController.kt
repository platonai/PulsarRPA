package ai.platon.pulsar.rest.api.controller

import ai.platon.pulsar.common.catastrophicError
import ai.platon.pulsar.ql.context.H2SQLContext
import ai.platon.pulsar.ql.h2.H2SQLSession
import ai.platon.pulsar.skeleton.crawl.CoreMetrics
import ai.platon.pulsar.skeleton.crawl.event.impl.DefaultPageEventHandlers
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping("/")
class DefaultController(
    val session: PulsarSession,
    val coreMetrics: CoreMetrics
) {
    @GetMapping("init")
    fun init(): Map<String, String> {
        try {
            require(session is H2SQLSession)
            require(session.context is H2SQLContext)
            require(session.isActive)

            return mapOf(
                "status" to "healthy",
                "message" to "Browser4 initialized"
            )
        } catch (e: Throwable) {
            catastrophicError(e, "Failed to initialize Browser4")
            return mapOf(
                "status" to "error",
                "message" to "Failed to initialize Browser4"
            )
        }
    }

    @GetMapping("health")
    fun health(): Map<String, String> {
        return if (session.context.isActive) {
            mapOf(
                "status" to "healthy"
            )
        } else {
            mapOf(
                "status" to "unhealthy"
            )
        }
    }
}
