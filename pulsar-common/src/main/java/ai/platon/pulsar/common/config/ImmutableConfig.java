package ai.platon.pulsar.common.config;

import org.apache.hadoop.conf.Configuration;

/**
 * <p>ImmutableConfig class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ImmutableConfig extends AbstractConfiguration {

    /** Constant <code>EMPTY</code> */
    public static final ImmutableConfig EMPTY = new ImmutableConfig(false);

    /** Constant <code>DEFAULT</code> */
    public static final ImmutableConfig DEFAULT = new ImmutableConfig(new Configuration());

    /**
     * <p>Constructor for ImmutableConfig.</p>
     */
    public ImmutableConfig() {
    }

    /**
     * <p>Constructor for ImmutableConfig.</p>
     *
     * @param loadDefaultResource a boolean.
     */
    public ImmutableConfig(boolean loadDefaultResource) {
        super(loadDefaultResource);
    }

    /**
     * <p>Constructor for ImmutableConfig.</p>
     *
     * @param profile a {@link java.lang.String} object.
     */
    public ImmutableConfig(String profile) {
        super(profile);
    }

    /**
     * <p>Constructor for ImmutableConfig.</p>
     *
     * @param conf a {@link org.apache.hadoop.conf.Configuration} object.
     */
    public ImmutableConfig(Configuration conf) {
        super(conf);
    }

    /**
     * <p>toMutableConfig.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.MutableConfig} object.
     */
    public MutableConfig toMutableConfig() {
        return new MutableConfig(this);
    }

    /**
     * <p>toVolatileConfig.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.VolatileConfig} object.
     */
    public VolatileConfig toVolatileConfig() {
        return new VolatileConfig(this);
    }
}
