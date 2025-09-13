package ai.platon.pulsar.persist.metadata;

/**
 * Created by vincent on 17-3-19.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
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
     * fetch
     */
    HREF("F_HREF"),
    LOCATION("F_LOCATION"),
    FETCH_MODE("F_MD"),
    FETCH_TIME_HISTORY("F_FTH"),
    FETCH_MAX_RETRY("F_MR"),
    RESPONSE_TIME("F_RT"),

    FORCE_FOLLOW("PFF"),
    REPARSE_LINKS("PRL"),
    PARSE_NO_LINK_FILTER("PNF"),
    PARSE_LINK_FILTER_DEBUG_LEVEL("PLFDL"),
    TOTAL_OUT_LINKS("PTOL"),
    ORIGINAL_CONTENT_LENGTH("POCL"),

    /**
     * export
     */
    ORIGINAL_EXPORT_PATH("S_OEP");

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
