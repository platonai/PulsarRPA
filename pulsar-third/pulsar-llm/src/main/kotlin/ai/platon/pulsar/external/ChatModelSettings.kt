package ai.platon.pulsar.external

import ai.platon.pulsar.common.config.ImmutableConfig

class ChatModelSettings(conf: ImmutableConfig) {

    /**
     * The maximum length of the prompt.
     * */
    val maximumLength = conf.getInt("chat.model.prompt.maximum.length", 65536 - 10000)
}
