package ai.platon.pulsar.boilerpipe.sax;

import ai.platon.pulsar.boilerpipe.document.BlockLabels;
import ai.platon.pulsar.boilerpipe.document.LabelAction;
import ai.platon.pulsar.boilerpipe.document.TextBlock;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Assigns labels for element CSS classes and ids to the corresponding {@link TextBlock}. CSS
 * classes are prefixed by <code>{@link BlockLabels#MARKUP_PREFIX}.</code>, and IDs are prefixed
 * by <code>{@link BlockLabels#MARKUP_PREFIX}#</code>
 */
public final class MarkupTagAction implements TagAction {

  private final boolean isBlockLevel;
  private LinkedList<List<String>> labelStack = new LinkedList<>();

  public MarkupTagAction(final boolean isBlockLevel) {
    this.isBlockLevel = isBlockLevel;
  }

  private static final Pattern PAT_NUM = Pattern.compile("[0-9]+");

  @Override
  public boolean start(HTMLContentHandler instance, String localName, String qName,
                       Attributes atts) throws SAXException {
    List<String> labels = new ArrayList<>(5);
    labels.add(BlockLabels.MARKUP_PREFIX + localName);

    String classVal = atts.getValue("class");

    if (classVal != null && classVal.length() > 0) {
      classVal = PAT_NUM.matcher(classVal).replaceAll("#");
      classVal = classVal.trim();
      String[] vals = classVal.split("[ ]+");
      labels.add(BlockLabels.MARKUP_PREFIX + "." + classVal.replace(' ', '.'));
      if (vals.length > 1) {
        for (String s : vals) {
          labels.add(BlockLabels.MARKUP_PREFIX + "." + s);
        }
      }
    }

    String id = atts.getValue("id");
    if (id != null && id.length() > 0) {
      id = PAT_NUM.matcher(id).replaceAll("#");
      labels.add(BlockLabels.MARKUP_PREFIX + "#" + id);
    }

    Set<String> ancestors = getAncestorLabels();
    List<String> labelsWithAncestors = new ArrayList<>((ancestors.size() + 1) * labels.size());

    for (String l : labels) {
      for (String an : ancestors) {
        labelsWithAncestors.add(an);
        labelsWithAncestors.add(an + " " + l);
      }
      labelsWithAncestors.add(l);
    }

    instance.addLabelAction(new LabelAction(labelsWithAncestors.toArray(new String[labelsWithAncestors.size()])));

    labelStack.add(labels);

    return isBlockLevel;
  }

  @Override
  public boolean end(HTMLContentHandler instance, String localName, String qName)
      throws SAXException {

    labelStack.removeLast();
    return isBlockLevel;
  }

  public boolean changesTagLevel() {
    return isBlockLevel;
  }

  private Set<String> getAncestorLabels() {
    Set<String> set = new HashSet<>();
    for (List<String> labels : labelStack) {
      if (labels == null) {
        continue;
      }
      set.addAll(labels);
    }
    return set;
  }
}
