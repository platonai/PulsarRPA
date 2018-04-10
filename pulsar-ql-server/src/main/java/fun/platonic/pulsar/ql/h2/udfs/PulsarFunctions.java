package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.utils.QlUtils;
import fun.platonic.pulsar.common.options.LoadOptions;
import fun.platonic.pulsar.persist.WebPage;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;

@UDFGroup(namespace = "PUL")
public class PulsarFunctions {
    public static final Logger LOG = LoggerFactory.getLogger(PulsarFunctions.class);

    private static QueryEngine engine = QueryEngine.getInstance();

    public static void inject(@H2Context Session h2session, String configuredUrl) {
        engine.getSession(new DbSession(h2session)).inject(configuredUrl);
    }

    public static ResultSet load(@H2Context Session h2session, String url) {
        WebPage page = engine.getSession(new DbSession(h2session)).load(url);
        return QlUtils.toResultSet(page);
    }

    public static ResultSet fetch(@H2Context Session h2session, String url) {
        WebPage page = engine.getSession(new DbSession(h2session)).load(url, LoadOptions.parse("--expires=0s"));
        return QlUtils.toResultSet(page);
    }

    public static void parse(String url) {
        // InjectComponent.inject(url);
    }

    public static void update(String url) {
        // InjectComponent.inject(url);
    }

    public static void index(String url) {
        // InjectComponent.inject(url);
    }

    public static void getIndex(String url) {
        // InjectComponent.inject(url);
    }

    public static void getSolrIndex(String url) {
        // InjectComponent.inject(url);
    }
}
