package ai.platon.pulsar.skeleton.context

import ai.platon.pulsar.skeleton.context.PulsarContext

interface ContextAware {
    var context: PulsarContext?
}
