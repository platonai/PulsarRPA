package ai.platon.pulsar.persist.metadata;

/**
 * Created by vincent on 17-3-19.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * All valid Metadata names are listed here.
 * A metadata field can be moved to be a WebPage field if it's stable
 */
public enum Name {
    UNKNOWN(""),

    /**
     * common
     */
    LINK_FILTER_OPTS("LFOPTS"),

    IS_NAVIGATOR("I_N"),
    IS_SEED("I_S"),
    IS_TMP_SEED("I_TS"),
    IS_DETAIL("I_D"),

    /**
     * generate
     */
    GENERATE_TIME("G_GT"),
    MAX_DISTANCE("G_MD"),

    /**
     * fetch
     */
    FETCH_MODE("F_MD"),
    FETCH_AHEAD_TIME("F_EFT"),
    FETCH_TIME_HISTORY("F_FTH"),
    REDIRECT_DISCOVERED("F_RD"),
    RESPONSE_TIME("F_RT"),
    BROWSER("F_BR"),
    BROWSER_JS_DATA("F_JD"),
    PROXY("F_PX"),

    /**
     * parse
     */
    QUERY("P_Q"),
    NO_FOLLOW("P_NF"),
    FORCE_FOLLOW("P_FF"),
    REPARSE_LINKS("P_RL"),
    PARSE_NO_LINK_FILTER("P_NF"),
    PARSE_LINK_FILTER_DEBUG_LEVEL("P_LFDL"),
    TOTAL_OUT_LINKS("P_TOL"),
    ANCHORS("P_AS"),

    /**
     * harvest
     */
    HARVEST_STATUS("H_S"),

    /**
     * storage
     */
    ORIGINAL_EXPORT_PATH("S_OEP"),

    /**
     * index
     */
    INDEX_TIME_HISTORY("I_ITH"),

    /**
     * score
     */
    CASH_KEY("S_CASH"),

    /** update */

    /**
     * content
     */
    CHAR_ENCODING_FOR_CONVERSION("C_CEFC"),
    ENCODING_CLUES("C_EC"),
    CONTENT_BYTES("C_CB"),
    AVE_CONTENT_BYTES("C_ACB"),

    META_KEYWORDS("meta_keywords"),
    META_DESCRIPTION("meta_description"),

    /**
     * tmp
     */
    TMP_PAGE_FROM_SEED("T_PFS"),
    TMP_IS_DETAIL("T_ID"),
    TMP_CHARS("T_C");

    private String text;

    Name(String name) {
        this.text = name;
    }

    public String text() {
        return this.text;
    }

}
