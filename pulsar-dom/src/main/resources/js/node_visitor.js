/**
 * Created by vincent on 16-5-17.
 *
 * NodeVisitor: used with NodeTraversor together
 */

const DATA_VERSION = "0.2.1";
const DEBUG_LEVEL = "debug";

// the vision schema keeps consistent with DOMRect
const VISION_SCHEMA = ["left", "top", "width", "height"];
const VISION_SCHEMA_STRING = "l-t-w-h";
const CODE_STRUCTURE_SCHEMA_STRING = "d-s";
const VISUALIZE_TAGS = ["BODY", "DIV", "A", "IMG", "TABLE", "UL", "DL", "H1", "H2", "H3"];
const META_INFORMATION_ID = "ScrapingMetaInformation";
const VIEW_PORT_WIDTH = "{VIEW_PORT_WIDTH}";
const VIEW_PORT_HEIGHT = "{VIEW_PORT_HEIGHT}";

"use strict";

/**
 * Create a new WarpsNodeVisitor
 */
let WarpsNodeVisitor = function() {
    this.stopped = false;

    this.debug = __utils__.getIntAttribute(document.body, DEBUG_LEVEL, 1);

    this.sequence = 0;

    let metadata = document.querySelector("#" + META_INFORMATION_ID);
    if (metadata) {
        // already exists
        this.stopped = true;
        return;
    }

    this.bodyRect = document.body.getBoundingClientRect();

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
    ele.setAttribute("view-port", VIEW_PORT_WIDTH + "x" + VIEW_PORT_HEIGHT);
    ele.setAttribute("code-structure", CODE_STRUCTURE_SCHEMA_STRING);
    ele.setAttribute("vision-schema", VISION_SCHEMA_STRING);
    ele.setAttribute("date-time", date.toLocaleDateString() + " " + date.toLocaleTimeString());

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

    if (node.nodeType === Node.TEXT_NODE) {
        this.setCharacterWidth(node, depth)
    }

    if (node.nodeType === Node.ELEMENT_NODE) {
        // calculate the rectangle of this element
        let rect = __utils__.getClientRect(node);
        if (rect) {
            // 'vi' is short for 'vision information'
            node.setAttribute('vi', __utils__.formatDOMRect(rect));
        }

        // calculate the rectangle of each child text node
        for (let i = 0; i < node.childNodes.length; ++i) {
            let nd = node.childNodes[i];
            if (nd.nodeType === Node.TEXT_NODE) {
                rect = __utils__.getClientRect(nd);
                if (rect) {
                    // 'tv' is short for 'text node vision information'
                    node.setAttribute("tv" + i, __utils__.formatDOMRect(rect))
                }
            }
        }

        // check if it's visible
        // https://makandracards.com/makandra/1339-check-whether-an-element-is-visible-or-hidden-with-javascript
        let visible = node.offsetWidth > 0 && node.offsetHeight > 0;
        if (!visible) {
            node.setAttribute('_hidden', '1');
        }

        if (this.debug > 0) {
            // code structure calculated using js, this can be a complement to native code calculated info
            let debug = "d:" + depth + " s:" + this.sequence;
            node.setAttribute('_debug', debug);
        }
    }
};

/**
 * Calculate the width of the text node, this is a complement of the rectangle information, can be used for debugging
 *
 * @param node {Node} the node to enter
 * @param  depth {Number} the depth in the DOM
 */
WarpsNodeVisitor.prototype.setCharacterWidth = function(node, depth) {
    let parent = node.parentElement;
    let cw = parent.getAttribute('_cw');
    if (!cw) {
        let text = __utils__.getTextContent(node);
        if (text.length > 0) {
            let width = __utils__.getElementTextWidth(text, parent);
            cw = Math.round(width / text.length * 10) / 10;
            parent.setAttribute('_cw', cw.toString())
        }
    }
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
