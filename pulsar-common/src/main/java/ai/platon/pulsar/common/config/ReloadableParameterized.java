package ai.platon.pulsar.common.config;

/**
 * Created by vincent on 17-1-8.
 */
public interface ReloadableParameterized extends Parameterized {
    ImmutableConfig getConf();

    default void reload(ImmutableConfig conf) {
    }

    default void reload(Params params) {
    }
}
