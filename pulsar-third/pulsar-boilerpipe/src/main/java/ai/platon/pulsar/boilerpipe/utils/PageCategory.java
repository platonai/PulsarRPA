package ai.platon.pulsar.boilerpipe.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by vincent on 17-2-13.
 */
public enum PageCategory {
  INDEX, DETAIL, SEARCH, MEDIA, BBS, TIEBA, BLOG, UNKNOWN;
  
  /**
   * The follow patterns are simple rule to indicate a url's category, this is a very simple solution, and the result is
   * not accurate
   */
  public static Pattern[] INDEX_PAGE_URL_PATTERNS = {
          Pattern.compile(".+tieba.baidu.com/.+search.+"),
          Pattern.compile(".+(index|list|tags|chanel).+"),
  };

  public static Pattern SEARCH_PAGE_URL_PATTERN = Pattern.compile(".+(search|query|select).+");

  public static Pattern[] DETAIL_PAGE_URL_PATTERNS = {
          Pattern.compile(".+tieba.baidu.com/p/(\\d+)"),
          Pattern.compile(".+(detail|item|article|book|good|product|thread|view|post|content|/20[012][0-9]/{0,1}[01][0-9]/|/20[012]-[0-9]{0,1}-[01][0-9]/|/\\d{2,}/\\d{5,}|\\d{7,}).+")
  };

  public static Pattern MEDIA_PAGE_URL_PATTERN = Pattern.compile(".+(pic|picture|photo|avatar|photoshow|video).+");

  public static final String[] MEDIA_URL_SUFFIXES = {"js", "css", "jpg", "png", "jpeg", "gif"};

  public boolean is(PageCategory pageCategory) {
    return pageCategory == this;
  }
  public boolean isIndex() {
    return this == INDEX;
  }
  public boolean isDetail() {
    return this == DETAIL;
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

  /**
   * TODO : need carefully test
   */
  public static PageCategory sniff(String url, int _char, int _a) {
    if (url.isEmpty()) {
      return UNKNOWN;
    }

    PageCategory pageCategory = sniff(url);
    if (pageCategory.isDetail()) {
      return pageCategory;
    }

    if (_char < 100) {
      if (_a > 30) {
        pageCategory = INDEX;
      }
    } else {
      return sniffByTextDensity(_char, _a);
    }

    return pageCategory;
  }

  /* TODO : use machine learning to calculate the parameters */
  private static PageCategory sniffByTextDensity(double _char, double _a) {
    PageCategory pageCategory = UNKNOWN;

    if (_a < 1) {
      _a = 1;
    }

    if (_a > 60 && _char / _a < 20) {
      // 索引页：链接数不少于60个，文本密度小于20
      pageCategory = INDEX;
    } else if (_char / _a > 30) {
      pageCategory = DETAIL;
    }

    return pageCategory;
  }

  /**
   * A simple regex rule to sniff the possible category of a web page
   * */
  public static PageCategory sniff(String urlString) {
    PageCategory pageCategory = UNKNOWN;

    if (StringUtils.isEmpty(urlString)) {
      return pageCategory;
    }

    final String url = urlString.toLowerCase();

    // Notice : ***DO KEEP*** the right order
    if (url.endsWith("/")) {
      pageCategory = INDEX;
    }
    else if (StringUtils.countMatches(url, "/") <= 3) {
      // http://t.tt/12345678
      pageCategory = INDEX;
    }
    else if (Stream.of(INDEX_PAGE_URL_PATTERNS).anyMatch(pattern -> pattern.matcher(url).matches())) {
      pageCategory = INDEX;
    }
    else if (Stream.of(DETAIL_PAGE_URL_PATTERNS).anyMatch(pattern -> pattern.matcher(url).matches())){
      pageCategory = DETAIL;
    }
    else if (SEARCH_PAGE_URL_PATTERN.matcher(url).matches()) {
      pageCategory = SEARCH;
    }
    else if (MEDIA_PAGE_URL_PATTERN.matcher(url).matches()) {
      pageCategory = MEDIA;
    }

    return pageCategory;
  }
}
