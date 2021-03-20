package ai.platon.pulsar.common.config

/**
 *
 * ImmutableConfig class.
 *
 * @author vincent
 * @version $Id: $Id
 */
open class ImmutableConfig : AbstractConfiguration {

    @JvmOverloads
    constructor(profile: String = "", loadDefaultResource: Boolean = true) : super(profile, loadDefaultResource)

    constructor(loadDefaultResource: Boolean): this("", loadDefaultResource)

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
        /** Constant `EMPTY`  */
        val EMPTY = ImmutableConfig()

        /** Constant `DEFAULT`  */
        val UNSAFE = ImmutableConfig()

        /** Constant `DEFAULT`  */
        val DEFAULT = ImmutableConfig(loadDefaultResource = true)
    }
}
