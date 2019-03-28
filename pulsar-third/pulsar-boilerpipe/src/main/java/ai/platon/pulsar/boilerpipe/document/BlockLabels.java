package ai.platon.pulsar.boilerpipe.document;

/**
 * Some pre-defined labels which can be used in conjunction with {@link TextBlock#addLabel(String)}
 * and {@link TextBlock#hasLabel(String)}.
 */
public interface BlockLabels {
  String CONTENT_TITLE = "pulsar.text/CONTENT_TITLE";
  String ARTICLE_METADATA = "pulsar.text/ARTICLE_METADATA";
  String INDICATES_END_OF_TEXT = "pulsar.text/INDICATES_END_OF_TEXT";
  String MIGHT_BE_CONTENT = "pulsar.text/MIGHT_BE_CONTENT";
  String VERY_LIKELY_CONTENT = "pulsar.text/VERY_LIKELY_CONTENT";
  String STRICTLY_NOT_CONTENT = "pulsar.text/STRICTLY_NOT_CONTENT";
  String TOO_MANY_DATE_STRING_CONTENT = "pulsar.text/TOO_MANY_DATE_STRING_CONTENT";
  String HR = "pulsar.text/HR";
  String LI = "pulsar.text/LI";

  String HEADING = "pulsar.text/HEADING";
  String H1 = "pulsar.text/H1";
  String H2 = "pulsar.text/H2";
  String H3 = "pulsar.text/H3";

  String MARKUP_PREFIX = "<";
}
