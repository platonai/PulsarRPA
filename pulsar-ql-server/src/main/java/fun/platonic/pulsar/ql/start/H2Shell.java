package fun.platonic.pulsar.ql.start;

import com.google.common.collect.Lists;
import fun.platonic.pulsar.ql.H2Config;
import org.h2.tools.Shell;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.common.config.CapabilityTypes.APPLICATION_CONTEXT_CONFIG_LOCATION;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR;
import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_RESOURCES;
import static fun.platonic.pulsar.common.config.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION;

public class H2Shell extends Shell {
    public H2Shell() {
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
    }
    /**
     * Run the shell tool with the given command line settings.
     *
     * @param args the command line settings
     */
    @Override
    public void runTool(String... args) throws SQLException {
        H2Config.config();

        String url = "jdbc:h2:tcp://localhost/~/test";
        String user = "sa";
        String password = "sa";
        String driver = "org.h2.Driver";
        List<String> options = new LinkedList<>();
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-url")) {
                url = args[++i];
            } else if (arg.equals("-user")) {
                user = args[++i];
            } else if (arg.equals("-password")) {
                password = args[++i];
            } else if (arg.equals("-driver")) {
                driver = args[++i];
            } else {
                options.add(arg);
            }
        }

        List<String> l = Lists.newArrayList("-url", url, "-user", user, "-password", password, "-driver", driver);
        options.addAll(0, l);
        System.out.println(options.stream().collect(Collectors.joining("\n")));

        super.runTool(options.toArray(new String[0]));
    }

    public static void main(String... args) throws SQLException {
        new H2Shell().runTool(args);
    }
}
