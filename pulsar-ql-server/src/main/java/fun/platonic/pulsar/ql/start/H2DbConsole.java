package fun.platonic.pulsar.ql.start;

import org.h2.tools.Console;

import java.sql.SQLException;

import static fun.platonic.pulsar.common.config.CapabilityTypes.*;
import static fun.platonic.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION;

/**
 * Server start port
 * */
public class H2DbConsole {

    public static void main(String[] args) throws SQLException {
        // TODO: use config file
        System.setProperty("h2.sessionFactory", fun.platonic.pulsar.ql.h2.H2QueryEngine.class.getName());

        if (System.getProperty(PULSAR_CONFIG_PREFERRED_DIR) == null) {
            System.setProperty(PULSAR_CONFIG_PREFERRED_DIR, "pulsar-conf");
        }
        if (System.getProperty(PULSAR_CONFIG_RESOURCES) == null) {
            System.setProperty(PULSAR_CONFIG_RESOURCES, "pulsar-default.xml,pulsar-site.xml");
        }
        if (System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION) == null) {
            System.setProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION);
        }

        new Console().runTool(args);
    }
}
