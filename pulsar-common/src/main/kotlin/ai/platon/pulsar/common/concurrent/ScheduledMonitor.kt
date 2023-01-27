package ai.platon.pulsar.common.concurrent

@Deprecated("Inappropriate name", ReplaceWith("GracefulScheduledExecutorService"))
abstract class ScheduledMonitor: GracefulScheduledExecutor() {
    abstract fun watch()
}
