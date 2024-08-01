package ai.platon.pulsar.ql.common;

import org.h2.engine.SysProperties;
import org.h2.util.JdbcUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom H2 config, must be called on both server and client side
 * */
public class H2Config {
    private static AtomicBoolean configured = new AtomicBoolean();

    public static void config() {
        if (configured.getAndSet(true)) {
            return;
        }

        // From java doc:
        // "On the client side, this setting is required to be disabled"
        SysProperties.serializeJavaObject = false;
        JdbcUtils.serializer = new PulsarObjectSerializer();

        String dataTypeHandler = System.getProperty("h2.customDataTypesHandler");
        if (dataTypeHandler == null) {
            JdbcUtils.customDataTypesHandler = new PulsarDataTypesHandler();
        }
    }
}
