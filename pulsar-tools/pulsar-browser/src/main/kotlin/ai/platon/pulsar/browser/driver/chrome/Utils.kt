package ai.platon.pulsar.browser.driver.chrome

import com.github.kklisura.cdt.protocol.support.types.EventHandler
import com.github.kklisura.cdt.protocol.support.types.EventListener
import java.util.concurrent.CountDownLatch
import java.util.function.Function

/**
 * Waits for event from a given event consumer.
 *
 * @param eventConsumer Event consumer.
 * @param <T> Type of an event.
 */
fun <T> waitForEvent(eventConsumer: Function<EventHandler<T>, EventListener>) {
    val countDownLatch = CountDownLatch(1)
    val eventListener = eventConsumer.apply(EventHandler { event: T -> countDownLatch.countDown() })
    try {
        countDownLatch.await()
    } catch (e: InterruptedException) {
        throw RuntimeException("Interrupted while waiting for event.", e)
    } finally {
        eventListener.unsubscribe()
    }
}
