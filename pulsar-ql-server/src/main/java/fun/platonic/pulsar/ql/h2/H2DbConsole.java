package fun.platonic.pulsar.ql.h2;

import fun.platonic.pulsar.ql.QueryEngine;
import org.h2.tools.Console;

import java.sql.SQLException;

import static fun.platonic.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION;
import static fun.platonic.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;

public class H2DbConsole {

    public static void main(String[] args) throws SQLException {
        // TODO: use config file
        System.setProperty("h2.sessionFactory", QueryEngine.class.getName());
        System.setProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION);

        new Console().runTool(args);
    }
}
