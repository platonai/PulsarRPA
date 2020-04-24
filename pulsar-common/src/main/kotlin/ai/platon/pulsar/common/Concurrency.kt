package ai.platon.pulsar.common

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Stops the ExecutorService and if shutdownExecutorOnStop is true then shuts down its thread of execution.
 * Uses the shutdown pattern from http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
 */
fun stopExecution(executor: ExecutorService, future: Future<*>?, shutdown: Boolean = false) {
    if (shutdown) {
        executor.shutdown() // Disable new tasks from being submitted
        try { // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate")
                }
            }
        } catch (ie: InterruptedException) { // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow()
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
