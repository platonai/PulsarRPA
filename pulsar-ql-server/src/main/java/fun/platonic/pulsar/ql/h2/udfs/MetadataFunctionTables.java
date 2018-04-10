package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.options.LoadOptions;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.ql.utils.QlUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;

import java.sql.ResultSet;
import java.time.Duration;

@SuppressWarnings("unused")
@UDFGroup(namespace = "METAT")
public class MetadataFunctionTables {

    private static QueryEngine engine = QueryEngine.getInstance();

    @UDFunction
    public static ResultSet load(@H2Context Session h2session, String configuredUrl) {
        WebPage page = engine.getSession(new DbSession(h2session)).load(configuredUrl);
        return QlUtils.toResultSet(page);
    }

    @UDFunction
    public static ResultSet fetch(@H2Context Session h2session, String configuredUrl) {
        Pair<String, String> urlAndArgs = UrlUtil.splitUrlArgs(configuredUrl);
        LoadOptions loadOptions = LoadOptions.parse(urlAndArgs.getValue());
        loadOptions.setExpires(Duration.ZERO);

        WebPage page = engine.getSession(new DbSession(h2session)).load(urlAndArgs.getKey(), loadOptions);
        return QlUtils.toResultSet(page);
    }

    @UDFunction
    public static ResultSet parse(@H2Context Session h2session, String configuredUrl) {
        Pair<String, String> urlAndArgs = UrlUtil.splitUrlArgs(configuredUrl);
        LoadOptions loadOptions = LoadOptions.parse(urlAndArgs.getValue());
        loadOptions.setParse(true);

        QuerySession session = engine.getSession(new DbSession(h2session));
        WebPage page = session.load(urlAndArgs.getKey(), loadOptions);
        return QlUtils.toResultSet(page);
    }
}
