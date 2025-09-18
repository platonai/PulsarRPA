package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.dom.nodes.convertBox
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.common.types.ValueDom
import org.h2.value.ValueArray

/**
 * Box-based DOM element selection functions for X-SQL queries in Pulsar QL.
 *
 * This object provides specialized functions for selecting DOM elements within
 * defined "box" regions of web pages. Box functions enable precise element targeting
 * using box-based selectors, which are particularly useful for structured web scraping
 * and content extraction from layout-defined regions.
 *
 * ## Function Categories
 *
 * ### Box-based Selection
 * - [all] - Select all elements within a box region (with optional pagination)
 * - [first] - Select first element within a box region
 * - [nth] - Select nth element within a box region
 *
 * ### Content Extraction
 * - [firstText] - Extract text from first element in box
 * - [nthText] - Extract text from nth element in box
 * - [firstImg] - Extract image source from first element in box
 * - [nthImg] - Extract image source from nth element in box
 * - [firstHref] - Extract link from first element in box
 * - [nthHref] - Extract link from nth element in box
 *
 * ### Pattern-based Extraction
 * - [firstRe1] - Extract content using regex from first element in box
 * - [firstRe2] - Extract key-value pairs using regex from first element in box
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Select all elements in a box
 * SELECT IN_BOX.all(DOM.load('https://example.com'), 'content-box');
 *
 * -- Extract text from first element in box
 * SELECT IN_BOX.firstText(DOM.load('https://example.com'), 'article-box');
 *
 * -- Extract nth element text
 * SELECT IN_BOX.nthText(DOM.load('https://example.com'), 'product-box', 2);
 *
 * -- Extract image from box
 * SELECT IN_BOX.firstImg(DOM.load('https://example.com'), 'gallery-box');
 *
 * -- Extract link from box
 * SELECT IN_BOX.firstHref(DOM.load('https://example.com'), 'navigation-box');
 *
 * -- Use regex extraction within box
 * SELECT IN_BOX.firstRe1(DOM.load('https://example.com'), 'price-box', '\\$([0-9.]+)');
 * ```
 *
 * ## X-SQL Integration
 *
 * All box functions are automatically registered as H2 database functions under the
 * "IN_BOX" namespace. They can be used directly in X-SQL queries and combined with
 * DOM loading functions for targeted content extraction.
 *
 * ## Box Selector Format
 *
 * Box selectors typically refer to predefined CSS classes or regions that group
 * related content elements. Common patterns include:
 * - Content containers: "article-box", "content-box", "main-box"
 * - Product listings: "product-box", "item-box", "listing-box"
 * - Navigation elements: "nav-box", "menu-box", "sidebar-box"
 * - Media containers: "gallery-box", "image-box", "media-box"
 *
 * ## Performance Notes
 *
 * - Box functions delegate to optimized DOM selection functions
 * - Pagination support for large result sets
 * - Efficient box-to-selector conversion
 * - Minimal overhead for box-based targeting
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see convertBox
 * @see DomSelectFunctions
 * @see DomInlineSelectFunctions
 * @see UDFGroup
 * @see UDFunction
 */
@Suppress("unused")
@UDFGroup(namespace = "IN_BOX")
object BoxFunctions {

    /**
     * Selects all elements within a specified box region of the DOM.
     *
     * This function converts the box identifier to a CSS selector and returns all matching
     * elements within the box region. Useful for extracting collections of related elements
     * that are grouped within specific layout containers.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Select all elements in content box
     * SELECT IN_BOX.all(DOM.load('https://example.com'), 'article-box');
     *
     * -- Select all products in product listing
     * SELECT IN_BOX.all(DOM.load('https://shop.com'), 'product-box');
     *
     * -- Process multiple boxes
     * SELECT IN_BOX.all(dom, 'item-box') as items
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     * ```
     *
     * ## Use Cases
     * - Extracting all items from a list or grid
     * - Processing content within specific layout regions
     * - Batch extraction from container elements
     * - Collection processing within defined boundaries
     *
     * @param dom The DOM document to search within
     * @param box The box identifier to convert to CSS selector
     * @return ValueArray containing all matching elements within the box
     * @see convertBox for box-to-selector conversion
     * @see DomInlineSelectFunctions.inlineSelect
     */
    @JvmStatic
    @UDFunction
    fun all(dom: ValueDom, box: String): ValueArray {
        return DomInlineSelectFunctions.inlineSelect(dom, convertBox(box))
    }

    /**
     * Selects all elements within a box region with pagination support.
     *
     * This function converts the box identifier to a CSS selector and returns a subset
     * of matching elements based on offset and limit parameters. Useful for processing
     * large collections of elements in manageable chunks.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Get first 10 products
     * SELECT IN_BOX.all(DOM.load('https://shop.com'), 'product-box', 0, 10);
     *
     * -- Get next 10 products (page 2)
     * SELECT IN_BOX.all(DOM.load('https://shop.com'), 'product-box', 10, 10);
     *
     * -- Process large lists in batches
     * SELECT IN_BOX.all(dom, 'item-box', 0, 50) as first_batch
     * FROM (SELECT DOM.load('https://example.com') as dom) t;
     * ```
     *
     * ## Use Cases
     * - Processing large datasets in batches
     * - Implementing pagination for extraction queries
     * - Memory-efficient processing of large collections
     * - Incremental data extraction
     *
     * ## Pagination Parameters
     * - **offset**: Starting index (0-based)
     * - **limit**: Maximum number of elements to return
     *
     * @param dom The DOM document to search within
     * @param box The box identifier to convert to CSS selector
     * @param offset Starting index for pagination (0-based)
     * @param limit Maximum number of elements to return
     * @return ValueArray containing the paginated selection of elements
     * @see convertBox for box-to-selector conversion
     * @see DomInlineSelectFunctions.inlineSelect
     */
    @JvmStatic
    @UDFunction
    fun all(dom: ValueDom, box: String, offset: Int, limit: Int): ValueArray {
        return DomInlineSelectFunctions.inlineSelect(dom, convertBox(box), offset, limit)
    }

    /**
     * Selects the first element within a specified box region.
     *
     * This function converts the box identifier to a CSS selector and returns the first
     * matching element within the box region. Ideal for extracting primary or featured
     * content from specific layout containers.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Get first article in content box
     * SELECT IN_BOX.first(DOM.load('https://news.com'), 'article-box');
     *
     * -- Extract main product information
     * SELECT IN_BOX.first(DOM.load('https://shop.com'), 'featured-product');
     *
     * -- Get first navigation item
     * SELECT IN_BOX.first(DOM.load('https://example.com'), 'nav-box');
     * ```
     *
     * ## Use Cases
     * - Extracting primary content from containers
     * - Getting featured or highlighted items
     * - Accessing main elements within sections
     * - Primary content extraction from specific regions
     *
     * @param dom The DOM document to search within
     * @param box The box identifier to convert to CSS selector
     * @return ValueDom containing the first matching element, or ValueDom.NIL if not found
     * @see convertBox for box-to-selector conversion
     * @see DomSelectFunctions.selectFirst
     */
    @JvmStatic
    @UDFunction
    fun first(dom: ValueDom, box: String): ValueDom {
        return DomSelectFunctions.selectFirst(dom, convertBox(box))
    }

    /**
     * Selects the nth element within a specified box region.
     *
     * This function converts the box identifier to a CSS selector and returns the nth
     * matching element within the box region (0-based indexing). Useful for accessing
     * specific items in ordered collections or lists.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Get second article (index 1)
     * SELECT IN_BOX.nth(DOM.load('https://news.com'), 'article-box', 1);
     *
     * -- Get third product in listing (index 2)
     * SELECT IN_BOX.nth(DOM.load('https://shop.com'), 'product-box', 2);
     *
     * -- Access specific navigation items
     * SELECT IN_BOX.nth(DOM.load('https://example.com'), 'menu-box', 0); -- first item
     * SELECT IN_BOX.nth(DOM.load('https://example.com'), 'menu-box', 1); -- second item
     * ```
     *
     * ## Use Cases
     * - Accessing specific items in ordered lists
     * - Extracting elements by position
     * - Processing items at specific indices
     * - Sequential content extraction
     *
     * ## Indexing
     * - Index is 0-based (0 = first element, 1 = second element, etc.)
     * - Returns ValueDom.NIL if index is out of bounds
     * - Safe for use with dynamic indices
     *
     * @param dom The DOM document to search within
     * @param box The box identifier to convert to CSS selector
     * @param n The index of the element to select (0-based)
     * @return ValueDom containing the nth matching element, or ValueDom.NIL if not found
     * @see convertBox for box-to-selector conversion
     * @see DomSelectFunctions.selectNth
     */
    @JvmStatic
    @UDFunction
    fun nth(dom: ValueDom, box: String, n: Int): ValueDom {
        return DomSelectFunctions.selectNth(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstText(dom: ValueDom, box: String): String {
        return DomSelectFunctions.firstText(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nthText(dom: ValueDom, box: String, n: Int): String {
        return DomSelectFunctions.nthText(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstImg(dom: ValueDom, box: String): String {
        return DomSelectFunctions.firstImg(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nthImg(dom: ValueDom, box: String, n: Int): String {
        return DomSelectFunctions.nthImg(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstHref(dom: ValueDom, box: String): String {
        return DomSelectFunctions.firstHref(dom, convertBox(box))
    }

    @JvmStatic
    @UDFunction
    fun nthHref(dom: ValueDom, box: String, n: Int): String {
        return DomSelectFunctions.nthHref(dom, convertBox(box), n)
    }

    @JvmStatic
    @UDFunction
    fun firstRe1(dom: ValueDom, box: String, regex: String): String {
        return DomSelectFunctions.firstRe1(dom, convertBox(box), regex)
    }

    @JvmStatic
    @UDFunction
    fun firstRe1(dom: ValueDom, box: String, regex: String, group: Int): String {
        return DomSelectFunctions.firstRe1(dom, convertBox(box), regex, group)
    }

    @JvmStatic
    @UDFunction
    fun firstRe2(dom: ValueDom, box: String, regex: String): ValueArray {
        return DomSelectFunctions.firstRe2(dom, convertBox(box), regex)
    }

    @JvmStatic
    @UDFunction
    fun firstRe2(dom: ValueDom, box: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        return DomSelectFunctions.firstRe2(dom, convertBox(box), regex, keyGroup, valueGroup)
    }
}
