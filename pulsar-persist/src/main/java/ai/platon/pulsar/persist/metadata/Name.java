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
    FETCH_MAX_RETRY("F_MR"),
    REDIRECT_DISCOVERED("F_RD"),
    RESPONSE_TIME("F_RT"),
    @Deprecated
    BROWSER("F_BR"),
    @Deprecated
    IS_RESOURCE("F_RES"),
    @Deprecated
    HTML_INTEGRITY("F_HI"),
    ACTIVE_DOM_MULTI_STATUS("F_ADMS"),
    ACTIVE_DOM_URLS("F_ADU"),
    @Deprecated
    PROXY("F_PX"),
    FETCHED_LINK_COUNT("FFLC"),

    //////////////////////////
    // parse section
    //////////////////////////
    /**
     * Embedded query is not used any more, use SQL instead
     * */
    NO_FOLLOW("PNF"),
    FORCE_FOLLOW("PFF"),
    REPARSE_LINKS("PRL"),
    PARSE_NO_LINK_FILTER("PNF"),
    PARSE_LINK_FILTER_DEBUG_LEVEL("PLFDL"),
    TOTAL_OUT_LINKS("PTOL"),
    ANCHORS("P_AS"),

    MODEL_SYNC_TIME("PMST"),

    /**
     * harvest
     */
    HARVEST_STATUS("HS"),

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
    CASH_KEY("SCASH"),

    /** update */

    /**
     * content
     */
    @Deprecated
    CHAR_ENCODING_FOR_CONVERSION("CCEFC"),
    ENCODING_CLUES("C_EC"),
    @Deprecated
    CONTENT_BYTES("C_CB"),
    @Deprecated
    LAST_CONTENT_BYTES("C_LCB"),
    @Deprecated
    PERSIST_CONTENT_BYTES("CPCB"),
    @Deprecated
    AVE_CONTENT_BYTES("C_ACB"),

    META_KEYWORDS("meta_keywords"),
    META_DESCRIPTION("meta_description"),

    /**
     * tmp
     */
    TMP_PAGE_FROM_SEED("T_PFS"),
    TMP_IS_DETAIL("T_ID"),
    TMP_CHARS("T_C");

    private final String text;

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
