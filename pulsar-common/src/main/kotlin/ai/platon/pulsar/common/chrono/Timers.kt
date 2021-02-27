package ai.platon.pulsar.common.chrono

import java.time.Duration
import java.util.*

fun Timer.scheduleAtFixedRate(delay: Duration, period: Duration, task: () -> Unit) {
    scheduleAtFixedRate(object: TimerTask() {
        override fun run() {
            task()
        }
    }, delay.toMillis(), period.toMillis())
}
