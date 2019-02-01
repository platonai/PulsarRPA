package ai.platon.pulsar.common.config;

/**
 * Created by vincent on 17-1-8.
 */
public interface Parameterized {
    default Params getParams() {
        return Params.EMPTY_PARAMS;
    }
}
