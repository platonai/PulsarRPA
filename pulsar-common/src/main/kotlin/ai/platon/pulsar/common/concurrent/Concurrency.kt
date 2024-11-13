package ai.platon.pulsar.common.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

interface ShutdownHookRegistry {
    fun register(thread: Thread) {
        try {
            Runtime.getRuntime().addShutdownHook(thread)
        } catch (e: IllegalStateException) {
            // ignore if the JVM is already shutting down
        }
    }
    
    fun remove(thread: Thread) {
        try {
            Runtime.getRuntime().removeShutdownHook(thread)
        } catch (e: IllegalStateException) {
            // ignore if the JVM is already shutting down
        }
    }
}

class RuntimeShutdownHookRegistry : ShutdownHookRegistry

/**
 * Stops the ExecutorService and if shutdownExecutorOnStop is true then shuts down its thread of execution.
 * Uses the shutdown pattern from http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
 */
fun stopExecution(name: String, executor: ExecutorService, future: Future<*>?, shutdown: Boolean = false) {
    if (shutdown) {
        // Disable new tasks from being submitted
        executor.shutdown()
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                executor.shutdownNow()
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            // System.err.println("Interrupted, shutting down now (shutdown?$shutdown) | $name")
            // (Re-)Cancel if current thread also interrupted
            runCatching { executor.shutdownNow() }.onFailure { it.printStackTrace(System.err) }
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    } else { // The external manager(like JEE container) responsible for lifecycle of executor
        synchronized(executor) {
            // if already cancelled, nothing to do
            // or else, just cancel the future and exit
            future?.takeUnless { it.isCancelled }?.cancel(false)
        }
    }
}

