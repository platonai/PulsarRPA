package ai.platon.pulsar.boilerpipe.filters.heuristics;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import ai.platon.pulsar.boilerpipe.document.BoiTextDocument;
import ai.platon.pulsar.boilerpipe.filters.TextBlockFilter;
import ai.platon.pulsar.boilerpipe.utils.ManualRules;
import ai.platon.pulsar.boilerpipe.utils.ProcessingException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Marks {@link TextBlock}s which contain parts of the HTML <code>&lt;CONTENT_TITLE&gt;</code> tag, using
 * some heuristics which are quite specific to the news domain.
 */
public final class DocumentTitleMatchClassifier implements TextBlockFilter {

  public static final Pattern PAT_REMOVE_CHARACTERS = Pattern.compile("[\\?？\\!！\\.。\\-\\:：]+");

  public static final int MinTitleSize = 6;

  public static final int MaxTitleSize = 200;

  // 标题中常见的分隔符号，这些分隔符号往往带有一些低价值信息
  public static final Pattern[] PotentialTitlePatterns = {
          Pattern.compile("[ ]*[\\|»|-][ ]*"),
          Pattern.compile("[ ]*[\\|»|:][ ]*"),
          Pattern.compile("[ ]*[\\|»|:\\(\\)][ ]*"),
          Pattern.compile("[ ]*[\\|»|:\\(\\)\\-][ ]*"),
          Pattern.compile("[ ]*[\\|»|,|:\\(\\)\\-][ ]*"),
          Pattern.compile("[ ]*[\\|»|,|:\\(\\)\\-\u00a0][ ]*"),

          // 最精细分隔
          Pattern.compile("[ ]*[»,，:：_（）【】\\|\\-\\(\\)][ ]*"),
          // 相比上一个，少了-。某些标题中会带有范围参数如：索尼 NEX-3NL 16-50mm 单变焦镜头
          Pattern.compile("[ ]*[»,，:：_（）【】\\|\\(\\)][ ]*"),
          // 少了:和：
          Pattern.compile("[ ]*[»,，_（）【】\\|\\(\\)][ ]*"),
          // 少了_
          Pattern.compile("[ ]*[»,，（）【】\\|\\(\\)][ ]*"),
          // 最少分隔
          Pattern.compile("[ ]*[»【】][ ]*")
  };

  private final Set<String> potentialTitles;

  public DocumentTitleMatchClassifier(String title) {
    if (title == null) {
      this.potentialTitles = null;
    } else {
      title = title.replace('\u00a0', ' ');
      title = title.replace("'", "");

      title = title.trim().toLowerCase();

      if (title.length() == 0) {
        this.potentialTitles = null;
      } else {
        this.potentialTitles = new HashSet<>();

        potentialTitles.add(title);

        for (Pattern pattern : PotentialTitlePatterns) {
          String p = getLongestPart(title, pattern);
          if (validatePotentialTitle(p)) {
            potentialTitles.add(p);
          }
        }

        addPotentialTitles(potentialTitles, title, "[ ]+[\\|][ ]+", 4);
        addPotentialTitles(potentialTitles, title, "[ ]+[\\-][ ]+", 4);

        potentialTitles.add(title.replaceFirst(" - [^\\-]+$", ""));
        potentialTitles.add(title.replaceFirst("^[^\\-]+ - ", ""));
      }
    }
  }

  public boolean process(BoiTextDocument doc) throws ProcessingException {
    String contentTitle = extractContentTitleByRule(doc);
    if (contentTitle != null) {
      return true;
    }

    return extractContentTitleByPageTitle(doc);
  }

  public String extractContentTitleByRule(BoiTextDocument doc) {
    String selector = null;
    for (Map.Entry<String, String> entry : ManualRules.TITLE_RULES.entrySet()) {
      if (doc.getBaseUrl().matches(entry.getKey())) {
        selector = entry.getValue();
        break;
      }
    }

    if (selector == null) {
      return null;
    }

    String contentTitle = null;
    for (final TextBlock tb : doc.getTextBlocks()) {
      if (tb.getCssSelector().equalsIgnoreCase(selector)) {
        contentTitle = tb.getText();

        tb.setIsContent(true);
        tb.addLabel(BlockLabels.CONTENT_TITLE);
        break;
      }
    }

    return contentTitle;
  }

  public boolean extractContentTitleByPageTitle(BoiTextDocument doc) {
    if (potentialTitles == null) {
      return false;
    }

    boolean changes = false;

    for (final TextBlock tb : doc.getTextBlocks()) {
      String text = tb.getText();

      text = text.replace('\u00a0', ' ');
      text = text.replace("'", "");

      text = text.trim().toLowerCase();

      if (potentialTitles.contains(text)) {
        tb.addLabel(BlockLabels.CONTENT_TITLE);
        changes = true;
        break;
      }

      if (text.length() > MaxTitleSize) {
        continue;
      }

      text = PAT_REMOVE_CHARACTERS.matcher(text).replaceAll("").trim();
      if (potentialTitles.contains(text)) {
        tb.addLabel(BlockLabels.CONTENT_TITLE);
        changes = true;
        break;
      }
    }

    return changes;
  }

  public boolean validatePotentialTitle(String title) {
    // 商品标题，至少10个字符，最多200个字符
    return !(title == null || title.length() <= MinTitleSize || title.length() >= MaxTitleSize);

  }

  public Set<String> getPotentialTitles() {
    return potentialTitles;
  }

  private void addPotentialTitles(final Set<String> potentialTitles, final String title,
                                  final String pattern, final int minWords) {
    String[] parts = title.split(pattern);
    if (parts.length == 1) {
      return;
    }
    for (int i = 0; i < parts.length; i++) {
      String p = parts[i];
      if (p.contains(".com")) {
        continue;
      }
      final int numWords = p.split("[\b ]+").length;
      if (numWords >= minWords) {
        potentialTitles.add(p);
      }
    }
  }

  private String getLongestPart(final String title, final Pattern pattern) {
    String[] parts = title.split(pattern.pattern());
    if (parts.length == 1) {
      return null;
    }
    int longestNumWords = 0;
    String longestPart = "";
    for (int i = 0; i < parts.length; i++) {
      String p = parts[i];
      if (p.contains(".com")) {
        continue;
      }
      final int numWords = p.split("[\b ]+").length;
      if (numWords > longestNumWords || p.length() > longestPart.length()) {
        longestNumWords = numWords;
        longestPart = p;
      }
    }
    if (longestPart.length() == 0) {
      return null;
    } else {
      return longestPart.trim();
    }
  }

}
