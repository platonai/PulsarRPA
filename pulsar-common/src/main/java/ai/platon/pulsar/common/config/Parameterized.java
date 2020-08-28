package ai.platon.pulsar.common.config;

/**
 * Created by vincent on 17-1-8.
 *
 * @author vincent
 * @version $Id: $Id
 */
public interface Parameterized {
    /**
     * <p>getParams.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    default Params getParams() {
        return Params.EMPTY_PARAMS;
    }
}
