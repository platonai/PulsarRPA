package ai.platon.pulsar.persist.metadata;

/**
 * Created by vincent on 17-3-19.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * All valid Metadata names are listed here.
 * A metadata field can be moved to be a WebPage field if it's stable
 *
 * @author vincent
 * @version $Id: $Id
 */
public enum Name {
    UNKNOWN(""),

    /**
     * common
     */
    LINK_FILTER_OPTS("LFOPTS"),

    IS_NAVIGATOR("I_N"),
    IS_SEED("I_S"),
    IS_PORTAL("I_P"),
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
    HREF("F_HREF"),
    FETCH_MODE("F_MD"),
    FETCH_AHEAD_TIME("F_EFT"),
    FETCH_TIME_HISTORY("F_FTH"),
    REDIRECT_DISCOVERED("F_RD"),
    RESPONSE_TIME("F_RT"),
    BROWSER("F_BR"),
    HTML_INTEGRITY("F_HI"),
    ACTIVE_DOM_MULTI_STATUS("F_ADMS"),
    ACTIVE_DOM_URLS("F_ADU"),
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

    MODEL_SYNC_TIME("PST"),

    /**
     * harvest
     */
    HARVEST_STATUS("H_S"),

    /**
     * export
     */
    ORIGINAL_EXPORT_PATH("S_OEP"),
    SCREENSHOT_EXPORT_PATH("S_SSEP"),

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
    LAST_CONTENT_BYTES("C_LCB"),
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

    /**
     * <p>text.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String text() {
        return this.text;
    }

}
