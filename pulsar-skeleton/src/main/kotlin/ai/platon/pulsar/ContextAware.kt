package ai.platon.pulsar

import ai.platon.pulsar.context.PulsarContext

interface ContextAware {
    var context: PulsarContext?
}
