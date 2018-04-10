package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.WebPageFormatter;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;

@SuppressWarnings("unused")
@UDFGroup(namespace = "META")
public class MetadataFunctions {

    private static QueryEngine engine = QueryEngine.getInstance();

    @UDFunction
    public static String get(@H2Context Session h2session, String url) {
        WebPage page = engine.getSession(new DbSession(h2session)).load(url);
        return new WebPageFormatter(page).toString();
    }
}
