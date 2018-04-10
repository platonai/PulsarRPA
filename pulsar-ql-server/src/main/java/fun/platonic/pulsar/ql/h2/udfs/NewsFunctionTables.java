package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.ql.types.ValueDom;
import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.extractors.ChineseNewsExtractor;
import fun.platonic.pulsar.boilerpipe.sax.SAXInput;
import fun.platonic.pulsar.boilerpipe.utils.ProcessingException;
import org.h2.tools.SimpleResultSet;
import org.jsoup.nodes.Element;

import javax.annotation.Nonnull;
import java.sql.ResultSet;

import static fun.platonic.pulsar.boilerpipe.utils.Scent.*;

@UDFGroup(namespace = "NEWST")
public class NewsFunctionTables {

    @UDFunction
    @Nonnull
    public static ResultSet extract(ValueDom dom) {
        SimpleResultSet rs = new SimpleResultSet();

        rs.addColumn(DOC_FIELD_CONTENT_TITLE.toUpperCase());
        rs.addColumn(DOC_FIELD_PAGE_TITLE.toUpperCase());
        rs.addColumn(DOC_FIELD_HTML_CONTENT.toUpperCase());
        rs.addColumn(DOC_FIELD_TEXT_CONTENT.toUpperCase());

        rs.addColumn(DOC_FIELD_PAGE_CATEGORY.toUpperCase());
        rs.addColumn(DOC_FIELD_PUBLISH_TIME.toUpperCase());
        rs.addColumn(DOC_FIELD_MODIFIED_TIME.toUpperCase());

        Element ele = dom.getElement();
        try {
            TextDocument doc = new SAXInput().parse(ele.baseUri(), ele.outerHtml());
            ChineseNewsExtractor extractor = new ChineseNewsExtractor();
            extractor.process(doc);
            rs.addRow(
                    doc.getContentTitle(),
                    doc.getPageTitle(),
                    doc.getHtmlContent(),
                    doc.getTextContent(),
                    doc.getPageCategoryAsString(),
                    doc.getPublishTime(),
                    doc.getModifiedTime()
            );
        } catch (ProcessingException e) {
            e.printStackTrace();
        }

        return rs;
    }
}
