package ai.platon.pulsar.boilerpipe.filters.heuristics;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.TextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;
import ai.platon.pulsar.boilerpipe.utils.Scent;
import ai.platon.pulsar.common.DateTimeDetector;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

/**
 * Tries to find TextBlocks that comprise of "article metadata".
 */
public class ArticleMetadataFilter implements TextBlockFilter {

    public static final ArticleMetadataFilter INSTANCE = new ArticleMetadataFilter();

    private DateTimeDetector dateTimeDetector = new DateTimeDetector();

    public ArticleMetadataFilter() {
    }

    public ArticleMetadataFilter(ZoneId zoneId) {
        dateTimeDetector.setZoneId(zoneId);
    }

    @Override
    public boolean process(TextDocument doc) throws ProcessingException {
        Instant now = Instant.now();
        Instant publishTime = null;
        int sniffedDateTimeCount = 0;

        boolean changed = false;

        boolean foundLongText = false;
        for (TextBlock tb : doc.getTextBlocks()) {
            final String text = tb.getText();
            if (text.length() > Scent.MAX_META_STR_LENGTH) {
                foundLongText = true;
            }

            // Sniff latest page modified time
            Instant sniffedTime = sniffValidDateTime(text, now);
            if (sniffedTime.isAfter(doc.getModifiedTime())) {
                ++sniffedDateTimeCount;
                doc.setModifiedTime(sniffedTime);
            }

            // sniff publish time
            // TODO : need more features
            if (!foundLongText && text.length() > Scent.MIN_DATE_TIME_STR_LENGTH && publishTime == null) {
                if (sniffedTime.isAfter(doc.getPublishTime())) {
                    publishTime = sniffedTime;

                    doc.setPublishTime(publishTime);

                    tb.setIsContent(true);
                    tb.addLabel(BlockLabels.ARTICLE_METADATA);

                    changed = true;
                }
            }

            if (tb.getNumWords() < 10) {
                for (Pattern p : Scent.PATTERNS_SHORT) {
                    if (p.matcher(text).find()) {
                        changed = true;
                        tb.setIsContent(true);
                        tb.addLabel(BlockLabels.ARTICLE_METADATA);
                    }
                } // for
            }
        }

        doc.setDateTimeCount(sniffedDateTimeCount);

        return changed;
    }

    private Instant sniffValidDateTime(String text, Instant now) {
        OffsetDateTime offsetDateTime = dateTimeDetector.detectDateTimeLeniently(text);
        if (offsetDateTime != null) {
            Instant dateTime = offsetDateTime.toInstant();
            long days = Duration.between(dateTime, now).toDays();
            if (days < 5 * 365) {
                return dateTime;
            }
        }

        return Instant.EPOCH;
    }
}
