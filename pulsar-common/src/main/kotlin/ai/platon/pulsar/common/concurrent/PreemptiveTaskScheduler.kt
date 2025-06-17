package ai.platon.pulsar.common.concurrent

// TaskScheduler.kt
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

// Task.kt
sealed class Task {
    abstract val id: String
    abstract val name: String
    abstract val priority: Int
}

class NormalTask(
    override val id: String,
    override val name: String,
    override val priority: Int = 0
) : Task()

class ManagementTask(
    override val id: String,
    override val name: String,
    override val priority: Int = 1
) : Task()

class PreemptiveTaskScheduler {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val taskChannel = Channel<Task>(UNLIMITED)
    private val runningTasks = ConcurrentHashMap<String, Job>()
    private val isManagementTaskRunning = AtomicBoolean(false)
    private val isManagementTaskPreparing = AtomicBoolean(false)

    init {
        startTaskProcessor()
    }

    suspend fun submitTask(task: Task) {
        taskChannel.send(task)
    }

    suspend fun prepareManagementTask(task: ManagementTask) {
        isManagementTaskPreparing.set(true)

        try {
            // 取消所有正在运行的普通任务
            runningTasks.forEach { (key, job) ->
                if (key.startsWith("normal_")) {
                    job.cancel()
                }
            }

            // 等待所有普通任务取消完成
            while (runningTasks.any { it.key.startsWith("normal_") }) {
                delay(100)
            }

            // 提交管理任务
            submitTask(task)
        } finally {
            isManagementTaskPreparing.set(false)
        }
    }

    fun shutdown() {
        scope.cancel()
        taskChannel.close()
    }

    private fun startTaskProcessor() {
        scope.launch {
            for (task in taskChannel) {
                processTask(task)
            }
        }
    }

    private suspend fun processTask(task: Task) {
        when (task) {
            is ManagementTask -> processManagementTask(task)
            is NormalTask -> processNormalTask(task)
        }
    }

    private suspend fun processManagementTask(task: ManagementTask) {
        // 等待所有普通任务完成
        while (runningTasks.any { it.value.isActive && it.key.startsWith("normal_") }) {
            delay(100)
        }

        // 设置管理任务运行状态
        isManagementTaskRunning.set(true)

        try {
            // 执行管理任务
            val job = scope.launch {
                println("Executing management task: ${task.name}")
                // 模拟任务执行
                delay(5000)
                println("Completed management task: ${task.name}")
            }

            runningTasks["management_${task.id}"] = job
            job.join()
        } finally {
            isManagementTaskRunning.set(false)
            runningTasks.remove("management_${task.id}")
        }
    }

    private suspend fun processNormalTask(task: NormalTask) {
        // 如果管理任务正在运行或准备运行，等待
        while (isManagementTaskRunning.get() || isManagementTaskPreparing.get()) {
            delay(100)
        }

        // 执行普通任务
        val job = scope.launch {
            println("Executing normal task: ${task.name}")
            // 模拟任务执行
            delay(3000)
            println("Completed normal task: ${task.name}")
        }

        runningTasks["normal_${task.id}"] = job
        job.join()
        runningTasks.remove("normal_${task.id}")
    }
}

// Main.kt
suspend fun main() {
    val scheduler = PreemptiveTaskScheduler()

    // 提交一些普通任务
    repeat(3) { i ->
        scheduler.submitTask(NormalTask("normal_$i", "Normal Task $i"))
    }

    // 准备执行管理任务
    val managementTask = ManagementTask("management_1", "Management Task 1")
    scheduler.prepareManagementTask(managementTask)

    // 尝试提交更多任务
    repeat(2) { i ->
        scheduler.submitTask(NormalTask("normal_${i + 3}", "Normal Task ${i + 3}"))
    }

    // 等待一段时间后关闭
    delay(10000)
    scheduler.shutdown()
}
