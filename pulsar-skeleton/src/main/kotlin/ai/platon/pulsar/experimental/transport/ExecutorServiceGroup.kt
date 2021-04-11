package ai.platon.pulsar.experimental.transport

import java.util.concurrent.ExecutorService

class ExecutorServiceGroup(
    val executor: ExecutorService,
    val children: MutableList<ExecutorService>
) {
    fun next(): ExecutorService {
        return children.first()
    }
}
