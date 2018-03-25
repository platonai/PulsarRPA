package fun.platonic.pulsar.parse.html;

import fun.platonic.pulsar.common.MetricsCounters;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.options.EntityOptions;
import fun.platonic.pulsar.crawl.parse.ParseFilter;
import fun.platonic.pulsar.crawl.parse.ParseResult;
import fun.platonic.pulsar.crawl.parse.html.FieldCollection;
import fun.platonic.pulsar.crawl.parse.html.JsoupParser;
import fun.platonic.pulsar.crawl.parse.html.ParseContext;
import fun.platonic.pulsar.persist.*;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

/**
 * Created by vincent on 16-9-14.
 * <p>
 * Parse Web page using Jsoup if and only if WebPage.query is specified
 * <p>
 * Selector filter, Css selector, XPath selector and Scent selectors are supported
 */
public class GeneralHtmlFilter implements ParseFilter {

    public enum Counter {jsoupFailure, noEntity, brokenEntity, brokenSubEntity}

    static {
        MetricsCounters.register(Counter.class);
    }

    private ImmutableConfig conf;
    private MetricsCounters metricsCounters;

    public GeneralHtmlFilter() {
        metricsCounters = new MetricsCounters();
    }

    public GeneralHtmlFilter(ImmutableConfig conf) {
        this.metricsCounters = new MetricsCounters();
        reload(conf);
    }

    public GeneralHtmlFilter(MetricsCounters metricsCounters, ImmutableConfig conf) {
        this.metricsCounters = metricsCounters;
        reload(conf);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
    }

    public void filter(WebPage page, ParseResult parseResult) throws IOException {
        filter(new ParseContext(page, null, null, parseResult));
    }

    /**
     * Extract all fields in the page
     */
    @Override
    public void filter(ParseContext parseContext) throws IOException {
        WebPage page = parseContext.getPage();
        ParseResult parseResult = parseContext.getParseResult();

        parseResult.setMajorCode(ParseStatus.SUCCESS);

        String query = page.getQuery();
        if (query == null) {
            query = page.getOptions().toString();
        }

        EntityOptions options = EntityOptions.parse(query);
        if (!options.hasRules()) {
            parseResult.setMinorCode(ParseStatus.SUCCESS_EXT);
            return;
        }

        JsoupParser parser = new JsoupParser(page, conf);
        Document document = parser.parse();
        parseResult.setDocument(document);

        List<FieldCollection> fieldCollections = parser.extractAll(options);
        if (fieldCollections.isEmpty()) {
            return;
        }

        // All last extracted fields are cleared, so we just keep the last extracted fields
        // TODO: How to save updated comments?
        // We only save comments extracted from the current page,
        // Comments appears in sub pages can not be read in this WebPage,
        // they may be crawled as separated WebPages
        PageModel pageModel = page.getPageModel();

        FieldCollection fieldCollection = fieldCollections.get(0);
        FieldGroup majorGroup = pageModel.emplace(1, 0, "selector", fieldCollection);
        int loss = fieldCollection.getLoss();
        page.getPageCounters().set(PageCounters.Self.missingFields, loss);
        metricsCounters.increase(Counter.brokenEntity, loss > 0 ? 1 : 0);

        int brokenSubEntity = 0;
        for (int i = 1; i < fieldCollections.size(); i++) {
            fieldCollection = fieldCollections.get(i);
            pageModel.emplace(10000 + i, majorGroup.getId(), "selector-sub", fieldCollection);
            loss = fieldCollection.getLoss();
            if (loss > 0) {
                ++brokenSubEntity;
            }
        }

        page.getPageCounters().set(PageCounters.Self.brokenSubEntity, brokenSubEntity);
        metricsCounters.increase(Counter.brokenSubEntity, brokenSubEntity);
    }

    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

}
