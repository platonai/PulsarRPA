package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.boilerpipe.utils.Scent;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;

@UDFGroup(namespace = "NEWS")
public class NewsFunctions {

    @UDFunction
    @Nonnull
    public static String getPageTitle(ResultSet rs) {
        return get(rs, Scent.DOC_FIELD_PAGE_TITLE);
    }

    @UDFunction
    @Nonnull
    public static String getContentTitle(ResultSet rs) {
        return get(rs, Scent.DOC_FIELD_CONTENT_TITLE);
    }

    @UDFunction
    @Nonnull
    public static String getHtmlContent(ResultSet rs) {
        return get(rs, Scent.DOC_FIELD_HTML_CONTENT);
    }

    @UDFunction
    @Nonnull
    public static String getTextContent(ResultSet rs) {
        return get(rs, Scent.DOC_FIELD_TEXT_CONTENT);
    }

    @UDFunction
    @Nonnull
    public static String getPublishTime(ResultSet rs) {
        return get(rs, Scent.DOC_FIELD_PUBLISH_TIME);
    }

    @UDFunction
    @Nonnull
    public static String getModifiedTime(ResultSet rs) {
        return get(rs, Scent.DOC_FIELD_MODIFIED_TIME);
    }

    @UDFunction
    @Nonnull
    public static String get(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString());
        }
    }
}
