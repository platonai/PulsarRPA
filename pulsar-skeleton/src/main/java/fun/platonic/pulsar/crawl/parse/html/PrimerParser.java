/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fun.platonic.pulsar.crawl.parse.html;

import com.google.common.collect.Maps;
import fun.platonic.pulsar.common.DomUtil;
import fun.platonic.pulsar.common.EncodingDetector;
import fun.platonic.pulsar.common.NodeWalker;
import fun.platonic.pulsar.common.URLUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.filter.CrawlFilters;
import fun.platonic.pulsar.crawl.parse.Parser;
import fun.platonic.pulsar.persist.HypeLink;
import fun.platonic.pulsar.persist.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * A very simple DOM parser
 * <p>
 * A collection of methods for extracting content from DOM trees.
 * <p>
 * This class holds a few utility methods for pulling content out of DOM nodes,
 * such as getLiveLinks, getPageText, etc.
 */
public class PrimerParser {

    public static final Logger LOG = LoggerFactory.getLogger(Parser.class);
    private EncodingDetector encodingDetector;
    private HashMap<String, LinkParams> linkParams = new HashMap<>();

    public PrimerParser(ImmutableConfig conf) {
        setConf(conf);
    }

    public void setConf(ImmutableConfig conf) {
        this.encodingDetector = new EncodingDetector(conf);

        // forceTags is used to override configurable tag ignoring, later on
        Collection<String> forceTags = new ArrayList<>(1);

        linkParams.clear();
        linkParams.put("a", new LinkParams("a", "href", 1));
        linkParams.put("area", new LinkParams("area", "href", 0));
        if (conf.getBoolean("parser.html.form.use_action", true)) {
            linkParams.put("form", new LinkParams("form", "action", 1));
            if (conf.get("parser.html.form.use_action") != null) {
                forceTags.add("form");
            }
        }
        linkParams.put("frame", new LinkParams("frame", "src", 0));
        linkParams.put("iframe", new LinkParams("iframe", "src", 0));
        linkParams.put("script", new LinkParams("script", "src", 0));
        linkParams.put("link", new LinkParams("link", "href", 0));
        linkParams.put("img", new LinkParams("img", "src", 0));

        // remove unwanted link tags from the linkParams map
        String[] ignoreTags = conf.getStrings("parser.html.outlinks.ignore_tags");
        for (int i = 0; ignoreTags != null && i < ignoreTags.length; i++) {
            if (!forceTags.contains(ignoreTags[i]))
                linkParams.remove(ignoreTags[i]);
        }
    }

    public void detectEncoding(WebPage page) {
        String encoding = encodingDetector.sniffEncoding(page);
        if (encoding != null && !encoding.isEmpty()) {
            page.setEncoding(encoding);
            page.setEncodingClues(encodingDetector.getCluesAsString());
        } else {
            LOG.warn("Failed to detect encoding, url: " + page.getUrl());
        }
    }

    /**
     * This method takes a {@link StringBuilder} and a DOM {@link Node}, and will
     * append all the content text found beneath the DOM node to the
     * <code>StringBuilder</code>.
     * <p>
     * <p>
     * <p>
     * If <code>abortOnNestedAnchors</code> is true, DOM traversal will be aborted
     * and the <code>StringBuffer</code> will not contain any text encountered
     * after a nested anchor is found.
     * <p>
     * <p>
     *
     * @return true if nested anchors were found
     */
    public boolean getPageText(StringBuilder sb, Node root, boolean abortOnNestedAnchors) {
        Objects.requireNonNull(root);

        return getTextHelper(sb, root, abortOnNestedAnchors, 0);
    }

    /**
     * This is a convinience method, equivalent to
     * {@link #getPageText(StringBuilder, Node, boolean) getPageText(sb, node, false)}.
     */
    public void getPageText(StringBuilder sb, Node root) {
        Objects.requireNonNull(root);

        getPageText(sb, root, false);
    }

    public String getPageText(Node root) {
        Objects.requireNonNull(root);

        StringBuilder sb = new StringBuilder();
        getPageText(sb, root, false);
        return sb.toString();
    }

    public String getPageTitle(Node root) {
        Objects.requireNonNull(root);

        StringBuilder sb = new StringBuilder();
        getPageTitle(sb, root);
        return sb.toString();
    }

    /**
     * This method takes a {@link StringBuffer} and a DOM {@link Node}, and will
     * append the content text found beneath the first <code>title</code> node to
     * the <code>StringBuffer</code>.
     *
     * @return true if a title node was found, false otherwise
     */
    public boolean getPageTitle(StringBuilder sb, Node root) {
        Objects.requireNonNull(root);

        NodeWalker walker = new NodeWalker(root);

        while (walker.hasNext()) {
            Node node = walker.nextNode();
            String name = node.getNodeName();
            short type = node.getNodeType();

            // stop after HEAD
            if ("body".equalsIgnoreCase(name)) {
                return false;
            }

            if (type == Node.ELEMENT_NODE) {
                if ("title".equalsIgnoreCase(name)) {
                    getPageText(sb, node);
                    return true;
                }
            }
        }

        return false;
    }

    public Map<String, String> getMetadata(Node root) {
        Objects.requireNonNull(root);

        Map<String, String> metadata = Maps.newLinkedHashMap();
        StringBuilder sb = new StringBuilder();

        NodeWalker walker = new NodeWalker(root);

        while (walker.hasNext()) {
            Node currentNode = walker.nextNode();
            String nodeName = currentNode.getNodeName();
            short nodeType = currentNode.getNodeType();

            // stop after HEAD
            if ("body".equalsIgnoreCase(nodeName)) {
                return metadata;
            }

            if (nodeType == Node.ELEMENT_NODE) {
                if ("title".equalsIgnoreCase(nodeName)) {
                    sb.setLength(0);
                    getPageText(sb, currentNode);
                    metadata.put("meta-title", sb.toString());
                } else if ("meta".equalsIgnoreCase(nodeName)) {
                    getMetadataFromMetaTag(metadata, root);
                }
            } // if nodeType ...
        }

        return metadata;
    }

    private void getMetadataFromMetaTag(Map<String, String> metadata, Node metaNode) {
        String attrValue = DomUtil.getAttribute(metaNode, "name");
        if (attrValue == null) {
            return;
        }

        if ("keywords".equalsIgnoreCase(attrValue)) {
            attrValue = DomUtil.getAttribute(metaNode, "content");
            if (attrValue != null) {
                metadata.put("meta-keywords", attrValue);
            }
        } else if ("description".equalsIgnoreCase(attrValue)) {
            attrValue = DomUtil.getAttribute(metaNode, "content");
            if (attrValue != null) {
                metadata.put("meta-description", attrValue);
            }
        }
    }

    /**
     * If Node contains a BASE tag then it's HREF is returned.
     */
    public URL getBaseURL(Node root) {
        Objects.requireNonNull(root);

        NodeWalker walker = new NodeWalker(root);

        while (walker.hasNext()) {

            Node currentNode = walker.nextNode();
            String nodeName = currentNode.getNodeName();
            short nodeType = currentNode.getNodeType();

            // is this root a BASE tag?
            if (nodeType == Node.ELEMENT_NODE) {
                if ("body".equalsIgnoreCase(nodeName)) { // stop after HEAD
                    return null;
                }

                if ("base".equalsIgnoreCase(nodeName)) {
                    NamedNodeMap attrs = currentNode.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node attr = attrs.item(i);
                        if ("href".equalsIgnoreCase(attr.getNodeName())) {
                            try {
                                return new URL(attr.getNodeValue());
                            } catch (MalformedURLException ignored) {
                            }
                        }
                    }
                }
            }
        }

        // no.
        return null;
    }

    // returns true if abortOnNestedAnchors is true and we find nested
    // anchors
    private boolean getTextHelper(StringBuilder sb, Node root, boolean abortOnNestedAnchors, int anchorDepth) {
        boolean abort = false;
        NodeWalker walker = new NodeWalker(root);

        while (walker.hasNext()) {
            Node currentNode = walker.nextNode();
            String nodeName = currentNode.getNodeName();
            short nodeType = currentNode.getNodeType();

            if ("script".equalsIgnoreCase(nodeName)) {
                walker.skipChildren();
            }
            if ("style".equalsIgnoreCase(nodeName)) {
                walker.skipChildren();
            }

            if (abortOnNestedAnchors && "a".equalsIgnoreCase(nodeName)) {
                anchorDepth++;
                if (anchorDepth > 1) {
                    abort = true;
                    break;
                }
            }

            if (nodeType == Node.COMMENT_NODE) {
                walker.skipChildren();
            }

            if (nodeType == Node.TEXT_NODE) {
                // cleanup and trim the value
                String text = currentNode.getNodeValue();
                text = text.replaceAll("\\s+", " ");
                text = text.trim();
                if (text.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(text);
                }
            }
        }

        return abort;
    }

    private boolean hasOnlyWhiteSpace(Node root) {
        String val = root.getNodeValue();
        for (int i = 0; i < val.length(); i++) {
            if (!Character.isWhitespace(val.charAt(i)))
                return false;
        }
        return true;
    }

    // this only covers a few cases of empty links that are symptomatic
    // of nekohtml's DOM-fixup process...
    private boolean shouldThrowAwayLink(Node root, NodeList children, int childLen, LinkParams params) {
        if (childLen == 0) {
            // this has no inner structure
            return params.childLen != 0;
        } else if ((childLen == 1)
                && (children.item(0).getNodeType() == Node.ELEMENT_NODE)
                && (params.elName.equalsIgnoreCase(children.item(0).getNodeName()))) {
            // single nested link
            return true;
        } else if (childLen == 2) {
            Node c0 = children.item(0);
            Node c1 = children.item(1);

            if ((c0.getNodeType() == Node.ELEMENT_NODE)
                    && (params.elName.equalsIgnoreCase(c0.getNodeName()))
                    && (c1.getNodeType() == Node.TEXT_NODE) && hasOnlyWhiteSpace(c1)) {
                // single link followed by whitespace root
                return true;
            }

            if ((c1.getNodeType() == Node.ELEMENT_NODE)
                    && (params.elName.equalsIgnoreCase(c1.getNodeName()))
                    && (c0.getNodeType() == Node.TEXT_NODE) && hasOnlyWhiteSpace(c0)) {
                // whitespace root followed by single link
                return true;
            }

        } else if (childLen == 3) {
            Node c0 = children.item(0);
            Node c1 = children.item(1);
            Node c2 = children.item(2);

            if ((c1.getNodeType() == Node.ELEMENT_NODE)
                    && (params.elName.equalsIgnoreCase(c1.getNodeName()))
                    && (c0.getNodeType() == Node.TEXT_NODE)
                    && (c2.getNodeType() == Node.TEXT_NODE) && hasOnlyWhiteSpace(c0)
                    && hasOnlyWhiteSpace(c2)) {
                // single link surrounded by whitespace nodes
                return true;
            }
        }

        return false;
    }

    /**
     * This method finds all anchors below the supplied DOM <code>root</code>, and
     * creates appropriate {@link HypeLink} records for each (relative to the
     * supplied <code>base</code> URL), and adds them to the <code>outlinks</code>
     * {@link ArrayList}.
     * <p>
     * <p>
     * <p>
     * Links without inner structure (tags, text, etc) are discarded, as are links
     * which contain only single nested links and empty text nodes (this is a
     * common DOM-fixup artifact, at least with nekohtml).
     */

    public ArrayList<HypeLink> getLinks(URL base, Node root) {
        return getLinks(base, root, null);
    }

    public ArrayList<HypeLink> getLinks(URL base, Node root, CrawlFilters crawlFilters) {
        return getLinks(base, new ArrayList<>(), root, crawlFilters);
    }

    public ArrayList<HypeLink> getLinks(URL base, ArrayList<HypeLink> hypeLinks, Node root, CrawlFilters crawlFilters) {
        NodeWalker walker = new NodeWalker(root);

        while (walker.hasNext()) {
            Node currentNode = walker.nextNode();

            if (crawlFilters == null || crawlFilters.isAllowed(currentNode)) {
                getLinksStep2(base, hypeLinks, currentNode, crawlFilters);
                walker.skipChildren();
            } else {
                LOG.debug("Block disallowed, skip : " + DomUtil.getPrettyName(currentNode));
            }
        }

        return hypeLinks;
    }

    private void getLinksStep2(URL base, ArrayList<HypeLink> hypeLinks, Node root, CrawlFilters crawlFilters) {
        NodeWalker walker = new NodeWalker(root);
        // LOG.debug("Get hypeLinks for " + DomUtil.getPrettyName(root));

        while (walker.hasNext()) {
            Node currentNode = walker.nextNode();

            if (crawlFilters != null && crawlFilters.isDisallowed(currentNode)) {
                LOG.debug("Block disallowed, skip : " + DomUtil.getPrettyName(currentNode));
                walker.skipChildren();
                continue;
            }

            String nodeName = currentNode.getNodeName();
            short nodeType = currentNode.getNodeType();
            NodeList children = currentNode.getChildNodes();
            int childLen = (children != null) ? children.getLength() : 0;

            if (nodeType == Node.ELEMENT_NODE) {
                nodeName = nodeName.toLowerCase();
                LinkParams params = linkParams.get(nodeName);
                if (params != null) {
                    if (!shouldThrowAwayLink(currentNode, children, childLen, params)) {
                        StringBuilder linkText = new StringBuilder();
                        getPageText(linkText, currentNode, true);

                        NamedNodeMap attrs = currentNode.getAttributes();
                        String target = null;
                        boolean noFollow = false;
                        boolean post = false;
                        boolean allow = true;
                        for (int i = 0; i < attrs.getLength(); i++) {
                            Node attr = attrs.item(i);
                            String attrName = attr.getNodeName();
                            if (params.attrName.equalsIgnoreCase(attrName)) {
                                target = attr.getNodeValue();
                            } else if ("rel".equalsIgnoreCase(attrName)
                                    && "nofollow".equalsIgnoreCase(attr.getNodeValue())) {
                                noFollow = true;
                            } else if ("rel".equalsIgnoreCase(attrName)
                                    && "qi-nofollow".equalsIgnoreCase(attr.getNodeValue())) {
                                allow = false;
                            } else if ("method".equalsIgnoreCase(attrName)
                                    && "post".equalsIgnoreCase(attr.getNodeValue())) {
                                post = true;
                            }
                        }

                        if (target != null && !noFollow && !post)
                            try {
                                URL url = URLUtil.resolveURL(base, target);
                                hypeLinks.add(new HypeLink(url.toString(), linkText.toString().trim()));
                            } catch (MalformedURLException ignored) {
                            }
                    } // if not should throw away
                    // this should not have any children, skip them
                    if (params.childLen == 0) {
                    }
                }
            }
        }
    }

    private static class LinkParams {
        public String elName;
        public String attrName;
        public int childLen;

        public LinkParams(String elName, String attrName, int childLen) {
            this.elName = elName;
            this.attrName = attrName;
            this.childLen = childLen;
        }

        public String toString() {
            return "LP[el=" + elName + ",attr=" + attrName + ",len=" + childLen + "]";
        }
    }
}
