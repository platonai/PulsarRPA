package ai.platon.pulsar.test.server

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
    private val tasks: MutableMap<String, TaskStatus> = ConcurrentHashMap()

    // 模拟提交命令：返回 UUID
    @PostMapping(consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun submitCommand(@RequestBody command: String): String {
        val uuid = UUID.randomUUID().toString()
        val status = TaskStatus("processing", null, null)
        tasks[uuid] = status

        // 模拟后台异步执行任务
        executor.submit { processCommand(uuid, command) }
        return uuid
    }

    // SSE 推送任务状态
    @GetMapping(value = ["/stream/{uuid}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamResult(@PathVariable uuid: String): SseEmitter {
        val emitter = SseEmitter(0L) // 无超时

        executor.submit {
            try {
                while (true) {
                    val task = tasks[uuid]
                    if (task == null) {
                        emitter.send(
                            java.util.Map.of(
                                "status",
                                "failed",
                                "error",
                                "Task not found"
                            )
                        )
                        emitter.complete()
                        return@submit
                    }

                    if ("completed" == task.status) {
                        emitter.send(
                            java.util.Map.of(
                                "status",
                                "completed",
                                "result",
                                task.result
                            )
                        )
                        emitter.complete()
                        return@submit
                    } else if ("failed" == task.status) {
                        emitter.send(
                            java.util.Map.of(
                                "status",
                                "failed",
                                "error",
                                task.error
                            )
                        )
                        emitter.complete()
                        return@submit
                    } else {
                        emitter.send(java.util.Map.of("status", "processing"))
                        Thread.sleep(1000)
                    }
                }
            } catch (e: Exception) {
                try {
                    emitter.send(
                        java.util.Map.of(
                            "status",
                            "failed",
                            "error",
                            e.message
                        )
                    )
                } catch (ignored: Exception) {
                }
                emitter.completeWithError(e)
            }
        }

        return emitter
    }

    // 模拟任务执行过程
    private fun processCommand(uuid: String, command: String) {
        try {
            // 模拟耗时任务
            Thread.sleep(5000)

            // 模拟执行结果
            val result = mapOf(
                "productName" to "Mock Product",
                "price" to "$19.99",
                "ratings" to "4.5 stars",
                "summary" to "This is a mock summary of the product.",
                "links" to listOf("/dp/B0C1H26C46", "/dp/B0C1234567")
            )

            tasks[uuid] = TaskStatus("completed", result, null)
        } catch (e: Exception) {
            tasks[uuid] = TaskStatus("failed", null, e.message)
        }
    }

    // 内部类表示任务状态
    internal class TaskStatus(// processing, completed, failed
        var status: String, var result: Map<String, Any>?, var error: String?
    )
}
