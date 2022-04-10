package ai.platon.pulsar.context

import ai.platon.pulsar.context.PulsarContext

interface ContextAware {
    var context: PulsarContext?
}
