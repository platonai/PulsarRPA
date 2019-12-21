package ai.platon.pulsar.persist.metadata;

/**
 * Created by vincent on 16-12-18.
 * <p>
 */
public enum PageCategory {
    INDEX, DETAIL, REVIEW, PROFILE, SEARCH, MEDIA, BBS, TIEBA, BLOG, UNKNOWN;

    public boolean is(PageCategory pageCategory) {
        return pageCategory == this;
    }

    public boolean isIndex() {
        return this == INDEX;
    }

    public boolean isDetail() {
        return this == DETAIL;
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
        else if (this == PROFILE) return "P";
        else if (this == SEARCH)  return "S";
        else if (this == MEDIA)   return "M";
        else if (this == BBS)     return "B";
        else if (this == TIEBA)   return "T";
        else if (this == BLOG)    return "BG";
        else if (this == UNKNOWN) return "U";
        return n;
    }
}
