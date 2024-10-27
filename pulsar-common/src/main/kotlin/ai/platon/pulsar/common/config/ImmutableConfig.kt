package ai.platon.pulsar.common.config

/**
 *
 * ImmutableConfig class.
 *
 * @author vincent
 * @version $Id: $Id
 */
open class ImmutableConfig : AbstractConfiguration {

    constructor(): this(false)

    constructor(loadDefaults: Boolean): this(
        System.getProperty(CapabilityTypes.PROFILE_KEY, ""),
        loadDefaults
    )

    constructor(
        profile: String = System.getProperty(CapabilityTypes.PROFILE_KEY, ""),
        loadDefaults: Boolean = true,
        resources: Iterable<String> = mutableSetOf()
    ): super(profile, loadDefaults, resources)

    constructor(conf: KConfiguration) : super(conf)

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
