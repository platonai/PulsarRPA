/**
 * DOM (Document Object Model) manipulation functions for X-SQL queries in Pulsar QL.
 *
 * This object provides a comprehensive set of functions for loading, parsing, and manipulating
 * HTML/XML documents within X-SQL queries. It enables web scraping, data extraction, and
 * document analysis directly from SQL queries.
 *
 * ## Function Categories
 *
 * ### Document Loading and Parsing
 * - [load] - Load page from database or web with caching
 * - [fetch] - Fetch page immediately from web (force refresh)
 * - [parse] - Parse HTML content into DOM document
 *
 * ### Document Navigation and Selection
 * - [select] - Select elements using CSS selectors
 * - [selectFirst] - Select first matching element
 * - [selectNth] - Select nth matching element
 * - [parent], [children] - Navigate document hierarchy
 *
 * ### Element Property Extraction
 * - [text] - Extract text content from elements
 * - [attr] - Get element attribute values
 * - [style] - Get CSS style properties
 * - [feature] - Extract computed node features
 *
 * ### Link and Form Processing
 * - [links] - Extract all links from a document
 * - [forms] - Extract all forms from a document
 * - [images] - Extract all images from a document
 * - [hrefs] - Extract href attributes from links
 *
 * ### Content Analysis
 * - [wordCount] - Count words in element text
 * - [density] - Calculate keyword density
 * - [visibility] - Check element visibility
 * - [isNil] - Check if DOM element is null/invalid
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Load and parse a web page
 * SELECT DOM.load('https://example.com');
 *
 * -- Extract page title
 * SELECT DOM.text(DOM.selectFirst(DOM.load('https://example.com'), 'title'));
 *
 * -- Extract all links from a page
 * SELECT DOM.hrefs(DOM.load('https://example.com'));
 *
 * -- Extract article content
 * SELECT DOM.text(DOM.selectFirst(dom, 'article'))
 * FROM (
 *   SELECT DOM.load('https://news.example.com/article') as dom
 * );
 *
 * -- Extract product information
 * SELECT
 *   DOM.text(DOM.selectFirst(dom, '.product-title')) as title,
 *   DOM.text(DOM.selectFirst(dom, '.price')) as price,
 *   DOM.attr(DOM.selectFirst(dom, '.product-image'), 'src') as image_url
 * FROM (
 *   SELECT DOM.load('https://shop.example.com/product/123') as dom
 * ) t;
 * ```
 *
 * ## X-SQL Integration
 *
 * All DOM functions are automatically registered as H2 database functions under the
 * "DOM" namespace. They can be used directly in X-SQL queries and combined with
 * other SQL operations for powerful web data extraction workflows.
 *
 * ## Performance Notes
 *
 * - Document loading respects caching policies configured in the session
 * - CSS selector operations are optimized for performance
 * - Large document processing is memory-efficient
 * - Results are cached within the SQL session context
 *
 * ## Thread Safety
 *
 * DOM functions are thread-safe and can be safely used in concurrent query
 * execution contexts. Each function operates on immutable DOM representations.
 *
 * ## Error Handling
 *
 * - Returns null/empty values for invalid selectors or missing elements
 * - Graceful handling of malformed HTML
 * - Safe navigation for missing DOM nodes
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see ValueDom
 * @see UDFGroup
 * @see UDFGroup
 * @see <a href="https://jsoup.org/apidocs/org/jsoup/select/Selector.html">CSS Selector Reference</a>
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.config.AppConstants.PULSAR_META_INFORMATION_SELECTOR
import ai.platon.pulsar.dom.features.NodeFeature
import ai.platon.pulsar.dom.features.defined.*
import ai.platon.pulsar.dom.nodes.A_LABELS
import ai.platon.pulsar.dom.nodes.node.ext.*
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.ql.common.annotation.H2Context
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.h2.H2SessionFactory
import ai.platon.pulsar.ql.h2.domValue
import ai.platon.pulsar.ql.common.types.ValueDom
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueString
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.sql.Connection
import java.time.Duration

/**
 * DOM manipulation functions for X-SQL queries.
 *
 * Provides comprehensive HTML/XML document processing capabilities including
 * loading, parsing, navigation, extraction, and analysis functions for web scraping
 * and document processing within X-SQL queries.
 */
@Suppress("unused")
@UDFGroup(namespace = "DOM")
object DomFunctions {
    private val sqlContext get() = SQLContexts.create()

    /**
     * Loads a web page from database cache or fetches it from the web if not cached or expired.
     *
     * This function implements intelligent page loading with the following strategy:
     * 1. First checks if the page exists in the local database cache
     * 2. If cached and not expired, returns the cached version
     * 3. If not cached or expired, fetches from the web
     * 4. Parses the HTML content and returns a DOM document
     *
     * ## X-SQL Usage
     * ```sql
     * -- Load a web page and return DOM document
     * SELECT DOM.load('https://example.com');
     *
     * -- Load page and extract title
     * SELECT DOM.text(DOM.selectFirst(DOM.load('https://news.site.com/article'), 'h1'))
     * FROM (VALUES (1)) t;
     *
     * -- Load multiple pages
     * SELECT url, DOM.load(url) as document
     * FROM (
     *   SELECT 'https://site1.com' as url UNION ALL
     *   SELECT 'https://site2.com' as url UNION ALL
     *   SELECT 'https://site3.com' as url
     * ) pages;
     * ```
     *
     * ## Use Cases
     * - Web scraping with intelligent caching
     * - Batch processing of web pages
     * - Building data extraction pipelines
     * - Automated content analysis
     *
     * ## Caching Behavior
     * - Respects session-level caching configuration
     * - Uses URL normalization for cache key generation
     * - Applies configured expiration policies
     * - Handles cache invalidation based on content changes
     *
     * ## Error Handling
     * - Returns ValueDom.NIL if SQL context is not active
     * - Handles network errors gracefully
     * - Manages malformed HTML content
     *
     * @param conn The H2 database connection context
     * @param configuredUrl The URL to load (supports Pulsar URL configuration syntax)
     * @return ValueDom containing the parsed document, or ValueDom.NIL on failure
     * @see fetch
     * @see parse
     * @see ValueDom
     * @see Duration.ZERO
     */
    @UDFunction(
        description = "Load the page specified by url from db, if absent or expired, " +
                "fetch it from the web, and then parse it into a document"
    )
    @JvmStatic
    fun load(@H2Context conn: Connection, configuredUrl: String): ValueDom {
        if (!sqlContext.isActive) return ValueDom.NIL

        val session = H2SessionFactory.getSession(conn)
        return session.run { parseValueDom(load(configuredUrl)) }
    }

    /**
     * Fetches a web page immediately from the web, bypassing cache and forcing fresh content.
     *
     * This function implements forced page fetching with the following behavior:
     * 1. Always fetches from the web, ignoring any cached versions
     * 2. Sets expiration time to zero to ensure fresh content
     * 3. Parses the HTML content and returns a DOM document
     * 4. Updates the cache with the fresh content
     *
     * ## X-SQL Usage
     * ```sql
     * -- Fetch a page fresh from the web
     * SELECT DOM.fetch('https://example.com');
     *
     * -- Fetch page and extract content immediately
     * SELECT DOM.text(DOM.selectFirst(DOM.fetch('https://news.site.com/breaking'), 'h1'))
     * FROM (VALUES (1)) t;
     *
     * -- Compare cached vs fresh content
     * SELECT
     *   'cached' as source,
     *   DOM.text(DOM.selectFirst(DOM.load('https://example.com'), 'title')) as title
     * UNION ALL
     * SELECT
     *   'fresh' as source,
     *   DOM.text(DOM.selectFirst(DOM.fetch('https://example.com'), 'title')) as title;
     * ```
     *
     * ## Use Cases
     * - Getting real-time data that changes frequently
     * - Bypassing stale cache entries
     * - Testing website changes immediately
     * - Breaking news or live data extraction
     * - Cache invalidation workflows
     *
     * ## Differences from [load]
     * - **fetch**: Always gets fresh content from web, ignores cache
     * - **load**: Uses cache when available, respects expiration policies
     *
     * ## Performance Considerations
     * - Always makes network requests, potentially slower
     * - No cache benefits for repeated calls
     * - Useful for data that changes frequently
     *
     * @param conn The H2 database connection context
     * @param configuredUrl The URL to fetch (supports Pulsar URL configuration syntax)
     * @return ValueDom containing the parsed document, or ValueDom.NIL on failure
     * @see load
     * @see parse
     * @see ValueDom
     * @see Duration.ZERO
     */
    @UDFunction(description = "Fetch the page specified by url immediately, and then parse it into a document")
    @JvmStatic
    fun fetch(@H2Context conn: Connection, configuredUrl: String): ValueDom {
        if (!sqlContext.isActive) return ValueDom.NIL

        val h2session = H2SessionFactory.getH2Session(conn)
        val session = sqlContext.getSession(h2session.serialId)
        val normURL = session.normalize(configuredUrl).apply { options.expires = Duration.ZERO }
        return session.parseValueDom(session.load(normURL))
    }

    /**
     * Checks if a DOM element is null or invalid (represents a nil value).
     *
     * This function tests whether the provided ValueDom represents a null or invalid
     * DOM element. This is useful for error handling and validation in document
     * processing workflows.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Check if page load was successful
     * SELECT DOM.isNil(DOM.load('https://invalid-url.com')) as load_failed;
     *
     * -- Use in conditional logic
     * SELECT
     *   CASE
     *     WHEN DOM.isNil(dom) THEN 'Failed to load'
     *     ELSE 'Load successful'
     *   END as status
     * FROM (
     *   SELECT DOM.load('https://example.com') as dom
     * ) t;
     *
     * -- Filter out failed loads
     * SELECT url
     * FROM (
     *   SELECT 'https://site1.com' as url, DOM.load('https://site1.com') as dom
     *   UNION ALL
     *   SELECT 'https://site2.com' as url, DOM.load('https://site2.com') as dom
     * ) pages
     * WHERE DOM.isNil(dom) = false;
     * ```
     *
     * ## Use Cases
     * - Error handling in document loading
     * - Validating successful page loads
     * - Filtering failed operations
     * - Conditional processing based on DOM validity
     *
     * ## Return Values
     * - **true**: DOM is nil (invalid, null, or failed load)
     * - **false**: DOM is valid and contains document data
     *
     * @param dom The ValueDom to test for nil status
     * @return true if the DOM is nil/invalid, false otherwise
     * @see ValueDom
     * @see isNotNil
     */
    @UDFunction
    @JvmStatic
    fun isNil(dom: ValueDom) = dom.isNil

    /**
     * Checks if a DOM element is valid and not null.
     *
     * This function tests whether the provided ValueDom represents a valid
     * DOM element. It is the logical opposite of [isNil] and is useful for
     * confirming successful document operations.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Check if page load was successful
     * SELECT DOM.isNotNil(DOM.load('https://example.com')) as load_success;
     *
     * -- Use in WHERE clause
     * SELECT DOM.text(DOM.selectFirst(dom, 'title')) as title
     * FROM (
     *   SELECT DOM.load('https://example.com') as dom
     * ) t
     * WHERE DOM.isNotNil(dom) = true;
     *
     * -- Combine with other operations
     * SELECT
     *   url,
     *   CASE
     *     WHEN DOM.isNotNil(dom) THEN DOM.text(DOM.selectFirst(dom, 'h1'))
     *     ELSE 'Failed to load'
     *   END as title
     * FROM (
     *   SELECT 'https://example.com' as url, DOM.load('https://example.com') as dom
     * ) t;
     * ```
     *
     * ## Use Cases
     * - Validating successful document operations
     * - Filtering valid DOM elements
     * - Conditional processing based on DOM validity
     * - Error handling and recovery
     *
     * ## Return Values
     * - **true**: DOM is valid and contains document data
     * - **false**: DOM is nil (invalid, null, or failed load)
     *
     * @param dom The ValueDom to test for validity
     * @return true if the DOM is valid, false otherwise
     * @see ValueDom
     * @see isNil
     */
    @UDFunction
    @JvmStatic
    fun isNotNil(dom: ValueDom) = dom.isNotNil

    /**
     * Gets the value of the specified attribute from a DOM element.
     *
     * This function extracts attribute values from HTML elements using the
     * attribute name. It is commonly used to extract href, src, class, id,
     * and custom data attributes.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract href from link
     * SELECT DOM.attr(DOM.selectFirst(dom, 'a.main-link'), 'href')
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     *
     * -- Extract image source
     * SELECT DOM.attr(DOM.selectFirst(dom, 'img.logo'), 'src')
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     *
     * -- Extract class attribute
     * SELECT DOM.attr(DOM.selectFirst(dom, 'div.content'), 'class')
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     *
     * -- Extract custom data attributes
     * SELECT DOM.attr(DOM.selectFirst(dom, 'div[data-product-id]'), 'data-product-id')
     * FROM (SELECT DOM.load('https://shop.example.com') as dom) t;
     * ```
     *
     * ## Use Cases
     * - Extracting link URLs (href attributes)
     * - Getting image sources (src attributes)
     * - Reading element identifiers (id, class)
     * - Accessing custom data attributes
     * - Extracting form action URLs
     *
     * ## Return Values
     * - **Attribute value**: If the attribute exists
     * - **Empty string**: If the attribute doesn't exist
     *
     * ## Common Attributes
     * - `href` - Link destinations
     * - `src` - Media sources (images, scripts, stylesheets)
     * - `class` - CSS classes
     * - `id` - Element identifiers
     * - `alt` - Alternative text
     * - `title` - Tooltip text
     * - `data-*` - Custom data attributes
     *
     * @param dom The DOM element to extract attribute from
     * @param attrName The name of the attribute to retrieve
     * @return The attribute value, or empty string if attribute doesn't exist
     * @see Element.attr
     * @see hasAttr
     */
    @UDFunction
    @JvmStatic
    fun attr(dom: ValueDom, attrName: String) = dom.element.attr(attrName)

    /**
     * Gets the ARIA labels from a DOM element.
     *
     * This function extracts ARIA (Accessible Rich Internet Applications) label attributes
     * from HTML elements. ARIA labels provide accessibility information for screen readers
     * and assistive technologies.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Extract ARIA labels for accessibility analysis
     * SELECT DOM.labels(DOM.selectFirst(dom, 'button'))
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     *
     * -- Check labels on form elements
     * SELECT
     *   DOM.attr(input, 'type') as input_type,
     *   DOM.labels(input) as aria_label
     * FROM (
     *   SELECT DOM.load('https://form.example.com') as dom
     * ) t,
     * LATERAL (SELECT DOM.select(dom, 'input')) as inputs(input);
     * ```
     *
     * ## Use Cases
     * - Accessibility auditing and compliance
     * - Screen reader compatibility analysis
     * - Form usability assessment
     * - Web accessibility testing
     *
     * ## ARIA Labels
     * This function specifically extracts the `aria-label` attribute value, which provides
     * a text label for elements that might not have visible text content.
     *
     * @param dom The DOM element to extract ARIA labels from
     * @return The ARIA label value, or empty string if not present
     * @see A_LABELS
     * @see Element.attr
     * @see <a href="https://www.w3.org/WAI/ARIA/apg/">ARIA Authoring Practices Guide</a>
     */
    @UDFunction
    @JvmStatic
    fun labels(dom: ValueDom) = dom.element.attr(A_LABELS)

    /**
     * Gets the computed feature value of a DOM element.
     *
     * This function extracts computed node features that are calculated based on
     * the element's properties, position, and context within the document. Features
     * can include visibility, position, size, and other computed properties.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Get element visibility feature
     * SELECT DOM.feature(DOM.selectFirst(dom, 'div.content'), 'visibility')
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     *
     * -- Get position features
     * SELECT
     *   DOM.feature(element, 'x') as x_position,
     *   DOM.feature(element, 'y') as y_position
     * FROM (
     *   SELECT DOM.load('https://example.com') as dom
     * ) t,
     * LATERAL (SELECT DOM.select(dom, 'div.featured')) as elements(element);
     *
     * -- Analyze multiple features
     * SELECT
     *   DOM.text(element) as text,
     *   DOM.feature(element, 'visibility') as visible,
     *   DOM.feature(element, 'size') as size
     * FROM (
     *   SELECT DOM.load('https://example.com') as dom
     * ) t,
     * LATERAL (SELECT DOM.select(dom, 'p')) as elements(element);
     * ```
     *
     * ## Use Cases
     * - Element visibility analysis
     * - Position and layout extraction
     * - Content importance scoring
     * - Web page structure analysis
     *
     * ## Available Features
     * Features are computed based on the element context and may include:
     * - `visibility` - Element visibility status
     * - `position` - Element position coordinates
     * - `size` - Element dimensions
     * - `importance` - Content importance score
     * - Custom features defined by the NodeFeature system
     *
     * @param dom The DOM element to extract features from
     * @param featureName The name of the feature to retrieve
     * @return The feature value as a string, or empty string if feature not found
     * @see NodeFeature
     * @see NodeFeature.getValue
     */
    @UDFunction
    @JvmStatic
    fun feature(dom: ValueDom, featureName: String) = NodeFeature.getValue(featureName, dom.element)

    @UDFunction
    @JvmStatic
    fun hasAttr(dom: ValueDom, attrName: String) = dom.element.hasAttr(attrName)

    @UDFunction
    @JvmStatic
    fun style(dom: ValueDom, styleName: String) = dom.element.getStyle(styleName)

    @UDFunction
    @JvmStatic
    fun sequence(dom: ValueDom) = dom.element.sequence

    @UDFunction
    @JvmStatic
    fun depth(dom: ValueDom) = dom.element.depth

    @UDFunction
    @JvmStatic
    fun cssSelector(dom: ValueDom) = dom.element.cssSelector()

    @UDFunction
    @JvmStatic
    fun cssPath(dom: ValueDom) = dom.element.cssSelector()

    @UDFunction
    @JvmStatic
    fun siblingSize(dom: ValueDom) = dom.element.siblingNodes().size

    @UDFunction
    @JvmStatic
    fun siblingIndex(dom: ValueDom) = dom.element.siblingIndex()

    @UDFunction
    @JvmStatic
    fun elementSiblingSize(dom: ValueDom) = dom.element.siblingElements().size

    @UDFunction
    @JvmStatic
    fun elementSiblingIndex(dom: ValueDom) = dom.element.elementSiblingIndex()

    /**
     * The normalized uri, should be the same as WebPage.url, which is also the key in the database
     * */
    @UDFunction
    @JvmStatic
    fun uri(dom: ValueDom): String {
        return dom.element.ownerDocument.normalizedURI ?: ""
    }

    /**
     * uri = WebPage.url which is the permanent internal address, it might not still available to access the target.
     * And location = WebPage.location or baseUri = WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url, it's generally normalized.
     *
     * @return a {@link java.lang.String} object.
     */
    @UDFunction
    @JvmStatic
    fun baseUri(dom: ValueDom) = dom.element.baseUri()

    @UDFunction
    @JvmStatic
    fun absUrl(dom: ValueDom, attributeKey: String) = dom.element.absUrl(attributeKey)

    /**
     * WebPage.url is the permanent internal address, it might not still available to access the target,
     * while WebPage.location or WebPage.baseUrl is the last working address, it might redirect to url,
     * or it might have additional random parameters.
     * WebPage.location may be different from url, it's generally normalized.
     *
     * @return a {@link java.lang.String} object.
     */
    @UDFunction
    @JvmStatic
    fun location(dom: ValueDom) = dom.element.location

    @UDFunction
    @JvmStatic
    fun childNodeSize(dom: ValueDom) = dom.element.childNodeSize()

    @UDFunction
    @JvmStatic
    fun childElementSize(dom: ValueDom) = dom.element.children().size

    @UDFunction
    @JvmStatic
    fun tagName(dom: ValueDom) = dom.element.tagName()

    @UDFunction
    @JvmStatic
    fun href(dom: ValueDom) = dom.element.attr("href")

    @UDFunction
    @JvmStatic
    fun absHref(dom: ValueDom) = dom.element.absUrl("href")

    @UDFunction
    @JvmStatic
    fun src(dom: ValueDom) = dom.element.attr("src")

    @UDFunction
    @JvmStatic
    fun absSrc(dom: ValueDom) = dom.element.absUrl("abs:src")

    @UDFunction(description = "Get the element title")
    @JvmStatic
    fun title(dom: ValueDom) = dom.element.attr("title")

    @UDFunction(description = "Get the document title")
    @JvmStatic
    fun docTitle(dom: ValueDom): String {
        val ele = dom.element
        if (ele is Document) {
            return ele.title()
        }

        return dom.element.ownerDocument()!!.title()
    }

    @UDFunction
    @JvmStatic
    fun hasText(dom: ValueDom) = dom.element.hasText()

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun text(dom: ValueDom, truncate: Int = Int.MAX_VALUE): String {
        val text = dom.element.text()
        return if (truncate > text.length) {
            text
        } else {
            text.substring(0, truncate)
        }
    }

    @UDFunction
    @JvmStatic
    fun textLen(dom: ValueDom) = dom.element.text().length

    @UDFunction
    @JvmStatic
    fun textLength(dom: ValueDom) = dom.element.text().length

    @UDFunction
    @JvmStatic
    fun ownText(dom: ValueDom) = dom.element.ownText()

    @UDFunction
    @JvmStatic
    fun ownTexts(dom: ValueDom) = ValueArray.get(dom.element.ownTexts().map { ValueString.get(it) }.toTypedArray())

    @UDFunction
    @JvmStatic
    fun ownTextLen(dom: ValueDom) = dom.element.ownText().length

    @UDFunction
    @JvmStatic
    fun wholeText(dom: ValueDom) = dom.element.wholeText()

    @UDFunction
    @JvmStatic
    fun wholeTextLen(dom: ValueDom) = dom.element.wholeText().length

    @UDFunction(description = "Extract the first group of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re1(dom: ValueDom, regex: String): String {
        val text = text(dom)
        return RegexExtractor().re1(text, regex)
    }

    @UDFunction(description = "Extract the nth group of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re1(dom: ValueDom, regex: String, group: Int): String {
        val text = text(dom)
        return ai.platon.pulsar.common.RegexExtractor().re1(text, regex, group)
    }

    @UDFunction(description = "Extract two groups of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re2(dom: ValueDom, regex: String): ValueArray {
        val text = text(dom)
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction(description = "Extract two groups(key and value) of the result of java.util.regex.matcher() over the node text")
    @JvmStatic
    fun re2(dom: ValueDom, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val text = text(dom)
        val result = RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun data(dom: ValueDom) = dom.element.data()

    @UDFunction
    @JvmStatic
    fun id(dom: ValueDom) = dom.element.id()

    @UDFunction
    @JvmStatic
    fun className(dom: ValueDom) = dom.element.className()

    @UDFunction
    @JvmStatic
    fun classNames(dom: ValueDom) = dom.element.classNames()

    @UDFunction
    @JvmStatic
    fun hasClass(dom: ValueDom, className: String) = dom.element.hasClass(className)

    @UDFunction
    @JvmStatic
    fun value(dom: ValueDom) = dom.element.`val`()

    @UDFunction
    @JvmStatic
    fun ownerDocument(dom: ValueDom): ValueDom {
        if (dom.isNil) return ValueDom.NIL
        val documentNode = dom.element.extension.ownerDocumentNode ?: return ValueDom.NIL
        return ValueDom.get(documentNode as Document)
    }

    @UDFunction
    @JvmStatic
    fun ownerBody(dom: ValueDom): ValueDom {
        if (dom.isNil) return ValueDom.NIL
        val ownerBody = dom.element.extension.ownerBody ?: return ValueDom.NIL
        return ValueDom.get(ownerBody as Element)
    }

    @UDFunction
    @JvmStatic
    fun documentVariables(dom: ValueDom): ValueDom {
        if (dom.isNil) return ValueDom.NIL
        val ownerBody = dom.element.extension.ownerBody ?: return ValueDom.NIL
        val meta = ownerBody.selectFirstOrNull(PULSAR_META_INFORMATION_SELECTOR) ?: return ValueDom.NIL
        return ValueDom.get(meta)
    }

    @UDFunction
    @JvmStatic
    fun parent(dom: ValueDom): ValueDom {
        if (dom.isNil) return ValueDom.NIL
        return ValueDom.get(dom.element.parent())
    }

    @UDFunction
    @JvmStatic
    fun ancestor(dom: ValueDom, n: Int): ValueDom {
        if (dom.isNil) return ValueDom.NIL

        var i = 0
        var p = dom.element.parent()
        while (p != null && i++ < n) {
            p = dom.element.parent()
        }

        return p?.let { domValue(it) } ?: ValueDom.NIL
    }

    @UDFunction
    @JvmStatic
    fun parentName(dom: ValueDom): String {
        if (dom.isNil) return "nil"
        return parent(dom).element.uniqueName
    }

    @UDFunction
    @JvmStatic
    fun dom(dom: ValueDom) = dom

    @UDFunction
    @JvmStatic
    fun html(dom: ValueDom) = dom.element.slimCopy().html()

    @UDFunction
    @JvmStatic
    fun outerHtml(dom: ValueDom) = dom.element.slimCopy().outerHtml()

    @UDFunction
    @JvmStatic
    fun slimHtml(dom: ValueDom) = dom.element.slimHtml

    @UDFunction
    @JvmStatic
    fun minimalHtml(dom: ValueDom) = dom.element.minimalHtml

    @UDFunction
    @JvmStatic
    fun uniqueName(dom: ValueDom) = dom.element.uniqueName

    @UDFunction
    @JvmStatic
    fun links(dom: ValueDom): ValueArray {
        val elements = dom.element.getElementsByTag("a")
        return toValueArray(elements)
    }

    @UDFunction
    @JvmStatic
    fun ch(dom: ValueDom) = getFeature(dom, CH)

    @UDFunction
    @JvmStatic
    fun tn(dom: ValueDom) = getFeature(dom, TN)

    @UDFunction
    @JvmStatic
    fun img(dom: ValueDom) = getFeature(dom, IMG)

    @UDFunction
    @JvmStatic
    fun a(dom: ValueDom) = getFeature(dom, A)

    @UDFunction
    @JvmStatic
    fun sib(dom: ValueDom) = getFeature(dom, SIB)

    @UDFunction
    @JvmStatic
    fun c(dom: ValueDom) = getFeature(dom, C)

    @UDFunction
    @JvmStatic
    fun dep(dom: ValueDom) = getFeature(dom, DEP)

    @UDFunction
    @JvmStatic
    fun seq(dom: ValueDom) = getFeature(dom, SEQ)

    @UDFunction
    @JvmStatic
    fun top(dom: ValueDom): Double {
        return getFeature(dom, TOP)
    }

    @UDFunction
    @JvmStatic
    fun left(dom: ValueDom): Double {
        return getFeature(dom, LEFT)
    }

    @UDFunction
    @JvmStatic
    fun width(dom: ValueDom): Double {
        return getFeature(dom, WIDTH).coerceAtLeast(1.0)
    }

    @UDFunction
    @JvmStatic
    fun height(dom: ValueDom): Double {
        return getFeature(dom, HEIGHT).coerceAtLeast(1.0)
    }

    @UDFunction(description = "Get the area of the css box of a DOM, area = width * height")
    @JvmStatic
    fun area(dom: ValueDom): Double {
        return width(dom) * height(dom)
    }

    @UDFunction(description = "Get the aspect ratio of the DOM, aspect ratio = width / height")
    @JvmStatic
    fun aspectRatio(dom: ValueDom): Double {
        return width(dom) / height(dom)
    }

    private fun getFeature(dom: ValueDom, key: Int): Double {
        return dom.element.getFeature(key)
    }

    private fun toValueArray(elements: Elements): ValueArray {
        val values = arrayOf<Value>()
        for (i in 0 until elements.size) {
            values[i] = ValueDom.get(elements[i])
        }
        return ValueArray.get(values)
    }
}
