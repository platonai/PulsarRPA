package ai.platon.pulsar.persist.metadata;

/**
 * Created by vincent on 17-3-20.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public interface CrawlVariables {
    /** Constant <code>UNKNOWN=""</code> */
    String UNKNOWN = "";

    /**
     * inject
     */
    String SEED_OPTS = "S_OPTS";

    /** Constant <code>IS_NAVIGATOR="I_N"</code> */
    String IS_NAVIGATOR = "I_N";
    /** Constant <code>IS_SEED="I_S"</code> */
    String IS_SEED = "I_S";
    /** Constant <code>IS_DETAIL="I_D"</code> */
    String IS_DETAIL = "I_D";

    /**
     * generate
     */
    String GENERATE_TIME = "G_GT";
    /** Constant <code>MAX_DISTANCE="G_MD"</code> */
    String MAX_DISTANCE = "G_MD";

    /**
     * fetch
     */
    String FETCH_TIME_HISTORY = "F_FTH";
    /** Constant <code>REDIRECT_DISCOVERED="F_RD"</code> */
    String REDIRECT_DISCOVERED = "F_RD";
    /** Constant <code>RESPONSE_TIME="F_RT"</code> */
    String RESPONSE_TIME = "F_RT";

    /**
     * parse
     */
    String TOTAL_OUT_LINKS = "P_OLC";
    /** Constant <code>ANCHOR_ORDER="P_AO"</code> */
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
    /** Constant <code>CHAR_ENCODING_FOR_CONVERSION="C_CEFC"</code> */
    String CHAR_ENCODING_FOR_CONVERSION = "C_CEFC";

    /** Constant <code>META_KEYWORDS="meta_keywords"</code> */
    String META_KEYWORDS = "meta_keywords";
    /** Constant <code>META_DESCRIPTION="meta_description"</code> */
    String META_DESCRIPTION = "meta_description";

    /**
     * tmp
     */
    String TMP_PAGE_FROM_SEED = "T_PFS";
    /** Constant <code>TMP_IS_DETAIL="T_ID"</code> */
    String TMP_IS_DETAIL = "T_ID";
    /** Constant <code>TMP_CHARS="T_C"</code> */
    String TMP_CHARS = "T_C";
}
