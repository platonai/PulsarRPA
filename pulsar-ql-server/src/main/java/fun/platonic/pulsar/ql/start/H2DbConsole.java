package fun.platonic.pulsar.ql.start;

import org.h2.tools.Console;

import java.sql.SQLException;

/**
 * Server start port
 * */
public class H2DbConsole {

    public static void main(String[] args) throws SQLException {
        System.setProperty("h2.sessionFactory", fun.platonic.pulsar.ql.h2.H2QueryEngine.class.getName());

        new Console().runTool(args);
    }
}
