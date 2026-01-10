package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.service.SessionManager
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for health check endpoints.
 */
@RestController
@CrossOrigin
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@ConditionalOnBean(SessionManager::class)
class HealthController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(HealthController::class.java)

    /**
     * Basic health check endpoint.
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        logger.debug("Health check requested")

        val sessionCount = sessionManager.getActiveSessionCount()
        val response = mapOf<String, Any>(
            "status" to "UP",
            "activeSessions" to sessionCount
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Readiness check endpoint.
     */
    @GetMapping("/health/ready")
    fun ready(): ResponseEntity<Map<String, Any>> {
        logger.debug("Readiness check requested")

        val response = mapOf(
            "ready" to true
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Liveness check endpoint.
     */
    @GetMapping("/health/live")
    fun live(): ResponseEntity<Map<String, Any>> {
        logger.debug("Liveness check requested")

        val response = mapOf(
            "live" to true
        )

        return ResponseEntity.ok(response)
    }
}
