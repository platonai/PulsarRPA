package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.common.options.LoadOptions;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.ql.types.ValueDom;
import fun.platonic.pulsar.ql.utils.QlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.hadoop.classification.InterfaceStability;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;
import org.h2.tools.SimpleResultSet;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@UDFGroup(namespace = "DOMT")
public class DomFunctionTables {

    public static QueryEngine engine = QueryEngine.getInstance();

    @InterfaceStability.Stable
    @UDFunction
    public static ResultSet loadAndSelect(@H2Context Session h2session, String configuredUrl, String cssQuery) {
        return loadAndSelect(h2session, configuredUrl, cssQuery, 0, Integer.MAX_VALUE);
    }

    @InterfaceStability.Stable
    @UDFunction
    public static ResultSet loadAndSelect(
            @H2Context Session h2session, String configuredUrl, String cssQuery, int offset, int limit) {
        QuerySession session = QlUtils.getSession(h2session);

        Collection<ValueDom> doms = session.parse(session.load(configuredUrl)).select(cssQuery).stream()
                .skip(Math.max(0, offset))
                .limit(Math.max(0, limit))
                .map(ValueDom::get)
                .collect(Collectors.toList());
        return QlUtils.toResultSet("DOM", doms);
    }

    @InterfaceStability.Stable
    @UDFunction
    @Nonnull
    public static ResultSet select(ValueDom dom, String cssQuery) {
        Objects.requireNonNull(dom);
        return select(dom, cssQuery, 0, Integer.MAX_VALUE);
    }

    @InterfaceStability.Stable
    @UDFunction
    @Nonnull
    public static ResultSet select(ValueDom dom, String cssQuery, int offset, int limit) {
        Collection<ValueDom> doms = dom.getElement().select(cssQuery).stream()
                .skip(Math.max(0, offset))
                .limit(Math.max(0, limit))
                .map(ValueDom::get)
                .collect(Collectors.toList());
        return QlUtils.toResultSet("DOM", doms);
    }
}
