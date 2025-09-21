package ai.platon.pulsar.test.mock2.server

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Use mock-sse.html to test this controller.
 * */
@RestController
@RequestMapping("/mock/api/ai/command")
@CrossOrigin
class MockAICommandController {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val tasks: MutableMap<String, ScrapeResponse> = ConcurrentHashMap()

    // 模拟提交命令：返回 ScrapeResponse
    @PostMapping(consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun submitCommand(@RequestBody command: String): ScrapeResponse {
        val uuid = UUID.randomUUID().toString()
        val response = ScrapeResponse()
        response.id = uuid
        response.statusCode = ResourceStatus.SC_PROCESSING
        tasks[uuid] = response
        executor.submit { processCommand(uuid, command) }
        return response
    }

    // Simulate command refine
    @PostMapping(value = ["/refine"], consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun refineCommand(@RequestBody command: String): ScrapeResponse {
        val uuid = UUID.randomUUID().toString()
        val response = ScrapeResponse()
        response.id = uuid
        response.statusCode = ResourceStatus.SC_PROCESSING
        tasks[uuid] = response
        refineCommand(uuid, command)
        return response
    }

    // SSE 推送任务状态，返回 ScrapeResponse
    @GetMapping(value = ["/stream/{uuid}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamResultForCommand(@PathVariable uuid: String): SseEmitter {
        val emitter = SseEmitter(0L) // 无超时
        executor.submit {
            try {
                while (true) {
                    val resp = tasks[uuid]
                    if (resp == null) {
                        val errorResp = ScrapeResponse()
                        errorResp.id = uuid
                        errorResp.statusCode = ResourceStatus.SC_INTERNAL_SERVER_ERROR
                        emitter.send(errorResp)
                        emitter.complete()
                        return@submit
                    }
                    when (resp.statusCode) {
                        ResourceStatus.SC_OK, ResourceStatus.SC_INTERNAL_SERVER_ERROR -> {
                            emitter.send(resp)
                            emitter.complete()
                            return@submit
                        }
                        else -> {
                            emitter.send(resp)
                            Thread.sleep(1000)
                        }
                    }
                }
            } catch (e: Exception) {
                try {
                    val errorResp = ScrapeResponse()
                    errorResp.id = uuid
                    errorResp.statusCode = ResourceStatus.SC_INTERNAL_SERVER_ERROR
                    emitter.send(errorResp)
                } catch (ignored: Exception) {}
                emitter.completeWithError(e)
            }
        }
        return emitter
    }

    // 模拟任务执行过程，更新 ScrapeResponse
    private fun refineCommand(uuid: String, command: String) {
        try {
            Thread.sleep(5000)
            val resp = tasks[uuid]
            if (resp != null) {
                resp.resultSet = listOf(
                    mapOf(
                        "refinedCommand" to "Refined:\n$command",
                    )
                )
                resp.statusCode = ResourceStatus.SC_OK
            }
        } catch (e: Exception) {
            val resp = tasks[uuid]
            if (resp != null) {
                resp.statusCode = ResourceStatus.SC_INTERNAL_SERVER_ERROR
            }
        }
    }

    // 模拟任务执行过程，更新 ScrapeResponse
    private fun processCommand(uuid: String, command: String) {
        try {
            Thread.sleep(5000)
            val resp = tasks[uuid]
            if (resp != null) {
                resp.statusCode = ResourceStatus.SC_OK
            }
        } catch (e: Exception) {
            val resp = tasks[uuid]
            if (resp != null) {
                resp.statusCode = ResourceStatus.SC_INTERNAL_SERVER_ERROR
            }
        }
    }
}
