package ai.platon.pulsar.protocol.browser.react

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.driver.LoadingWebDriverPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class WebDriverEventLoop(
        val driverPool: LoadingWebDriverPool,
        val conf: ImmutableConfig
): AutoCloseable {
    private val closed = AtomicBoolean()
    val urls = ArrayBlockingQueue<String>(1000)
    val pendingTasks = ArrayBlockingQueue<FetchTask>(1000)
    val results = ConcurrentLinkedQueue<FetchResult>()

    fun run() {
        while (!closed.get()) {
            val task = pendingTasks.take()
            val driver = driverPool.poll(conf)
            GlobalScope.launch {
                println(1)
                driver.startWork()
                navigateTo(task, driver)
                println(2)
                val result = emulate(task, driver)
                println(3)
                println(result)
                results.add(FetchResult(task, ForwardingResponse.canceled(task.page)))
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    private suspend fun navigateTo(task: FetchTask, driver: ManagedWebDriver) {
        withContext(Dispatchers.IO) {
            driver.navigateTo(task.url)
        }
    }

    private suspend fun emulate(task: FetchTask, driver: ManagedWebDriver): Any? {
        return withContext(Dispatchers.IO) {
            driver.evaluate("7+5")
        }
    }
}
