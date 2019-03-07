package ai.platon.pulsar.ql.start;

import ai.platon.pulsar.common.PulsarEnv;
import org.h2.tools.Console;

import java.sql.SQLException;

/**
 * Server start port
 * */
public class H2DbConsole {

    public static void main(String[] args) throws SQLException {
        System.setProperty("h2.sessionFactory", ai.platon.pulsar.ql.h2.H2SessionFactory.class.getName());

        PulsarEnv.INSTANCE.ensureEnv();

        new Console().runTool(args);
    }
}
