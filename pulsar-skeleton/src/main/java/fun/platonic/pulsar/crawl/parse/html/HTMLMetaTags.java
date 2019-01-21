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

import fun.platonic.pulsar.persist.metadata.MultiMetadata;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.Properties;

/**
 * This class holds the information about HTML "meta" tags extracted from a
 * page. Some special tags have convenience methods for easy checking.
 */
public class HTMLMetaTags {
    private boolean noIndex = false;
    private boolean noFollow = false;
    private boolean noCache = false;
    private URL baseHref = null;
    private boolean refresh = false;
    private int refreshTime = 0;
    private URL refreshHref = null;
    private MultiMetadata generalTags = new MultiMetadata();
    private Properties httpEquivTags = new Properties();

    private URL currURL;

    public HTMLMetaTags(Node root, URL currURL) {
        this.currURL = currURL;
        walk(root);
    }

    /**
     * Sets all boolean values to <code>false</code>. Clears all other tags.
     */
    public void reset() {
        noIndex = false;
        noFollow = false;
        noCache = false;
        refresh = false;
        refreshTime = 0;
        baseHref = null;
        refreshHref = null;
        generalTags.clear();
        httpEquivTags.clear();
    }

    /**
     * Sets <code>noFollow</code> to <code>true</code>.
     */
    public void setNoFollow() {
        noFollow = true;
    }

    /**
     * Sets <code>noIndex</code> to <code>true</code>.
     */
    public void setNoIndex() {
        noIndex = true;
    }

    /**
     * Sets <code>noCache</code> to <code>true</code>.
     */
    public void setNoCache() {
        noCache = true;
    }

    /**
     * A convenience method. Returns the current value of <code>noIndex</code>.
     */
    public boolean getNoIndex() {
        return noIndex;
    }

    /**
     * A convenience method. Returns the current value of <code>noFollow</code>.
     */
    public boolean getNoFollow() {
        return noFollow;
    }

    /**
     * A convenience method. Returns the current value of <code>noCache</code>.
     */
    public boolean getNoCache() {
        return noCache;
    }

    /**
     * A convenience method. Returns the current value of <code>refresh</code>.
     */
    public boolean getRefresh() {
        return refresh;
    }

    /**
     * Sets <code>refresh</code> to the supplied value.
     */
    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    /**
     * A convenience method. Returns the <code>baseHref</code>, if set, or
     * <code>null</code> otherwise.
     */
    public URL getBaseHref() {
        return baseHref;
    }

    /**
     * Sets the <code>baseHref</code>.
     */
    public void setBaseHref(URL baseHref) {
        this.baseHref = baseHref;
    }

    /**
     * A convenience method. Returns the <code>refreshHref</code>, if set, or
     * <code>null</code> otherwise. The value may be invalid if
     * {@link #getRefresh()}returns <code>false</code>.
     */
    public URL getRefreshHref() {
        return refreshHref;
    }

    /**
     * Sets the <code>refreshHref</code>.
     */
    public void setRefreshHref(URL refreshHref) {
        this.refreshHref = refreshHref;
    }

    /**
     * A convenience method. Returns the current value of <code>refreshTime</code>
     * . The value may be invalid if {@link #getRefresh()}returns
     * <code>false</code>.
     */
    public int getRefreshTime() {
        return refreshTime;
    }

    /**
     * Sets the <code>refreshTime</code>.
     */
    public void setRefreshTime(int refreshTime) {
        this.refreshTime = refreshTime;
    }

    /**
     * Returns all collected values of the general meta tags. Property names are
     * tag names, property values are "content" values.
     */
    public MultiMetadata getGeneralTags() {
        return generalTags;
    }

    /**
     * Returns all collected values of the "http-equiv" meta tags. Property names
     * are tag names, property values are "content" values.
     */
    public Properties getHttpEquivTags() {
        return httpEquivTags;
    }

    /**
     * Utility class with indicators for the robots directives "noindex" and
     * "nofollow", and HTTP-EQUIV/no-cache
     */
    public void walk(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if ("body".equalsIgnoreCase(node.getNodeName())) {
                // META tags should not be under body
                return;
            }

            if ("meta".equalsIgnoreCase(node.getNodeName())) {
                NamedNodeMap attrs = node.getAttributes();
                Node nameNode = null;
                Node equivNode = null;
                Node contentNode = null;
                // Retrieves name, http-equiv and content attribues
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName().toLowerCase();
                    if (attrName.equals("name")) {
                        nameNode = attr;
                    } else if (attrName.equals("http-equiv")) {
                        equivNode = attr;
                    } else if (attrName.equals("content")) {
                        contentNode = attr;
                    }
                }

                if (nameNode != null) {
                    if (contentNode != null) {
                        String name = nameNode.getNodeValue().toLowerCase();
                        this.getGeneralTags().put(name, contentNode.getNodeValue());
                        if ("robots".equals(name)) {

                            if (contentNode != null) {
                                String directives = contentNode.getNodeValue().toLowerCase();
                                int index = directives.indexOf("none");

                                if (index >= 0) {
                                    this.setNoIndex();
                                    this.setNoFollow();
                                }

                                index = directives.indexOf("all");
                                if (index >= 0) {
                                    // do nothing...
                                }

                                index = directives.indexOf("noindex");
                                if (index >= 0) {
                                    this.setNoIndex();
                                }

                                index = directives.indexOf("nofollow");
                                if (index >= 0) {
                                    this.setNoFollow();
                                }

                                index = directives.indexOf("noarchive");
                                if (index >= 0) {
                                    this.setNoCache();
                                }
                            }

                        } // end if (name == robots)
                    }
                }

                if (equivNode != null) {
                    if (contentNode != null) {
                        String name = equivNode.getNodeValue().toLowerCase();
                        String content = contentNode.getNodeValue();
                        this.getHttpEquivTags().setProperty(name, content);
                        if ("pragma".equals(name)) {
                            content = content.toLowerCase();
                            int index = content.indexOf("no-cache");
                            if (index >= 0)
                                this.setNoCache();
                        } else if ("refresh".equals(name)) {
                            int idx = content.indexOf(';');
                            String time = null;
                            if (idx == -1) { // just the refresh time
                                time = content;
                            } else
                                time = content.substring(0, idx);
                            try {
                                this.setRefreshTime(Integer.parseInt(time));
                                // skip this if we couldn't parse the time
                                this.setRefresh(true);
                            } catch (Exception e) {
                            }
                            URL refreshUrl = null;
                            if (this.getRefresh() && idx != -1) { // set the URL
                                idx = content.toLowerCase().indexOf("url=");
                                if (idx == -1) { // assume a mis-formatted entry with just the
                                    // url
                                    idx = content.indexOf(';') + 1;
                                } else
                                    idx += 4;
                                if (idx != -1) {
                                    String url = content.substring(idx);
                                    try {
                                        refreshUrl = new URL(url);
                                    } catch (Exception e) {
                                        // XXX according to the spec, this has to be an absolute
                                        // XXX url. However, many websites use relative URLs and
                                        // XXX expect browsers to handle that.
                                        // XXX Unfortunately, in some cases this may create a
                                        // XXX infinitely recursive paths (a crawler trap)...
                                        // if (!url.startsWith("/")) url = "/" + url;
                                        try {
                                            refreshUrl = new URL(currURL, url);
                                        } catch (Exception e1) {
                                            refreshUrl = null;
                                        }
                                    }
                                }
                            }
                            if (this.getRefresh()) {
                                if (refreshUrl == null) {
                                    // apparently only refresh time was present. set the URL
                                    // to the same URL.
                                    refreshUrl = currURL;
                                }
                                this.setRefreshHref(refreshUrl);
                            }
                        }
                    }
                }
            } else if ("base".equalsIgnoreCase(node.getNodeName())) {
                NamedNodeMap attrs = node.getAttributes();
                Node hrefNode = attrs.getNamedItem("href");

                if (hrefNode != null) {
                    String urlString = hrefNode.getNodeValue();

                    URL url = null;
                    try {
                        if (currURL == null)
                            url = new URL(urlString);
                        else
                            url = new URL(currURL, urlString);
                    } catch (Exception ignored) {
                    }

                    if (url != null)
                        this.setBaseHref(url);
                }
            }
        }

        NodeList children = node.getChildNodes();
        if (children != null) {
            int len = children.getLength();
            for (int i = 0; i < len; i++) {
                walk(children.item(i));
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("base=" + baseHref + ", noCache=" + noCache + ", noFollow="
                + noFollow + ", noIndex=" + noIndex + ", refresh=" + refresh
                + ", refreshHref=" + refreshHref + "\n");

        sb.append(" * general tags:\n");
        for (String name : generalTags.names()) {
            String key = name;
            sb.append("   - " + key + "\t=\t" + generalTags.get(key) + "\n");
        }

        sb.append(" * http-equiv tags:\n");
        for (Object o : httpEquivTags.keySet()) {
            String key = (String) o;
            sb.append("   - " + key + "\t=\t" + httpEquivTags.get(key) + "\n");
        }
        return sb.toString();
    }
}
