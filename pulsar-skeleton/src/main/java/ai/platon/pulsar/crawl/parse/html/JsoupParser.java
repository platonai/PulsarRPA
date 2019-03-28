package ai.platon.pulsar.crawl.parse.html;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.CollectionOptions;
import ai.platon.pulsar.common.options.EntityOptions;
import ai.platon.pulsar.persist.WebPage;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by vincent on 16-9-14.
 * <p>
 * General parser, Css selector, XPath selector, Regex and Scent selectors are supported
 */
public class JsoupParser extends EntityOptions.Builder {
    public static final Logger LOG = LoggerFactory.getLogger(JsoupParser.class);

    private final ImmutableConfig conf;
    private final WebPage page;
    private boolean ignoreScripts;
    private Document document;
    private List<FieldCollection> entities = new LinkedList<>();

    public JsoupParser(WebPage page, ImmutableConfig conf) {
        this.conf = conf;
        this.page = page;
    }

    /**
     * Apply css selector
     */
    public static void extract(String name, String selector, Element root, FieldCollection fields) {
        boolean required = true;
        if (selector.endsWith("?")) {
            required = false;
            selector = selector.substring(0, selector.length() - 1);
        }
        selector = selector.replaceAll("%", " ");

        String value = getText(selector, root);

        if (required) {
            fields.increaseRequired(1);
            if (value.equals("")) {
                fields.loss(1);
            }
        }

        if (!value.equals("")) {
            fields.put(name, value);
        }
    }

    /**
     *
     * */
    public static String getText(String cssQuery, Element root) {
        Elements elements = query(cssQuery, root);
        if (elements.isEmpty()) {
            return "";
        }

        return StringUtils.strip(elements.first().text());
    }

    /**
     * Select a element set using css selector.
     *
     * @param cssQuery The css selector
     * @param root     The root element
     *                 The root element to query
     * @return An element set who matches the css selector
     */
    public static Elements query(String cssQuery, Element root) {
        if (cssQuery == null || cssQuery.isEmpty()) {
            return new Elements();
        }

        try {
            return root.select(cssQuery);
        } catch (Throwable ignored) {
        }

        return new Elements();
    }

    public Document getDocument() {
        return document;
    }

    @Nonnull
    public Document parse() {
        if (page.getEncoding() == null) {
            PrimerParser primerParser = new PrimerParser(conf);
            primerParser.detectEncoding(page);
        }

        try {
            document = Jsoup.parse(page.getContentAsInputStream(), page.getEncoding(), page.getUrl(), ignoreScripts);
        } catch (IOException e) {
            LOG.warn("Failed to parse page {}" + page.getUrl());
            LOG.warn(e.toString());

            document = Document.createShell(page.getUrl());
        }

        return document;
    }

    public void setIgnoreScripts(boolean ignoreScripts) {
        this.ignoreScripts = ignoreScripts;
    }

    /**
     * Extract all fields using EntityOptions
     */
    public List<FieldCollection> extractAll(EntityOptions options) throws IOException {
        // No rules
        if (!options.hasRules()) {
            return entities;
        }

        FieldCollection fields = extract(options);

        if (!fields.isEmpty()) {
            entities.add(fields);
            extract(options.getCollectionOptions());
        }

        return entities;
    }

    public List<FieldCollection> extractAll() throws IOException {
        return extractAll(build());
    }

    public FieldCollection extract() throws IOException {
        return extract(build());
    }

    /**
     * Parse entity
     */
    public FieldCollection extract(EntityOptions options) {
        FieldCollection fields = new FieldCollection();

        Element root = query(options.getRoot(), document).first();
        if (root == null) {
            return fields;
        }

        fields.setName(options.getName());
        options.getCssRules().forEach((key, value) -> extract(key, value, root, fields));

        return fields;
    }

    /**
     * Parse sub entity collection
     */
    public List<FieldCollection> extract(CollectionOptions rules) {
        // Parse fields for sub entity collection
        Element root = query(rules.getRoot(), document).first();
        if (root == null) {
            return entities;
        }

        // Parse fields for sub entity collection
        Elements elements = query(rules.getItem(), root);

        int id = 0;
        for (Element ele : elements) {
            FieldCollection fields = new FieldCollection();
            fields.setName("sub_" + String.valueOf(++id));
            rules.getCssRules().forEach((key, value) -> extract(key, value, ele, fields));
            entities.add(fields);
        }

        return entities;
    }
}
