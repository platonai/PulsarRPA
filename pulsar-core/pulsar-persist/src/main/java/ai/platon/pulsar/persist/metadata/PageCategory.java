package ai.platon.pulsar.persist.metadata;

import org.apache.commons.lang3.StringUtils;

/**
 * Predefined page category enumeration
 */
public enum PageCategory {
    INDEX, DETAIL, OFFER_LIST, SELLER, REVIEW, PROFILE, SEARCH, MEDIA, BBS, TIEBA, BLOG, UNKNOWN;

    public boolean is(PageCategory pageCategory) {
        return pageCategory == this;
    }

    public boolean isIndex() {
        return this == INDEX;
    }

    public boolean isDetail() {
        return this == DETAIL;
    }

    public boolean isOfferList() {
        return this == OFFER_LIST;
    }

    public boolean isSeller() {
        return this == SELLER;
    }

    public boolean isReview() {
        return this == REVIEW;
    }

    public boolean isProfile() {
        return this == PROFILE;
    }

    public boolean isSearch() {
        return this == SEARCH;
    }

    public boolean isMedia() {
        return this == MEDIA;
    }

    public boolean isBBS() {
        return this == BBS;
    }

    public boolean isTieBa() {
        return this == TIEBA;
    }

    public boolean isBlog() {
        return this == BLOG;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

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

    public String format() {
        return name() + " " + symbol();
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * Parse a string to a PageCategory object, format() and this method are symmetric.
     * */
    public static PageCategory parse(String category) {
        try {
            return PageCategory.valueOf(StringUtils.substringBefore(category.toUpperCase(), " "));
        } catch (Throwable e) {
            return PageCategory.UNKNOWN;
        }
    }
}
