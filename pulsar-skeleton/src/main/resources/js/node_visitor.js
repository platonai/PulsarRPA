/**
 * Created by vincent on 16-5-17.
 *
 * NodeVisitor: used with NodeTraversor together
 */

const DATA_VERSION = "0.2.3";

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
const ATTR_OVERFLOW_HIDDEN = '_o_hidden';

"use strict";

let NodeExt = function (node, config) {
    /**
     * The config
     * */
    this.config = config;
    /**
     * Desired property names of computed styles
     * Array
     * */
    this.propertyNames = [];
    /**
     * Computed styles
     * Map
     * */
    this.styles = {};
    /**
     * Max width for all descendants, if an element have property overflow:hidden, then
     * all it's descendants should hide the parts overflowed.
     * Number
     * */
    this.maxWidth = config.viewPortWidth;
    /**
     * The rectangle of this node
     * DOMRect
     * */
    this.rect = null;
    /**
     * Integer
     * */
    this.depth = 0;
    /**
     * Sequence
     * */
    this.sequence = 0;
    /**
     * Node
     * */
    this.node = node;
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isText = function() {
    return this.node != null && this.node.nodeType === Node.TEXT_NODE;
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isElement = function() {
    return this.node != null && this.node.nodeType === Node.ELEMENT_NODE;
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isTextOrElement = function() {
    return this.isText() || this.isElement();
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isImage = function() {
    return this.node.nodeName === "img";
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isAnchor = function() {
    return this.node.nodeName === "a";
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isTile = function() {
    return this.isImage() || this.isText();
};

/**
 * Check if it's visible
 * https://stackoverflow.com/questions/19669786/check-if-element-is-visible-in-dom
 * @return {boolean}
 * */
NodeExt.prototype.isVisible = function() {
    let hidden = this.node.offsetParent === null;

    if (hidden) {
        // this.setAttributeIfNotBlank("_offset", this.node.offsetWidth + "x" + this.node.offsetHeight);
        return false
    }

    return !this.isOverflowHidden()
};

NodeExt.prototype.isHidden = function() {
    return !this.isVisible();
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isOverflown = function() {
    return this.node.scrollHeight > this.node.clientHeight || this.node.scrollWidth > this.node.clientWidth;
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.isOverflowHidden = function() {
    let p = this.parent();
    let maxWidth = this.config.viewPortWidth;
    if (p != null && p.maxWidth < maxWidth && (this.left() >= p.right() || this.right() <= p.left())) {
        return true
    }
    return false
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.hasOverflowHidden = function() {
    return this.styles["overflow"] === "hidden";
};

/**
 * @return {boolean}
 * */
NodeExt.prototype.hasParent = function() {
    return this.node.parentElement != null && this.parent() != null;
};

/**
 * @return {NodeExt}
 * */
NodeExt.prototype.parent = function() {
    return this.node.parentElement.nodeExt;
};

NodeExt.prototype.getRect = function() {
    this.rect = __utils__.getClientRect(this.node);

    if (this.isImage()) {
        if (this.rect.width === 0) {
            let w = this.attr("width");
            if (w.match(/\d+/)) {
                this.rect.width = Number.parseInt(w)
            }
        }

        if (this.rect.height === 0) {
            let h = this.attr("height");
            if (w.match(/\d+/)) {
                this.rect.height = Number.parseInt(h)
            }
        }
    }

    return this.rect
};

/**
 * Get left
 * */
NodeExt.prototype.left = function() {
    return this.rect.left
};

/**
 * Get right
 * */
NodeExt.prototype.right = function() {
    return this.left() + this.width()
};

/**
 * Get top
 * */
NodeExt.prototype.top = function() {
    return this.rect.top
};

/**
 * Get bottom
 * */
NodeExt.prototype.bottom = function() {
    return this.top() + this.height()
};

/**
 * Get width
 * */
NodeExt.prototype.width = function() {
    return this.rect.width
};

/**
 * Get height
 * */
NodeExt.prototype.height = function() {
    return this.rect.height
};

/**
 * @param width {Number|null}
 * */
NodeExt.prototype.updateMaxWidth = function(width) {
    if (this.hasParent()) {
        this.maxWidth = Math.min(this.parent().maxWidth, width);
    }
};

/**
 * Get the attribute value
 * @param attrName {String}
 * @return {String|null}
 * */
NodeExt.prototype.attr = function(attrName) {
    if (this.isElement()) {
        return this.node.getAttribute(attrName)
    }
    return null
};

/**
 * Set attribute if it's not blank
 * @param attrName {String}
 * @param attrValue {String}
 * */
NodeExt.prototype.setAttributeIfNotBlank = function(attrName, attrValue) {
    if (this.isElement() && attrValue && attrValue.trim().length > 0) {
        this.node.setAttribute(attrName, attrValue.trim())
    }
};

/**
 * Get the formatted rect
 * */
NodeExt.prototype.formatDOMRect = function() {
    return __utils__.formatDOMRect(this.rect)
};

/**
 * Get the formatted rect
 * */
NodeExt.prototype.formatStyles = function() {
    return this.propertyNames.map(propertyName => this.styles[propertyName]).join(", ")
};

/**
 * Adjust the node's DOMRect
 * If the child element larger than the parent and the parent have overflow:hidden style,
 * the child element's DOMRect should be adjusted
 * */
NodeExt.prototype.adjustDOMRect = function() {
    if (this.rect) {
        this.rect.width = Math.min(this.rect.width, this.maxWidth);
    }
};

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
 * 0.2.2 :
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
    ele.setAttribute("timestamp", date.getTime().toString());

    if (this.config.version !== DATA_VERSION) {
        ele.setAttribute("version-mismatch", this.config.version + "-" + DATA_VERSION);
    }

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

    node.nodeExt = new NodeExt(node, this.config);

    this.calcSelfIndicator(node, depth);
};

/**
 * Calculate the features of the Node itself
 * @param node {Node|Text|HTMLElement} the node to enter
 * @param  depth {Number} the depth in the DOM
 */
WarpsNodeVisitor.prototype.calcSelfIndicator = function(node, depth) {
    let nodeExt = node.nodeExt;

    if (nodeExt.isText()) {
        this.calcCharacterWidth(node, depth);
    }

    nodeExt.depth = depth;
    nodeExt.sequence = this.sequence;

    if (nodeExt.isElement()) {
        // Browser computed styles. Only leaf elements matter
        nodeExt.propertyNames = this.config.propertyNames || [];
        let requiredPropertyNames = nodeExt.propertyNames.concat("overflow");
        nodeExt.styles = __utils__.getComputedStyle(node, requiredPropertyNames);
    }

    // Calculate the rectangle of this node
    nodeExt.getRect();

    if (nodeExt.isElement()) {
        // "hidden" seems not defined properly,
        // some parent element is "hidden" and some of there children are not expected to be hidden
        // for example, ul tag often have a zero dimension
        if (nodeExt.isHidden()) {
            node.setAttribute(ATTR_HIDDEN, '1');
        }

        if (nodeExt.isOverflowHidden() || (nodeExt.hasParent() && nodeExt.parent().node.hasAttribute(ATTR_OVERFLOW_HIDDEN))) {
            node.setAttribute(ATTR_OVERFLOW_HIDDEN, '1');
        }
    }

    // all descendant nodes should be smaller than this one
    if (nodeExt.hasOverflowHidden()) {
        // TODO: also update max height
        nodeExt.updateMaxWidth(nodeExt.rect.width);
    } else {
        nodeExt.updateMaxWidth(this.config.viewPortWidth);
    }

    nodeExt.adjustDOMRect();
};

/**
 * Leaving the the element
 *
 * @param node {Node|Element} the node visited
 * @param  depth {Number} the depth in the DOM
 */
WarpsNodeVisitor.prototype.tail = function(node, depth) {
    let nodeExt = node.nodeExt;
    if (!nodeExt) {
        return
    }

    if (nodeExt.isElement()) {
        nodeExt.setAttributeIfNotBlank(ATTR_COMPUTED_STYLE, nodeExt.formatStyles());
        nodeExt.setAttributeIfNotBlank(ATTR_ELEMENT_NODE_VI, nodeExt.formatDOMRect());

        // calculate the rectangle of each child text node
        for (let i = 0; i < node.childNodes.length; ++i) {
            let childPulsar = node.childNodes[i].nodeExt;
            if (childPulsar && childPulsar.isText()) {
                // 'tv' is short for 'text node vision information'
                nodeExt.setAttributeIfNotBlank(ATTR_TEXT_NODE_VI + i, childPulsar.formatDOMRect());
            }
        }
    }

    if (this.debug > 0) {
        this.addDebugInfo()
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

WarpsNodeVisitor.prototype.addDebugInfo = function(node) {
    if (!node.nodeExt) {
        return
    }

    let nodeExt = node.nodeExt;

    if (nodeExt.isText()) {
        // 'tl' is short for 'text length', it's used to diagnosis
        if (node.textContent) {
            __utils__.addTuple(node, ATTR_DEBUG, "tl" + i, node.textContent.length);
        }
    } else {
        let descend = __utils__.getIntAttribute(node, "_d", 0);
        __utils__.increaseIntAttribute(node.parentElement, '_d', 1);
    }
};
