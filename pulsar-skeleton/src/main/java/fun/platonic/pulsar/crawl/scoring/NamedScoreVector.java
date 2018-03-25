package fun.platonic.pulsar.crawl.scoring;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class NamedScoreVector extends ScoreVector {

    public static final ScoreEntry PRIORITY = createScoreEntry(Name.priority);
    public static final ScoreEntry DISTANCE = createScoreEntry(Name.distance);
    public static final ScoreEntry CREATE_TIME = createScoreEntry(Name.createTime);
    public static final ScoreEntry CONTENT_SCORE = createScoreEntry(Name.contentScore);
    public static final ScoreEntry WEB_GRAPH_SCORE = createScoreEntry(Name.webGraphScore);
    public static final ScoreEntry REF_FETCH_ERROR_DENSITY = createScoreEntry(Name.refFetchErrDensity);
    public static final ScoreEntry REF_PARSE_ERROR_DENSITY = createScoreEntry(Name.refParseErrDensity);
    public static final ScoreEntry REF_EXTRACT_ERROR_DENSITY = createScoreEntry(Name.refExtractErrDensity);
    public static final ScoreEntry REF_INDEX_ERROR_DENSITY = createScoreEntry(Name.refIndexErrDensity);
    public static final ScoreEntry MODIFY_TIME = createScoreEntry(Name.modifyTime);
    public static final ScoreEntry INLINK_ORDER = createScoreEntry(Name.inlinkOrder);
    public static final ScoreEntry[] SCORE_ENTRIES = {
            PRIORITY,
            DISTANCE,
            CREATE_TIME,

            CONTENT_SCORE,
            WEB_GRAPH_SCORE,

            REF_FETCH_ERROR_DENSITY,
            REF_PARSE_ERROR_DENSITY,
            REF_EXTRACT_ERROR_DENSITY,
            REF_INDEX_ERROR_DENSITY,

            MODIFY_TIME,
            INLINK_ORDER,
    };

    public NamedScoreVector() {
        super(SCORE_ENTRIES.length, Stream.of(SCORE_ENTRIES).map(ScoreEntry::clone)
                .sorted((e, e2) -> e.getPriority() - e2.getPriority())
                .collect(Collectors.toList()));
    }

    public NamedScoreVector(int... values) {
        this();
        setValue(values);
    }

    /**
     * Enum ordinal is the priority in reversed order
     */
    public static ScoreEntry createScoreEntry(Name name) {
        return new ScoreEntry(name.name(), name.ordinal(), 0, ScoreEntry.DEFAULT_DIGITS);
    }

    public static ScoreEntry createScoreEntry(Name name, int value, int digits) {
        return new ScoreEntry(name.name(), name.ordinal(), value, digits);
    }

    public ScoreEntry get(Name name) {
        return get(name.ordinal());
    }

    public void setValue(Name name, int value) {
        get(name).setValue(value);
    }

    public void setValue(Name name, long value) {
        get(name).setValue((int) value);
    }

    public void setValue(Name name, float value) {
        setValue(name, (int) value);
    }

    /**
     * All available parameters to calculate a page's score, eg, score to evaluate the most important N pages.
     * We use enum ordinal as the priority for simplification (smaller ordinal means higher priority).
     * To change one parameter's priority, just change it's order in the following enum definition.
     */
    public enum Name {
        priority,      // bigger,  better
        distance,      // smaller, better
        createTime,    // bigger, better, limited

        contentScore, // bigger, better
        webGraphScore, // bigger, better

        refFetchErrDensity,  // smaller, better
        refParseErrDensity,  // smaller, better
        refExtractErrDensity,// smaller, better
        refIndexErrDensity,  // smaller, better

        modifyTime,    // bigger, better
        inlinkOrder,  // smaller, better
    }
}
