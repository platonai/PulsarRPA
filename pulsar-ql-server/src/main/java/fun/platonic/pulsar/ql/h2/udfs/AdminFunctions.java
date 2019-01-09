package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.common.PulsarFiles;
import fun.platonic.pulsar.common.proxy.ProxyPool;
import fun.platonic.pulsar.persist.WebPage;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@SuppressWarnings("unused")
@UDFGroup(namespace = "ADMIN")
public class AdminFunctions {
    public static final Logger LOG = LoggerFactory.getLogger(AdminFunctions.class);

    private static QueryEngine engine = QueryEngine.getInstance();
    private static PulsarFiles fs = new PulsarFiles();

    @UDFunction(deterministic = true)
    public static String echo(@H2Context Session h2session, String message) {
        return message;
    }

    @UDFunction(deterministic = true)
    public static String echo(@H2Context Session h2session, String message, String message2) {
        return message + ", " + message2;
    }

    @UDFunction
    public static void print(String message) {
        System.out.println(message);
    }

    @UDFunction
    public static String closeSession(@H2Context Session h2session) {
        checkPrivilege(h2session);

        LOG.info("About to close h2session {}", h2session);
        h2session.close();
        return h2session.toString();
    }

    @UDFunction
    public static String save(@H2Context Session h2session, String url) {
        return save(h2session, url, ".htm");
    }

    @UDFunction
    public static String save(@H2Context Session h2session, String url, String suffix) {
        checkPrivilege(h2session);

        QuerySession session = engine.getSession(new DbSession(h2session));
        WebPage page = session.load(url);
        Path path = fs.get("cache",  "web", fs.fromUri(page.getUrl(), ".htm"));
        return fs.saveTo(page, path);
    }

    @UDFunction
    public static String testProxy(String ipPort) {
        ProxyPool proxyPool = ProxyPool.getInstance(engine.getConf());
        return proxyPool.toString();
    }

    private static void checkPrivilege(Session h2session) {

    }
}
