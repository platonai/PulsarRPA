/**
 * Created by vincent on 16-5-17.
 *
 * NodeVisitor: used with NodeTraversor together
 */

const DATA_VERSION = "0.2.2";

// the vision schema keeps consistent with DOMRect
const VISION_SCHEMA = ["left", "top", "width", "height"];
const VISION_SCHEMA_STRING = "l-t-w-h";
const CODE_STRUCTURE_SCHEMA_STRING = "d-s";
const VISUALIZE_TAGS = ["BODY", "DIV", "A", "IMG", "TABLE", "UL", "DL", "H1", "H2", "H3"];
const META_INFORMATION_ID = "ScrapingMetaInformation";

const ATTR_COMPUTED_STYLE = 'st';
const ATTR_ELEMENT_NODE_VI = 'vi';
const ATTR_TEXT_NODE_VI = 'tv';

const ATTR_DEBUG = '_debug';
const ATTR_DEBUG_LEVEL = "_debugLevel";
const ATTR_HIDDEN = '_hidden';

"use strict";

/**
 * Create a new WarpsNodeVisitor
 */
let WarpsNodeVisitor = function() {
    this.stopped = false;

    this.config = PULSAR_CONFIGS || {};

    this.debug = this.config.debug;

    this.sequence = 0;

    let metadata = document.querySelector("#" + META_INFORMATION_ID);
    if (metadata) {
        // already exists
        this.stopped = true;
        return;
    }

    this.generateMetadata();
};

/**
 * Generate meta data
 *
 * MetaInformation version :
 * No version : as the same as 0.1.0, the first div was selected as the holder
 * 0.2.0 : add a input element at the end of body element
 * 0.2.1 : add "vi" attribute for each (important) element, deprecate "data-" series
 * 		to deduce file size
 * 0.2.2 : coming soon...
 * */
WarpsNodeVisitor.prototype.generateMetadata = function() {
    document.body.setAttribute("data-url", document.URL);
    let date = new Date();

    let ele = document.createElement("input");
    ele.setAttribute("type", "hidden");
    ele.setAttribute("id", META_INFORMATION_ID);
    ele.setAttribute("domain", document.domain);
    ele.setAttribute("version", DATA_VERSION);
    ele.setAttribute("url", document.URL);
    ele.setAttribute("base-uri", document.baseURI);
    ele.setAttribute("view-port", this.config.viewPortWidth + "x" + this.config.viewPortHeight);
    ele.setAttribute("code-structure", CODE_STRUCTURE_SCHEMA_STRING);
    ele.setAttribute("vision-schema", VISION_SCHEMA_STRING);
    ele.setAttribute("date-time", date.toLocaleDateString() + " " + date.toLocaleTimeString());

    // TODO: insert a piece of javascript code install of add a hidden element

    document.body.firstElementChild.appendChild(ele)
};

/**
 * Check if stopped
 */
WarpsNodeVisitor.prototype.isStopped = function() {
    return this.stopped;
};

/**
 * Enter the element for the first time
 * @param node {Node} the node to enter
 * @param  depth {Number} the depth in the DOM
 */
WarpsNodeVisitor.prototype.head = function(node, depth) {
    ++this.sequence;
    this.calcSelfIndicator(node, depth);
};

/**
 * Calculate the features of the Node itself
 * @param node {Node|Text|HTMLElement} the node to enter
 * @param  depth {Number} the depth in the DOM
 */
WarpsNodeVisitor.prototype.calcSelfIndicator = function(node, depth) {
    if (node.nodeType !== Node.TEXT_NODE && node.nodeType !== Node.ELEMENT_NODE) {
        return
    }

    // check if it's visible
    // https://makandracards.com/makandra/1339-check-whether-an-element-is-visible-or-hidden-with-javascript
    let visible = node.offsetWidth > 0 && node.offsetHeight > 0;
    if (node.nodeType === Node.ELEMENT_NODE) {
        if (!visible) {
            node.setAttribute(ATTR_HIDDEN, '1');
        }
    }

    if (node.nodeType === Node.TEXT_NODE) {
        this.calcCharacterWidth(node, depth);

        // Browser computed styles. Only leaf elements matter
        let propertyNames = this.config.propertyNames || [];
        if (propertyNames.length > 0 && node.textContent.length > 0) {
            let text = __utils__.getTextContent(node);
            if (text.length > 0) {
                let parent = node.parentElement;
                let computed = parent.hasAttribute(ATTR_COMPUTED_STYLE);
                if (!computed) {
                    let styles = __utils__.getComputedStyle(parent, propertyNames);
                    if (styles) {
                        parent.setAttribute(ATTR_COMPUTED_STYLE, styles);
                    }
                }
            }
        }
    }

    if (node.nodeType === Node.ELEMENT_NODE) {
        // calculate the rectangle of this element
        let rect = __utils__.getClientRect(node);
        if (rect) {
            node.setAttribute(ATTR_ELEMENT_NODE_VI, __utils__.formatDOMRect(rect));
        }

        // calculate the rectangle of each child text node
        for (let i = 0; i < node.childNodes.length; ++i) {
            let nd = node.childNodes[i];
            if (nd.nodeType === Node.TEXT_NODE) {
                rect = __utils__.getClientRect(nd);
                if (rect) {
                    // 'tv' is short for 'text node vision information'
                    node.setAttribute(ATTR_TEXT_NODE_VI + i, __utils__.formatDOMRect(rect));
                    // 'l' is short for 'text length', it's used to diagnosis
                    if (this.debug > 0 && nd.textContent) {
                        __utils__.addTuple(node, ATTR_DEBUG, "l" + i, nd.textContent.length);
                    }
                }
            }
        }

        if (this.debug > 0) {
            // code structure calculated using js, this can be a complement to native code calculated info
            __utils__.addTuple(node, ATTR_DEBUG, "d", depth);
            __utils__.addTuple(node, ATTR_DEBUG, "s", this.sequence);
        }
    }
};

/**
 * Calculate the width of the text node, this is a complement of the rectangle information, can be used for debugging
 *
 * @param node {Node} the node to enter
 * @param  depth {Number} the depth in the DOM
 * @return {Number}
 */
WarpsNodeVisitor.prototype.calcCharacterWidth = function(node, depth) {
    let parent = node.parentElement;
    let cw = parent.getAttribute('_cw');
    let width = 0;
    if (!cw) {
        let text = __utils__.getTextContent(node);
        if (text.length > 0) {
            width = __utils__.getElementTextWidth(text, parent);
            cw = Math.round(width / text.length * 10) / 10;
            parent.setAttribute('_cw', cw.toString())
        }
    }
    return width
};

/**
 * Leaving the the element
 *
 * @param node {Node|Element} the node visited
 * @param  depth {Number} the depth in the DOM
 */
WarpsNodeVisitor.prototype.tail = function(node, depth) {
    if (node.nodeType !== Node.TEXT_NODE && node.nodeType !== Node.ELEMENT_NODE) {
        return
    }

    if (node.nodeType === Node.ELEMENT_NODE) {
        // Element descends
        if (this.debug > 0) {
            let descend = __utils__.getIntAttribute(node, "_d", 0);
            __utils__.increaseIntAttribute(node.parentElement, '_d', descend + 1);
        }
    }
};
