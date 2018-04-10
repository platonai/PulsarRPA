package fun.platonic.pulsar.ql.utils;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.WebPageFormatter;
import fun.platonic.pulsar.ql.types.ValueDom;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.classification.InterfaceStability;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.tools.SimpleResultSet;
import org.h2.value.DataType;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueString;
import org.jsoup.nodes.Element;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QlUtils {

    public static QueryEngine engine = QueryEngine.getInstance();

    public static QuerySession getSession(org.h2.engine.Session h2session) {
        return engine.getSession(new DbSession(h2session));
    }

    /**
     * Get result set for each field in Web page
     * */
    public static ResultSet toResultSet(WebPage page) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.addColumn("KEY");
        rs.addColumn("VALUE");

        Map<String, Object> fields = new WebPageFormatter(page).toMap();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString();
            rs.addRow(entry.getKey(), value);
        }

        return rs;
    }

    /**
     * Get a result set, the result set contains just one column DOM
     * TODO: generalization
     * */
    @InterfaceStability.Evolving
    public static <E> ResultSet toResultSet(String colName, Iterable<E> collection) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.setAutoClose(false);
        int colType = colName.equalsIgnoreCase("DOM") ? ValueDom.type : Value.STRING;
        rs.addColumn(colName, DataType.convertTypeToSQLType(colType), 0, 0);

        if (colType == ValueDom.type) {
            collection.forEach(rs::addRow);
        } else {
            collection.forEach(e -> rs.addRow(ValueString.get(e.toString())));
        }

        return rs;
    }

    public static String appendIfMissingIgnoreCase(String cssQuery, String appendix) {
        cssQuery = cssQuery.toLowerCase().replaceAll("\\s+", " ").trim();
        appendix = appendix.trim();

        String[] parts = StringUtils.split(cssQuery, " ");
        if (!parts[parts.length - 1].startsWith(appendix)) {
            cssQuery += " " + appendix;
        }

        return cssQuery;
    }
}
