package fun.platonic.pulsar.ql.utils;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.WebPageFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.classification.InterfaceStability;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.tools.SimpleResultSet;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueString;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QlUtils {

    public static QueryEngine engine = QueryEngine.getInstance();

    public static QuerySession getSession(org.h2.engine.Session h2session) {
        return engine.getSession(new DbSession(h2session));
    }

    @InterfaceStability.Evolving
    public static Collection<WebPage> loadAll(QuerySession session, Value configuredPortal) {
        List<WebPage> pages = new ArrayList<>();

        if (configuredPortal instanceof ValueString) {
            pages.add(session.load(configuredPortal.getString()));
        } else if (configuredPortal instanceof ValueArray) {
            for (Value configuredUrl : ((ValueArray) configuredPortal).getList()) {
                pages.add(session.load(configuredUrl.getString()));
            }
        } else {
            throw DbException.get(ErrorCode.FUNCTION_NOT_FOUND_1, "Unknown custom type");
        }

        return pages;
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
