package ai.platon.pulsar.boilerpipe.extractors;

import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.heuristics.*;
import ai.platon.pulsar.boilerpipe.filters.simple.BoilerplateBlockFilter;
import ai.platon.pulsar.boilerpipe.filters.simple.LabeledFieldExtractorFilter;
import ai.platon.pulsar.boilerpipe.filters.simple.RegexFieldExtractorFilter;
import ai.platon.pulsar.boilerpipe.filters.statistics.NumWordsRulesClassifier;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import ai.platon.pulsar.common.DateTimes;
import com.google.common.collect.ListMultimap;

import java.time.ZoneId;
import java.util.Set;

import static ai.platon.pulsar.boilerpipe.utils.Scent.*;

/**
 * A full-text extractor which is tuned towards news articles. In this scenario it achieves higher
 * accuracy than {@link DefaultExtractor}.
 */
public final class ChineseNewsExtractor implements TextExtractor {
    public static final ChineseNewsExtractor INSTANCE = new ChineseNewsExtractor();

    private ZoneId zoneId = ZoneId.systemDefault();
    private ListMultimap<String, String> labeledFieldRules;
    private ListMultimap<String, String> regexFieldRules;
    private Set<String> terminatingBlocksContains;
    private Set<String> terminatingBlocksStartsWith;

    public ChineseNewsExtractor() {
        this.labeledFieldRules = LABELED_FIELD_RULES;
        this.regexFieldRules = REGEX_FIELD_RULES;
        this.terminatingBlocksContains = TERMINATING_BLOCKS_CONTAINS;
        this.terminatingBlocksStartsWith = TERMINATING_BLOCKS_STARTS_WITH;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void setLabeledFieldRules(ListMultimap<String, String> labeledFieldRules) {
        this.labeledFieldRules.putAll(labeledFieldRules);
    }

    public void setRegexFieldRules(ListMultimap<String, String> regexFieldRules) {
        this.regexFieldRules.putAll(regexFieldRules);
    }

    public void setTerminatingBlocksContains(Set<String> terminatingBlocksContains) {
        this.terminatingBlocksContains.addAll(terminatingBlocksContains);
    }

    public void setTerminatingBlocksStartsWith(Set<String> terminatingBlocksStartsWith) {
        this.terminatingBlocksStartsWith.addAll(terminatingBlocksStartsWith);
    }

    /**
     * Returns the singleton instance for {@link ChineseNewsExtractor}.
     */
    public static ChineseNewsExtractor getInstance() {
        return INSTANCE;
    }

    public boolean process(TextDocument doc) throws ProcessingException {

        new TerminatingBlocksFinder(terminatingBlocksContains, terminatingBlocksStartsWith).process(doc);
        new DocumentTitleMatchClassifier(doc.getPageTitle()).process(doc);
        NumWordsRulesClassifier.INSTANCE.process(doc);
        IgnoreBlocksAfterContentFilter.DEFAULT_INSTANCE.process(doc);
        IgnoreBlocksAfterContentFromEndFilter.INSTANCE.process(doc);
        TrailingHeadlineToBoilerplateFilter.INSTANCE.process(doc);
        BlockProximityFusion.MAX_DISTANCE_1.process(doc);
        new ArticleMetadataFilter(zoneId).process(doc);
        BoilerplateBlockFilter.INSTANCE_KEEP_TITLE.process(doc);
        BlockProximityFusion.MAX_DISTANCE_1_CONTENT_ONLY_SAME_TAGLEVEL.process(doc);
        KeepLargestBlockFilter.INSTANCE_EXPAND_TO_SAME_TAGLEVEL_MIN_WORDS.process(doc);
        ExpandTitleToContentFilter.INSTANCE.process(doc);
        LargeBlockSameTagLevelToContentFilter.INSTANCE.process(doc);
        ListAtEndFilter.INSTANCE.process(doc);
        ContentDateStringNumberFilter.INSTANCE.process(doc);
        new RegexFieldExtractorFilter(regexFieldRules, MAX_META_STR_LENGTH).process(doc);
        new LabeledFieldExtractorFilter(labeledFieldRules).process(doc);

        doc.setContentTitle(doc.getFieldOrDefault(DOC_FIELD_CONTENT_TITLE, ""));

        doc.setField(DOC_FIELD_TEXT_CONTENT_LENGTH, String.valueOf(doc.getTextContent().length()));
        doc.setField(DOC_FIELD_HTML_CONTENT_LENGTH, String.valueOf(doc.getHtmlContent().length()));
        doc.setField(DOC_FIELD_PUBLISH_TIME, DateTimes.isoInstantFormat(doc.getPublishTime()));
        doc.setField(DOC_FIELD_MODIFIED_TIME, DateTimes.isoInstantFormat(doc.getModifiedTime()));
        doc.setField(DOC_FIELD_PAGE_CATEGORY, doc.getPageCategoryAsString());
        doc.setField(DOC_FIELD_PAGE_TITLE, doc.getPageTitle());
        doc.setField(DOC_FIELD_CONTENT_TITLE, doc.getContentTitle());
        doc.setField(DOC_FIELD_HTML_CONTENT, doc.getHtmlContent());
        doc.setField(DOC_FIELD_TEXT_CONTENT, doc.getTextContent());

        return true;
    }
}
