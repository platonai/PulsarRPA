package ai.platon.pulsar.common.collect

interface Loadable<T> {
    fun load()
    fun loadNow(): Collection<T>
}
