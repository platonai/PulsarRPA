/*
 * XXX ab@apache.org: This class is copied verbatim from Xalan-J 2.6.0
 * XXX distribution, org.apache.xml.utils.DOMBuilder, in order to
 * avoid dependency on Xalan.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: DOMBuilder.java 823614 2009-10-09 17:02:32Z ab $
 */
package ai.platon.pulsar.parse.tika

import org.w3c.dom.*
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.ext.LexicalHandler
import java.io.Writer
import java.util.*

/**
 * This class takes SAX events (in addition to some extra events that SAX
 * doesn't handle yet) and adds the result to a document or document fragment.
 */
internal class DOMBuilder : ContentHandler, LexicalHandler {
    /**
     * Root document
     */
    var m_doc: Document
    /**
     * First node of document fragment or null if not a DocumentFragment
     */
    var m_docFrag: DocumentFragment? = null
    /**
     * Get the node currently being processed.
     *
     * @return the current node being processed
     */
    /**
     * Current node
     */
    var currentNode: Node? = null
        protected set
    /**
     * Vector of element nodes
     */
    protected var m_elemStack = Stack<Element>()
    /**
     * Flag indicating that we are processing a CData section
     */
    protected var m_inCData = false

    /**
     * DOMBuilder instance constructor... it will add the DOM nodes to the
     * document fragment.
     *
     * @param doc  Root document
     * @param node Current node
     */
    constructor(doc: Document, node: Node?) {
        m_doc = doc
        currentNode = node
    }

    /**
     * DOMBuilder instance constructor... it will add the DOM nodes to the
     * document fragment.
     *
     * @param doc     Root document
     * @param docFrag Document fragment
     */
    constructor(doc: Document, docFrag: DocumentFragment?) {
        m_doc = doc
        m_docFrag = docFrag
    }

    /**
     * DOMBuilder instance constructor... it will add the DOM nodes to the
     * document.
     *
     * @param doc Root document
     */
    constructor(doc: Document) {
        m_doc = doc
    }

    /**
     * Get the root node of the DOM being created. This is either a Document or a
     * DocumentFragment.
     *
     * @return The root document or document fragment if not null
     */
    val rootNode: Node
        get() = if (null != m_docFrag) m_docFrag!! else m_doc

    /**
     * Return null since there is no Writer for this class.
     *
     * @return null
     */
    val writer: Writer?
        get() = null

    /**
     * Append a node to the current container.
     *
     * @param newNode New node to append
     */
    @Throws(SAXException::class)
    protected fun append(newNode: Node) {
        val currentNode = currentNode
        if (null != currentNode) {
            currentNode.appendChild(newNode)
            // System.out.println(newNode.getNodeName());
        } else if (null != m_docFrag) {
            m_docFrag!!.appendChild(newNode)
        } else {
            var ok = true
            val type = newNode.nodeType
            if (type == Node.TEXT_NODE) {
                val data = newNode.nodeValue
                if (null != data && data.trim { it <= ' ' }.length > 0) {
                    throw SAXException(
                            "Warning: can't output text before document element!  Ignoring...")
                }
                ok = false
            } else if (type == Node.ELEMENT_NODE) {
                if (m_doc.documentElement != null) {
                    throw SAXException(
                            "Can't have more than one root on a DOM!")
                }
            }
            if (ok) m_doc.appendChild(newNode)
        }
    }

    /**
     * Receive an object for locating the origin of SAX document events.
     *
     *
     *
     *
     * SAX parsers are strongly encouraged (though not absolutely required) to
     * supply a locator: if it does so, it must supply the locator to the
     * application by invoking this method before invoking any of the other
     * methods in the ContentHandler interface.
     *
     *
     *
     *
     *
     * The locator allows the application to determine the end position of any
     * document-related event, even if the parser is not reporting an error.
     * Typically, the application will use this information for reporting its own
     * errors (such as character content that does not match an application's
     * business rules). The information returned by the locator is probably not
     * sufficient for use with a search engine.
     *
     *
     *
     *
     *
     * Note that the locator will return correct information only during the
     * invocation of the events in this interface. The application should not
     * attempt to use it at any other time.
     *
     *
     * @param locator An object that can return the location of any SAX document event.
     * @see org.xml.sax.Locator
     */
    override fun setDocumentLocator(locator: Locator) { // No action for the moment.
    }

    /**
     * Receive notification of the beginning of a document.
     *
     *
     *
     *
     * The SAX parser will invoke this method only once, before any other methods
     * in this interface or in DTDHandler (except for setDocumentLocator).
     *
     */
    @Throws(SAXException::class)
    override fun startDocument() { // No action for the moment.
    }

    /**
     * Receive notification of the end of a document.
     *
     *
     *
     *
     * The SAX parser will invoke this method only once, and it will be the last
     * method invoked during the parse. The parser shall not invoke this method
     * until it has either abandoned parsing (because of an unrecoverable error)
     * or reached the end of input.
     *
     */
    @Throws(SAXException::class)
    override fun endDocument() { // No action for the moment.
    }

    /**
     * Receive notification of the beginning of an element.
     *
     *
     *
     *
     * The Parser will invoke this method at the beginning of every element in the
     * XML document; there will be a corresponding endElement() event for every
     * startElement() event (even when the element is empty). All of the element's
     * content will be reported, in order, before the corresponding endElement()
     * event.
     *
     *
     *
     *
     *
     * If the element name has a namespace prefix, the prefix will still be
     * attached. Note that the attribute list provided will contain only
     * attributes with explicit values (specified or defaulted): #IMPLIED
     * attributes will be omitted.
     *
     *
     * @param ns        The namespace of the node
     * @param localName The local part of the qualified name
     * @param name      The element name.
     * @param atts      The attributes attached to the element, if any.
     * @see .endElement
     *
     * @see org.xml.sax.Attributes
     */
    @Throws(SAXException::class)
    override fun startElement(ns: String, localName: String, name: String,
                              atts: Attributes) {
        val elem: Element
        // Note that the namespace-aware call must be used to correctly
// construct a Level 2 DOM, even for non-namespaced nodes.
        elem = if (null == ns || ns.length == 0) m_doc.createElementNS(null, name) else m_doc.createElementNS(ns, name)
        append(elem)
        try {
            val nAtts = atts.length
            if (0 != nAtts) {
                for (i in 0 until nAtts) { // System.out.println("type " + atts.getType(i) + " name " +
// atts.getLocalName(i) );
// First handle a possible ID attribute
                    if (atts.getType(i).equals("ID", ignoreCase = true)) setIDAttribute(atts.getValue(i), elem)
                    var attrNS = atts.getURI(i)
                    if ("" == attrNS) attrNS = null // DOM represents no-namespace as null
                    // System.out.println("attrNS: "+attrNS+", localName: "+atts.getQName(i)
// +", qname: "+atts.getQName(i)+", value: "+atts.getValue(i));
// Crimson won't let us set an xmlns: attribute on the DOM.
                    val attrQName = atts.getQName(i)
                    // In SAX, xmlns: attributes have an empty namespace, while in DOM
// they should have the xmlns namespace
                    if (attrQName.startsWith("xmlns:")) attrNS = "http://www.w3.org/2000/xmlns/"
                    // ALWAYS use the DOM Level 2 call!
                    elem.setAttributeNS(attrNS, attrQName, atts.getValue(i))
                }
            }
            // append(elem);
            m_elemStack.push(elem)
            currentNode = elem
            // append(elem);
        } catch (de: Exception) { // de.printStackTrace();
            throw SAXException(de)
        }
    }

    /**
     * Receive notification of the end of an element.
     *
     *
     *
     *
     * The SAX parser will invoke this method at the end of every element in the
     * XML document; there will be a corresponding startElement() event for every
     * endElement() event (even when the element is empty).
     *
     *
     *
     *
     *
     * If the element name has a namespace prefix, the prefix will still be
     * attached to the name.
     *
     *
     * @param ns        the namespace of the element
     * @param localName The local part of the qualified name of the element
     * @param name      The element name
     */
    @Throws(SAXException::class)
    override fun endElement(ns: String, localName: String, name: String) {
        m_elemStack.pop()
        currentNode = if (m_elemStack.isEmpty()) null else m_elemStack.peek() as Node
    }

    /**
     * Set an ID string to node association in the ID table.
     *
     * @param id   The ID string.
     * @param elem The associated ID.
     */
    fun setIDAttribute(id: String?, elem: Element?) { // Do nothing. This method is meant to be overiden.
    }

    /**
     * Receive notification of character data.
     *
     *
     *
     *
     * The Parser will call this method to report each chunk of character data.
     * SAX parsers may return all contiguous character data in a single chunk, or
     * they may split it into several chunks; however, all of the characters in
     * any single event must come from the same external entity, so that the
     * Locator provides useful information.
     *
     *
     *
     *
     *
     * The application must not attempt to read from the array outside of the
     * specified range.
     *
     *
     *
     *
     *
     * Note that some parsers will report whitespace using the
     * ignorableWhitespace() method rather than this one (validating parsers must
     * do so).
     *
     *
     * @param ch     The characters from the XML document.
     * @param start  The start position in the array.
     * @param length The number of characters to read from the array.
     * @see .ignorableWhitespace
     *
     * @see org.xml.sax.Locator
     */
    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (isOutsideDocElem
                && XMLCharacterRecognizer.isWhiteSpace(ch, start, length)) return  // avoid DOM006 Hierarchy request error
        if (m_inCData) {
            cdata(ch, start, length)
            return
        }
        val s = String(ch, start, length)
        val childNode: Node?
        childNode = if (currentNode != null) currentNode!!.lastChild else null
        if (childNode != null && childNode.nodeType == Node.TEXT_NODE) {
            (childNode as Text).appendData(s)
        } else {
            val text = m_doc.createTextNode(s)
            append(text)
        }
    }

    /**
     * If available, when the disable-output-escaping attribute is used, output
     * raw text without escaping. A PI will be inserted in front of the node with
     * the name "lotusxsl-next-is-raw" and a value of "formatter-to-dom".
     *
     * @param ch     Array containing the characters
     * @param start  Index to start of characters in the array
     * @param length Number of characters in the array
     */
    @Throws(SAXException::class)
    fun charactersRaw(ch: CharArray, start: Int, length: Int) {
        if (isOutsideDocElem
                && XMLCharacterRecognizer.isWhiteSpace(ch, start, length)) return  // avoid DOM006 Hierarchy request error
        val s = String(ch, start, length)
        append(m_doc.createProcessingInstruction("xslt-next-is-raw",
                "formatter-to-dom"))
        append(m_doc.createTextNode(s))
    }

    /**
     * Report the beginning of an entity.
     *
     *
     * The start and end of the document entity are not reported. The start and
     * end of the external DTD subset are reported using the pseudo-name "[dtd]".
     * All other events must be properly nested within start/end entity events.
     *
     * @param name The name of the entity. If it is a parameter entity, the name will
     * begin with '%'.
     * @see .endEntity
     *
     * @see org.xml.sax.ext.DeclHandler.internalEntityDecl
     *
     * @see org.xml.sax.ext.DeclHandler.externalEntityDecl
     */
    @Throws(SAXException::class)
    override fun startEntity(name: String) { // Almost certainly the wrong behavior...
// entityReference(name);
    }

    /**
     * Report the end of an entity.
     *
     * @param name The name of the entity that is ending.
     * @see .startEntity
     */
    @Throws(SAXException::class)
    override fun endEntity(name: String) {
    }

    /**
     * Receive notivication of a entityReference.
     *
     * @param name name of the entity reference
     */
    @Throws(SAXException::class)
    fun entityReference(name: String?) {
        append(m_doc.createEntityReference(name))
    }

    /**
     * Receive notification of ignorable whitespace in element content.
     *
     *
     *
     *
     * Validating Parsers must use this method to report each chunk of ignorable
     * whitespace (see the W3C XML 1.0 recommendation, section 2.10):
     * non-validating parsers may also use this method if they are capable of
     * parsing and using content models.
     *
     *
     *
     *
     *
     * SAX parsers may return all contiguous whitespace in a single chunk, or they
     * may split it into several chunks; however, all of the characters in any
     * single event must come from the same external entity, so that the Locator
     * provides useful information.
     *
     *
     *
     *
     *
     * The application must not attempt to read from the array outside of the
     * specified range.
     *
     *
     * @param ch     The characters from the XML document.
     * @param start  The start position in the array.
     * @param length The number of characters to read from the array.
     * @see .characters
     */
    @Throws(SAXException::class)
    override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) {
        if (isOutsideDocElem) return  // avoid DOM006 Hierarchy request error
        val s = String(ch, start, length)
        append(m_doc.createTextNode(s))
    }

    /**
     * Tell if the current node is outside the document element.
     *
     * @return true if the current node is outside the document element.
     */
    private val isOutsideDocElem: Boolean
        private get() = (null == m_docFrag
                && m_elemStack.size == 0 && (null == currentNode || currentNode!!.nodeType == Node.DOCUMENT_NODE))

    /**
     * Receive notification of a processing instruction.
     *
     *
     *
     *
     * The Parser will invoke this method once for each processing instruction
     * found: note that processing instructions may occur before or after the main
     * document element.
     *
     *
     *
     *
     *
     * A SAX parser should never report an XML declaration (XML 1.0, section 2.8)
     * or a text declaration (XML 1.0, section 4.3.1) using this method.
     *
     *
     * @param target The processing instruction target.
     * @param data   The processing instruction data, or null if none was supplied.
     */
    @Throws(SAXException::class)
    override fun processingInstruction(target: String, data: String) {
        append(m_doc.createProcessingInstruction(target, data))
    }

    /**
     * Report an XML comment anywhere in the document.
     *
     *
     * This callback will be used for comments inside or outside the document
     * element, including comments in the external DTD subset (if read).
     *
     * @param ch     An array holding the characters in the comment.
     * @param start  The starting position in the array.
     * @param length The number of characters to use from the array.
     */
    @Throws(SAXException::class)
    override fun comment(ch: CharArray, start: Int, length: Int) { // tagsoup sometimes submits invalid values here
        if (ch == null || start < 0 || length >= ch.size - start || length < 0) return
        append(m_doc.createComment(String(ch, start, length)))
    }

    /**
     * Report the start of a CDATA section.
     *
     * @see .endCDATA
     */
    @Throws(SAXException::class)
    override fun startCDATA() {
        m_inCData = true
        append(m_doc.createCDATASection(""))
    }

    /**
     * Report the end of a CDATA section.
     *
     * @see .startCDATA
     */
    @Throws(SAXException::class)
    override fun endCDATA() {
        m_inCData = false
    }

    /**
     * Receive notification of cdata.
     *
     *
     *
     *
     * The Parser will call this method to report each chunk of character data.
     * SAX parsers may return all contiguous character data in a single chunk, or
     * they may split it into several chunks; however, all of the characters in
     * any single event must come from the same external entity, so that the
     * Locator provides useful information.
     *
     *
     *
     *
     *
     * The application must not attempt to read from the array outside of the
     * specified range.
     *
     *
     *
     *
     *
     * Note that some parsers will report whitespace using the
     * ignorableWhitespace() method rather than this one (validating parsers must
     * do so).
     *
     *
     * @param ch     The characters from the XML document.
     * @param start  The start position in the array.
     * @param length The number of characters to read from the array.
     * @see .ignorableWhitespace
     *
     * @see org.xml.sax.Locator
     */
    @Throws(SAXException::class)
    fun cdata(ch: CharArray, start: Int, length: Int) {
        if (isOutsideDocElem
                && XMLCharacterRecognizer.isWhiteSpace(ch, start, length)) return  // avoid DOM006 Hierarchy request error
        val s = String(ch, start, length)
        // XXX ab@apache.org: modified from the original, to accomodate TagSoup.
        val n = currentNode!!.lastChild
        if (n is CDATASection) n.appendData(s) else if (n is Comment) n.appendData(s)
    }

    /**
     * Report the start of DTD declarations, if any.
     *
     *
     * Any declarations are assumed to be in the internal subset unless otherwise
     * indicated.
     *
     * @param name     The document type name.
     * @param publicId The declared public identifier for the external DTD subset, or
     * null if none was declared.
     * @param systemId The declared system identifier for the external DTD subset, or
     * null if none was declared.
     * @see .endDTD
     *
     * @see .startEntity
     */
    @Throws(SAXException::class)
    override fun startDTD(name: String, publicId: String, systemId: String) { // Do nothing for now.
    }

    /**
     * Report the end of DTD declarations.
     *
     * @see .startDTD
     */
    @Throws(SAXException::class)
    override fun endDTD() { // Do nothing for now.
    }

    /**
     * Begin the scope of a prefix-URI Namespace mapping.
     *
     *
     *
     *
     * The information from this event is not necessary for normal Namespace
     * processing: the SAX XML reader will automatically replace prefixes for
     * element and attribute names when the http://xml.org/sax/features/namespaces
     * feature is true (the default).
     *
     *
     *
     *
     *
     * There are cases, however, when applications need to use prefixes in
     * character data or in attribute values, where they cannot safely be expanded
     * automatically; the start/endPrefixMapping event supplies the information to
     * the application to expand prefixes in those contexts itself, if necessary.
     *
     *
     *
     *
     *
     * Note that start/endPrefixMapping events are not guaranteed to be properly
     * nested relative to each-other: all startPrefixMapping events will occur
     * before the corresponding startElement event, and all endPrefixMapping
     * events will occur after the corresponding endElement event, but their order
     * is not guaranteed.
     *
     *
     * @param prefix The Namespace prefix being declared.
     * @param uri    The Namespace URI the prefix is mapped to.
     * @see .endPrefixMapping
     *
     * @see .startElement
     */
    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String, uri: String) { /*
     * // Not sure if this is needed or wanted // Also, it fails in the stree.
     * if((null != m_currentNode) && (m_currentNode.getNodeType() ==
     * Node.ELEMENT_NODE)) { String qname; if(((null != prefix) &&
     * (prefix.length() == 0)) || (null == prefix)) qname = "xmlns"; else qname
     * = "xmlns:"+prefix;
     *
     * Element elem = (Element)m_currentNode; String val =
     * elem.getAttribute(qname); // Obsolete, should be DOM2...? if(val == null)
     * { elem.setAttributeNS("http://www.w3.org/XML/1998/namespace", qname,
     * uri); } }
     */
    }

    /**
     * End the scope of a prefix-URI mapping.
     *
     *
     *
     *
     * See startPrefixMapping for details. This event will always occur after the
     * corresponding endElement event, but the order of endPrefixMapping events is
     * not otherwise guaranteed.
     *
     *
     * @param prefix The prefix that was being mapping.
     * @see .startPrefixMapping
     *
     * @see .endElement
     */
    @Throws(SAXException::class)
    override fun endPrefixMapping(prefix: String) {
    }

    /**
     * Receive notification of a skipped entity.
     *
     *
     *
     *
     * The Parser will invoke this method once for each entity skipped.
     * Non-validating processors may skip entities if they have not seen the
     * declarations (because, for example, the entity was declared in an external
     * DTD subset). All processors may skip external entities, depending on the
     * values of the http://xml.org/sax/features/external-general-entities and the
     * http://xml.org/sax/features/external-parameter-entities properties.
     *
     *
     * @param name The name of the skipped entity. If it is a parameter entity, the
     * name will begin with '%'.
     */
    @Throws(SAXException::class)
    override fun skippedEntity(name: String) {
    }
}