package ai.platon.pulsar.common

import com.google.common.annotations.Beta
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implements the preemptive channel concurrency pattern, which consists of two channels: preemptive and normal.
 *
 * Key characteristics:
 * 1. Both channels allow multiple threads.
 * 2. New workers must wait until there are no ready or running preemptive tasks.
 * 3. A preemptive task locks the working channel immediately but must wait to run until all workers are finished.
 *
 *                     |------ waiting ------------|- ready -|-------------- critical  -------------------|---finished----
 *
 *                                              The entrance gate                                    The exit gate
 * Preemptive channel: ------------#1--#1--#1------|----#2---|-----------------------------------#3-------|--- #4 --------
 *                                                 |         |                                            |
 * Normal channel:     ----*1----*1--*1------*1----|---------|-----------*2--*2------*2--*2---------------|--- *3 --------
 *
 * #1 The waiting preemptive tasks
 * #2 The ready preemptive tasks
 * #3 The running preemptive tasks
 * #4 The finished preemptive tasks
 *
 * *1 The waiting workers
 * *2 The running workers
 * *3 The finished workers
 */
@Beta
open class PreemptChannelSupportNonBlocking(val name: String = "") {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val preemptiveMutex = Mutex()
    private val normalMutex = Mutex()

    private val _preemptiveTasks = AtomicInteger()
    private val _runningPreemptiveTasks = AtomicInteger()
    private val _pendingNormalTasks = AtomicInteger()
    private val _runningNormalTasks = AtomicInteger()

    val preemptiveTasks get() = _preemptiveTasks.get()
    val runningPreemptiveTasks get() = _runningPreemptiveTasks.get()
    val pendingNormalTasks get() = _pendingNormalTasks.get()
    val runningNormalTasks get() = _runningNormalTasks.get()

    val hasEvent get() = arrayOf(runningPreemptiveTasks,
        runningPreemptiveTasks, pendingNormalTasks, runningNormalTasks).sumOf { it } > 0

    val isPreempted get() = runningPreemptiveTasks > 0

    val hasPreemptiveTasks get() = runningNormalTasks > 0

    val hasNormalTasks get() = runningNormalTasks > 0

    suspend fun <T> preempt(block: suspend () -> T): T {
        _preemptiveTasks.incrementAndGet()

        // Waits for all running normal tasks to finish
        while (_runningNormalTasks.get() > 0) {
            delay(100)
        }
        _runningPreemptiveTasks.incrementAndGet()

        return try {
            block()
        } finally {
            _runningPreemptiveTasks.decrementAndGet()
            _preemptiveTasks.decrementAndGet()
        }
    }

    suspend fun <T> whenNormal(block: suspend () -> T): T {
        _pendingNormalTasks.incrementAndGet()

        // Waits for all preemptive tasks to finish
        while (_preemptiveTasks.get() > 0) {
            delay(100)
        }
        _runningNormalTasks.incrementAndGet()

        _pendingNormalTasks.decrementAndGet()

        return try {
            block()
        } finally {
            _runningNormalTasks.decrementAndGet()
        }
    }

    fun releaseLocks() {
        scope.launch {
            preemptiveMutex.unlock()
            normalMutex.unlock()
        }
    }
}
