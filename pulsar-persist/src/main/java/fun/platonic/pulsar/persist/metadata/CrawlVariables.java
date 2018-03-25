package fun.platonic.pulsar.persist.metadata;

/**
 * Created by vincent on 17-3-20.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 *
 * @Experiental
 */
public interface CrawlVariables {
    String UNKNOWN = "";

    /**
     * inject
     */
    String SEED_OPTS = "S_OPTS";

    String IS_NAVIGATOR = "I_N";
    String IS_SEED = "I_S";
    String IS_DETAIL = "I_D";

    /**
     * generate
     */
    String GENERATE_TIME = "G_GT";
    String MAX_DISTANCE = "G_MD";

    /**
     * fetch
     */
    String FETCH_TIME_HISTORY = "F_FTH";
    String REDIRECT_DISCOVERED = "F_RD";
    String RESPONSE_TIME = "F_RT";

    /**
     * parse
     */
    String TOTAL_OUT_LINKS = "P_OLC";
    String ANCHOR_ORDER = "P_AO";

    /**
     * index
     */
    String INDEX_TIME_HISTORY = "I_ITH";

    /**
     * score
     */
    String CASH_KEY = "S_CASH";

    /**
     * update
     */
    String ANCHORS = "U_A";

    /**
     * content
     */
    String ORIGINAL_CHAR_ENCODING = "C_OCE";
    String CHAR_ENCODING_FOR_CONVERSION = "C_CEFC";

    String META_KEYWORDS = "meta_keywords";
    String META_DESCRIPTION = "meta_description";

    /**
     * tmp
     */
    String TMP_PAGE_FROM_SEED = "T_PFS";
    String TMP_IS_DETAIL = "T_ID";
    String TMP_CHARS = "T_C";
}
