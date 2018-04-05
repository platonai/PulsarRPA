package fun.platonic.pulsar.crawl.parse.html;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by vincent on 17-8-9.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class JsoupUtils {

    public static Document sanitize(Document doc, boolean pithy) {
        Set<String> unsafeNodes = Sets.newHashSet(
                "title", "base", "script", "meta", "iframe", "link[ref=icon]", "link[ref=\"shortcut icon\"]");
        Set<String> obsoleteNodeNames = Sets.newHashSet("style", "link", "head");
        Set<Node> obsoleteNodes = new HashSet<>();

        doc.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int i) {
                String nodeName = node.nodeName();
                if (unsafeNodes.contains(nodeName)) {
                    obsoleteNodes.add(node);
                }

                if (pithy) {
                    node.removeAttr("style");
                    if (obsoleteNodeNames.contains(nodeName)) {
                        obsoleteNodes.add(node);
                    }
                }
            }

            @Override
            public void tail(Node node, int i) {

            }
        });

        obsoleteNodes.forEach(Node::remove);

        doc.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int i) {
                if (!(node instanceof Element)) {
                    return;
                }

                Element ele = (Element) node;
                if (ele.id().isEmpty() && ele.className().isEmpty()) {
                    return;
                }

                String selector = ele.cssSelector();
                ele.attr("warpsselector", selector); // Deprecated
                ele.addClass("warpsselector"); // Deprecated
                ele.addClass("has-selector"); // Deprecated
            }

            @Override
            public void tail(Node node, int i) {

            }
        });

        for (Element ele : doc.select("html,head,body")) {
            // ele.clearAttrs();
            ele.attr("id", "pulsar" + StringUtils.capitalize(ele.nodeName()));
        }

        return doc;
    }

    public static String toHtmlPiece(Document doc, boolean pithy) {
        doc = sanitize(doc, pithy);

        String content = doc.toString();
        int pos = StringUtils.indexOf(content, "<html");
        if (pos > 0) {
            content = content.substring(pos);
        }

        content = content
                .replaceFirst("<html", "<div")
                .replaceFirst("<body", "<div")
                .replaceFirst("<head", "<div")
                .replaceAll("</html|</body|</head", "</div");

        return content;
    }
}
