package ai.platon.pulsar.rest.openapi.controller

import ai.platon.pulsar.rest.openapi.dto.*
import ai.platon.pulsar.rest.openapi.service.SessionManager
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for PerceptiveAgent operations.
 * Provides AI-powered browser automation capabilities.
 */
@RestController
@CrossOrigin
@RequestMapping(
    "/session/{sessionId}/agent",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ConditionalOnBean(SessionManager::class)
class AgentController(
    private val sessionManager: SessionManager
) {
    private val logger = LoggerFactory.getLogger(AgentController::class.java)

    /**
     * Runs an autonomous agent task.
     * The agent observes the page, acts, and repeats until the goal is achieved.
     */
    @PostMapping("/run", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun run(
        @PathVariable sessionId: String,
        @RequestBody request: AgentRunRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} running agent task: {}", sessionId, request.task.take(100))
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val result = try {
            // Use real PerceptiveAgent.run
            val history = runBlocking {
                session.agent.run(request.task)
            }

            AgentRunResult(
                success = !history.hasErrors,
                message = if (history.hasErrors) "Agent task has errors" else "Agent task completed",
                historySize = history.size,
                processTraceSize = history.size
            )
        } catch (e: Exception) {
            logger.error("Error running agent task: {}", e.message, e)
            AgentRunResult(
                success = false,
                message = "Error: ${e.message}",
                historySize = 0,
                processTraceSize = 0
            )
        }

        return ResponseEntity.ok(AgentRunResponse(value = result))
    }

    /**
     * Observes the current page and returns potential actions.
     */
    @PostMapping("/observe", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun observe(
        @PathVariable sessionId: String,
        @RequestBody request: AgentObserveRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} observing page with instruction: {}", sessionId, request.instruction?.take(100))
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val result = try {
            // Use real PerceptiveAgent.observe
            val observeResults = runBlocking {
                session.agent.observe(request.instruction ?: "")
            }

            // Convert to DTOs
            observeResults.map { observeResult ->
                ObserveResultDto(
                    locator = observeResult.locator,
                    domain = observeResult.domain,
                    method = observeResult.method,
                    arguments = observeResult.arguments,
                    description = observeResult.description
                )
            }
        } catch (e: Exception) {
            logger.error("Error observing page: {}", e.message, e)
            emptyList()
        }

        return ResponseEntity.ok(AgentObserveResponse(value = result))
    }

    /**
     * Executes a single action on the page.
     */
    @PostMapping("/act", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun act(
        @PathVariable sessionId: String,
        @RequestBody request: AgentActRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} executing action: {}", sessionId, request.action.take(100))
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val result = try {
            // Use real PerceptiveAgent.act
            val actResult = runBlocking {
                session.agent.act(request.action)
            }

            ActResultDto(
                success = actResult.success,
                message = actResult.message
                    ?: (if (actResult.success) "Action executed successfully" else "Action failed"),
                action = request.action,
                isComplete = actResult.isComplete
            )
        } catch (e: Exception) {
            logger.error("Error executing action: {}", e.message, e)
            ActResultDto(
                success = false,
                message = "Error: ${e.message}",
                action = request.action,
                isComplete = false
            )
        }

        return ResponseEntity.ok(AgentActResponse(value = result))
    }

    /**
     * Extracts structured data from the page.
     */
    @PostMapping("/extract", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun extract(
        @PathVariable sessionId: String,
        @RequestBody request: AgentExtractRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} extracting data with instruction: {}", sessionId, request.instruction.take(100))
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val result = try {
            // Use real PerceptiveAgent.extract
            val extractResult = runBlocking {
                session.agent.extract(request.instruction)
            }

            ExtractResultDto(
                success = extractResult.success,
                data = extractResult.data,
                message = extractResult.message
            )
        } catch (e: Exception) {
            logger.error("Error extracting data: {}", e.message, e)
            ExtractResultDto(
                success = false,
                data = null,
                message = "Error: ${e.message}"
            )
        }

        return ResponseEntity.ok(AgentExtractResponse(value = result))
    }

    /**
     * Summarizes the current page content.
     */
    @PostMapping("/summarize", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun summarize(
        @PathVariable sessionId: String,
        @RequestBody request: AgentSummarizeRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} summarizing page", sessionId)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val summary = try {
            // Use real PerceptiveAgent.summarize
            runBlocking {
                session.agent.summarize(
                    instruction = request.instruction,
                    selector = request.selector
                )
            }
        } catch (e: Exception) {
            logger.error("Error summarizing page: {}", e.message, e)
            "Error: ${e.message}"
        }

        return ResponseEntity.ok(AgentSummarizeResponse(value = summary))
    }

    /**
     * Clears the agent's history.
     */
    @PostMapping("/clearHistory", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun clearHistory(
        @PathVariable sessionId: String,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.debug("Session {} clearing agent history", sessionId)
        ControllerUtils.addRequestId(response)

        val session = sessionManager.getSession(sessionId)
            ?: return ControllerUtils.notFound("session not found", "No active session with id $sessionId")

        val success = try {
            // Use real PerceptiveAgent.clearHistory
            runBlocking {
                session.agent.clearHistory()
            }
            true
        } catch (e: Exception) {
            logger.error("Error clearing agent history: {}", e.message, e)
            false
        }

        return ResponseEntity.ok(AgentClearHistoryResponse(value = success))
    }
}
