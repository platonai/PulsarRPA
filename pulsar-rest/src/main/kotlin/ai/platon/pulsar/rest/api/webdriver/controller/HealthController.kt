package ai.platon.pulsar.rest.api.webdriver.controller

import ai.platon.pulsar.rest.api.webdriver.service.SessionManager
import ai.platon.pulsar.rest.api.webdriver.store.InMemoryStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
class HealthController(
    @param:Autowired(required = false) private val sessionManager: SessionManager?,
    private val store: InMemoryStore
) {
    private val logger = LoggerFactory.getLogger(HealthController::class.java)

    /**
     * Basic health check endpoint.
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        logger.debug("Health check requested")
        
        val useRealSessions = sessionManager != null
        val status = "UP"
        
        val response = mutableMapOf<String, Any>(
            "status" to status,
            "mode" to if (useRealSessions) "real" else "mock"
        )
        
        if (useRealSessions) {
            val sessionCount = sessionManager!!.getActiveSessionCount()
            response["activeSessions"] = sessionCount
        } else {
            response["activeSessions"] = store.getActiveSessionCount()
        }
        
        return ResponseEntity.ok(response)
    }

    /**
     * Readiness check endpoint.
     */
    @GetMapping("/health/ready")
    fun ready(): ResponseEntity<Map<String, Any>> {
        logger.debug("Readiness check requested")
        
        val useRealSessions = sessionManager != null
        val ready = true // Always ready in current implementation
        
        val response = mapOf(
            "ready" to ready,
            "mode" to if (useRealSessions) "real" else "mock"
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
