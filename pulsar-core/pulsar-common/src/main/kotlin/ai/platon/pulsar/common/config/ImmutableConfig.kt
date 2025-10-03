package ai.platon.pulsar.common.config

/**
 *
 * ImmutableConfig class.
 *
 * @author vincent
 * @version $Id: $Id
 */
open class ImmutableConfig : AbstractRelaxedConfiguration {

    constructor(): this(false)

    constructor(loadDefaults: Boolean = false): super(MultiSourceProperties(loadDefaults))

    constructor(props: MultiSourceProperties) : super(props)

    constructor(conf: ImmutableConfig) : super(conf.unbox()) {
        this.environment = conf.environment
    }

    /**
     *
     * toMutableConfig.
     *
     * @return a [MutableConfig] object.
     */
    fun toMutableConfig(): MutableConfig {
        return MutableConfig(this)
    }

    /**
     *
     * toVolatileConfig.
     *
     * @return a [ai.platon.pulsar.common.config.VolatileConfig] object.
     */
    open fun toVolatileConfig(): VolatileConfig {
        return VolatileConfig(this)
    }

    companion object {
        val EMPTY = ImmutableConfig()

        val UNSAFE = ImmutableConfig()

        val DEFAULT = ImmutableConfig(loadDefaults = true)
    }
}
