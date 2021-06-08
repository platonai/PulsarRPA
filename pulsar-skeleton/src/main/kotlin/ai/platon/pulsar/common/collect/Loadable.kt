package ai.platon.pulsar.common.collect

import java.time.Duration

interface Loadable<T> {
    fun load()
    fun load(delay: Duration)
    fun loadNow(): Collection<T>
}
