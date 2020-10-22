package ai.platon.pulsar.persist.metadata;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by vincent on 16-12-18.
 * <p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public enum PageCategory {
    INDEX, DETAIL, OFFER_LIST, SELLER, REVIEW, PROFILE, SEARCH, MEDIA, BBS, TIEBA, BLOG, UNKNOWN;

    /**
     * <p>is.</p>
     *
     * @param pageCategory a {@link ai.platon.pulsar.persist.metadata.PageCategory} object.
     * @return a boolean.
     */
    public boolean is(PageCategory pageCategory) {
        return pageCategory == this;
    }

    /**
     * <p>isIndex.</p>
     *
     * @return a boolean.
     */
    public boolean isIndex() {
        return this == INDEX;
    }

    /**
     * <p>isDetail.</p>
     *
     * @return a boolean.
     */
    public boolean isDetail() {
        return this == DETAIL;
    }

    /**
     * <p>isOfferList.</p>
     *
     * @return a boolean.
     */
    public boolean isOfferList() {
        return this == OFFER_LIST;
    }

    /**
     * <p>isSeller.</p>
     *
     * @return a boolean.
     */
    public boolean isSeller() {
        return this == SELLER;
    }

    /**
     * <p>isReview.</p>
     *
     * @return a boolean.
     */
    public boolean isReview() {
        return this == REVIEW;
    }

    /**
     * <p>isProfile.</p>
     *
     * @return a boolean.
     */
    public boolean isProfile() {
        return this == PROFILE;
    }

    /**
     * <p>isSearch.</p>
     *
     * @return a boolean.
     */
    public boolean isSearch() {
        return this == SEARCH;
    }

    /**
     * <p>isMedia.</p>
     *
     * @return a boolean.
     */
    public boolean isMedia() {
        return this == MEDIA;
    }

    /**
     * <p>isBBS.</p>
     *
     * @return a boolean.
     */
    public boolean isBBS() {
        return this == BBS;
    }

    /**
     * <p>isTieBa.</p>
     *
     * @return a boolean.
     */
    public boolean isTieBa() {
        return this == TIEBA;
    }

    /**
     * <p>isBlog.</p>
     *
     * @return a boolean.
     */
    public boolean isBlog() {
        return this == BLOG;
    }

    /**
     * <p>isUnknown.</p>
     *
     * @return a boolean.
     */
    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    /**
     * <p>symbol.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String symbol() {
        String n = "U";
        if (this == INDEX)        return "I";
        else if (this == DETAIL)  return "D";
        else if (this == REVIEW)  return "R";
        else if (this == SELLER)  return "SL";
        else if (this == PROFILE) return "P";
        else if (this == SEARCH)  return "S";
        else if (this == MEDIA)   return "M";
        else if (this == BBS)     return "B";
        else if (this == TIEBA)   return "T";
        else if (this == BLOG)    return "G";
        else if (this == UNKNOWN) return "U";
        return n;
    }

    @Override
    public String toString() {
        return name() + " " + symbol();
    }

    public static PageCategory parse(String category) {
        try {
            return PageCategory.valueOf(StringUtils.substringBefore(category, " "));
        } catch (Throwable e) {
            return PageCategory.UNKNOWN;
        }
    }
}
