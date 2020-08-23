/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.persist.metadata.MultiMetadata
import org.w3c.dom.Node
import java.net.URL
import java.util.*

/**
 * This class holds the information about HTML "meta" tags extracted from a
 * page. Some special tags have convenience methods for easy checking.
 */
class HTMLMetaTags(root: Node, private val currURL: URL?) {
    /**
     * A convenience method. Returns the current value of `noIndex`.
     */
    var noIndex = false
    /**
     * A convenience method. Returns the current value of `noFollow`.
     */
    var noFollow = false
    /**
     * A convenience method. Returns the current value of `noCache`.
     */
    var noCache = false
    /**
     * A convenience method. Returns the `baseHref`, if set, or
     * `null` otherwise.
     */
    /**
     * Sets the `baseHref`.
     */
    var baseHref: URL? = null
    /**
     * A convenience method. Returns the current value of `refresh`.
     */
    /**
     * Sets `refresh` to the supplied value.
     */
    var refresh = false
    /**
     * A convenience method. Returns the current value of `refreshTime`
     * . The value may be invalid if [.getRefresh]returns
     * `false`.
     */
    /**
     * Sets the `refreshTime`.
     */
    var refreshTime = 0
    /**
     * A convenience method. Returns the `refreshHref`, if set, or
     * `null` otherwise. The value may be invalid if
     * [.getRefresh]returns `false`.
     */
    /**
     * Sets the `refreshHref`.
     */
    var refreshHref: URL? = null
    /**
     * Returns all collected values of the general meta tags. Property names are
     * tag names, property values are "content" values.
     */
    val generalTags = MultiMetadata()
    /**
     * Returns all collected values of the "http-equiv" meta tags. Property names
     * are tag names, property values are "content" values.
     */
    val httpEquivTags = Properties()

    /**
     * Sets all boolean values to `false`. Clears all other tags.
     */
    fun reset() {
        noIndex = false
        noFollow = false
        noCache = false
        refresh = false
        refreshTime = 0
        baseHref = null
        refreshHref = null
        generalTags.clear()
        httpEquivTags.clear()
    }

    /**
     * Sets `noFollow` to `true`.
     */
    fun setNoFollow() {
        noFollow = true
    }

    /**
     * Sets `noIndex` to `true`.
     */
    fun setNoIndex() {
        noIndex = true
    }

    /**
     * Sets `noCache` to `true`.
     */
    fun setNoCache() {
        noCache = true
    }

    /**
     * Utility class with indicators for the robots directives "noindex" and
     * "nofollow", and HTTP-EQUIV/no-cache
     */
    fun walk(node: Node) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            if ("body".equals(node.nodeName, ignoreCase = true)) { // META tags should not be under body
                return
            }
            if ("meta".equals(node.nodeName, ignoreCase = true)) {
                val attrs = node.attributes
                var nameNode: Node? = null
                var equivNode: Node? = null
                var contentNode: Node? = null
                // Retrieves name, http-equiv and content attribues
                for (i in 0 until attrs.length) {
                    val attr = attrs.item(i)
                    val attrName = attr.nodeName.toLowerCase()
                    if (attrName == "name") {
                        nameNode = attr
                    } else if (attrName == "http-equiv") {
                        equivNode = attr
                    } else if (attrName == "content") {
                        contentNode = attr
                    }
                }
                if (nameNode != null) {
                    if (contentNode != null) {
                        val name = nameNode.nodeValue.toLowerCase()
                        generalTags.put(name, contentNode.nodeValue)
                        if ("robots" == name) {
                            if (contentNode != null) {
                                val directives = contentNode.nodeValue.toLowerCase()
                                var index = directives.indexOf("none")
                                if (index >= 0) {
                                    noIndex = true
                                    noFollow = true
                                }
                                index = directives.indexOf("all")
                                if (index >= 0) { // do nothing...
                                }
                                index = directives.indexOf("noindex")
                                if (index >= 0) {
                                    noIndex = true
                                }
                                index = directives.indexOf("nofollow")
                                if (index >= 0) {
                                    noFollow = true
                                }
                                index = directives.indexOf("nocache")
                                if (index >= 0) {
                                    noCache = true
                                }
                            }
                        } // end if (name == robots)
                    }
                }
                if (equivNode != null) {
                    if (contentNode != null) {
                        val name = equivNode.nodeValue.toLowerCase()
                        var content = contentNode.nodeValue
                        httpEquivTags.setProperty(name, content)
                        if ("pragma" == name) {
                            content = content.toLowerCase()
                            val index = content.indexOf("no-cache")
                            if (index >= 0) noCache = true
                        } else if ("refresh" == name) {
                            var idx = content.indexOf(';')
                            val time = if (idx == -1) { // just the refresh time
                                content
                            } else content.substring(0, idx)

                            try {
                                refreshTime = time.toInt()
                                // skip this if we couldn't parse the time
                                refresh = true
                            } catch (e: Exception) {
                            }

                            var refreshUrl: URL? = null
                            if (refresh && idx != -1) { // set the URL
                                idx = content.toLowerCase().indexOf("url=")
                                if (idx == -1) {
                                    // assume a mis-formatted entry with just the url
                                    idx = content.indexOf(';') + 1
                                } else idx += 4
                                if (idx != -1) {
                                    val url = content.substring(idx)
                                    refreshUrl = try {
                                        URL(url)
                                    } catch (e: Exception) {
                                        // XXX according to the spec, this has to be an absolute
                                        // XXX url. However, many websites use relative URLs and
                                        // XXX expect browsers to handle that.
                                        // XXX Unfortunately, in some cases this may create a
                                        // XXX infinitely recursive paths (a crawler trap)...
                                        // if (!url.startsWith("/")) url = "/" + url;
                                        try {
                                            URL(currURL, url)
                                        } catch (e1: Exception) {
                                            null
                                        }
                                    }
                                }
                            }
                            if (refresh) {
                                if (refreshUrl == null) {
                                    // apparently only refresh time was present. set the URL
                                    // to the same URL.
                                    refreshUrl = currURL
                                }
                                refreshHref = refreshUrl
                            }
                        }
                    }
                }
            } else if ("base".equals(node.nodeName, ignoreCase = true)) {
                val attrs = node.attributes
                val hrefNode = attrs.getNamedItem("href")
                if (hrefNode != null) {
                    val urlString = hrefNode.nodeValue
                    var url: URL? = null
                    try {
                        url = currURL?.let { URL(it, urlString) } ?: URL(urlString)
                    } catch (ignored: Exception) {
                    }
                    if (url != null) baseHref = url
                }
            }
        }
        val children = node.childNodes
        if (children != null) {
            val len = children.length
            for (i in 0 until len) {
                walk(children.item(i))
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("base=$baseHref, noCache=$noCache, noFollow=$noFollow, noIndex=$noIndex, refresh=$refresh, refreshHref=$refreshHref")
        sb.append(" * general tags:\n")
        generalTags.names().forEach { name ->
            sb.append("   - " + name + "\t=\t" + generalTags[name] + "\n")
        }
        sb.append(" * http-equiv tags:\n")
        httpEquivTags.keys.forEach { o ->
            val key = o as String
            sb.append("   - " + key + "\t=\t" + httpEquivTags[key] + "\n")
        }
        return sb.toString()
    }

    init {
        walk(root)
    }
}