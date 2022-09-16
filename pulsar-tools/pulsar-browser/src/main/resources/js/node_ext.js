"use strict";

/**
 * Set attribute if it's not blank
 * @param attrName {String}
 * @param attrValue {String}
 * */
Node.prototype.__pulsar_setAttributeIfNotBlank = function(attrName, attrValue) {
    if (this instanceof HTMLElement && attrValue && attrValue.trim().length > 0) {
        this.setAttribute(attrName, attrValue.trim())
    }
};

/**
 * @param predicate The predicate
 * */
Node.prototype.__pulsar_count = function(predicate) {
    let c = 0;
    let visitor = function () {};
    visitor.head = function (node, depth) {
        if (predicate(node)) {
            ++c;
        }
    };

    new __pulsar_NodeTraversor(visitor).traverse(this);
    return c;
};

/**
 * @param action The action applied to each node
 * */
Node.prototype.__pulsar_forEach = function(action) {
    let visitor = {};
    visitor.head = function (node, depth) {
        action(node)
    };
    new __pulsar_NodeTraversor(visitor).traverse(this);
};

/**
 * @param action The action applied to each node
 * */
Node.prototype.__pulsar_forEachElement = function(action) {
    let visitor = {};
    visitor.head = function (node, depth) {
        if (node.nodeType === Node.ELEMENT_NODE) {
            action(node)
        }
    };
    new __pulsar_NodeTraversor(visitor).traverse(this);
};

/**
 * @param pattern The pattern to match
 * @return {Element|null}
 * */
Node.prototype.__pulsar_findMatches = function(pattern) {
    let visitor = {};

    let result = null
    visitor.head = function (node, depth) {
        if (node instanceof HTMLElement) {
            let text = node.textContent
            if (text.match(pattern)) {
                result = node
                visitor.stopped = true
            }
        }
    };

    new __pulsar_NodeTraversor(visitor).traverse(this);

    return result
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isText = function() {
    return this.nodeType === Node.TEXT_NODE;
};

/**
 * @return {string}
 * */
Node.prototype.__pulsar_cleanText = function() {
    let text = this.textContent.replace(/\s+/g, ' ');
    // remove &nbsp;
    text = text.replace(/\u00A0/g, ' ');
    return text.trim();
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isShortText = function() {
    if (!this.__pulsar_isText()) return false;

    let text = this.__pulsar_cleanText();
    return text.length >= 1 && text.length <= 9;
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isNumberLike = function() {
    if (!this.__pulsar_isShortText()) return false;

    let text = this.__pulsar_cleanText().replace(/\s+/g, '');
    // matches ￥3,412.25, ￥3,412.25, 3,412.25, 3412.25, etc
    return /.{0,4}((\d+),?)*(\d+)\.?\d+.{0,3}/.test(text);
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isElement = function() {
    return this.nodeType === Node.ELEMENT_NODE;
};

/**
 * @return {Element}
 * */
Node.prototype.__pulsar_bestElement = function() {
    if (this.__pulsar_isElement()) return this;
    else return this.parentElement;
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isTextOrElement = function() {
    return this.__pulsar_isText() || this.__pulsar_isElement();
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isDiv = function() {
    // HTML-uppercased qualified name
    return this.nodeName === "DIV";
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isImage = function() {
    // HTML-uppercased qualified name
    return this.nodeName === "IMG";
};

/**
 * @return {Number}
 * */
Node.prototype.__pulsar_nScreen = function() {
    let rect = this.__pulsar_getRect();
    const config = __pulsar_CONFIGS;
    const viewPortHeight = config.viewPortHeight;
    let ns = rect.y / viewPortHeight;
    return Math.ceil(ns);
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isSmallImage = function() {
    if (!this.__pulsar_isImage()) {
        return false
    }

    let rect = this.__pulsar_getRect();
    return rect.width <= 50 || rect.height <= 50;
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isMediumImage = function() {
    if (!this.__pulsar_isImage()) {
        return false
    }

    let rect = this.__pulsar_getRect();
    let area = rect.width * rect.height;
    return rect.width > 50 && rect.height > 50 && area < 300 * 300;
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isLargeImage = function() {
    if (!this.__pulsar_isImage()) {
        return false
    }

    let rect = this.__pulsar_getRect();
    let area = rect.width * rect.height;
    return area > 300 * 300;
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isAnchor = function() {
    // HTML-uppercased qualified name
    // if (this instanceof HTMLAnchorElement)
    return this.nodeName === "A";
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_maybeClickable = function() {
    let element = this.__pulsar_bestElement();
    if (element == null) {
        return false
    }
    if (!element.__pulsar_isAnchor()) {
        return false
    }

    let clickable = true
    let rect = this.__pulsar_getRect()
    if (rect.x < 0 || rect.y < 0) {
        clickable = false
    }
    if (rect.width < 5 || rect.height < 5.0) {
        clickable = false
    }

    return clickable
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isTile = function() {
    return this.__pulsar_isImage() || this.__pulsar_isText();
};

/**
 * @return {boolean}
 * */
Node.prototype.__pulsar_isIFrame = function() {
    return this.nodeName === "IFRAME";
};

/**
 * Get the estimated rect of this node, if the node is not an element, return it's parent element's rect
 * @return {DOMRect|null}
 * */
Node.prototype.__pulsar_getRect = function() {
    let element = this.__pulsar_bestElement();
    if (element == null) {
        return null
    }

    let rect = __pulsar_utils__.getClientRect(element);

    if (element.__pulsar_isImage()) {
        if (!rect) {
            rect = new DOMRect(0, 0, 0, 0)
        }

        if (rect.width === 0) {
            let w = element.getAttribute("width");
            if (w && /\d+/.test(w)) {
                rect.width = Number.parseInt(w)
            }
        }

        if (rect.height === 0) {
            let h = element.getAttribute("height");
            if (h && /\d+/.test(h)) {
                rect.height = Number.parseInt(h)
            }
        }
    }

    return rect
};

let __pulsar_NodeExt = function (node, config) {
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
 * Check if it's visible
 * https://stackoverflow.com/questions/19669786/check-if-element-is-visible-in-dom
 * @return {boolean}
 * */
__pulsar_NodeExt.prototype.isVisible = function() {
    let hidden = this.node.offsetParent === null;

    if (hidden) {
        return false
    }

    return !this.isOverflowHidden()
};

__pulsar_NodeExt.prototype.isHidden = function() {
    return !this.isVisible();
};

/**
 * @return {boolean}
 * */
__pulsar_NodeExt.prototype.isOverflown = function() {
    return this.node.scrollHeight > this.node.clientHeight || this.node.scrollWidth > this.node.clientWidth;
};

/**
 * @return {boolean}
 * */
__pulsar_NodeExt.prototype.isOverflowHidden = function() {
    let p = this.parent();
    let maxWidth = this.config.viewPortWidth;
    return p != null && p.maxWidth < maxWidth && (this.left() >= p.right() || this.right() <= p.left());

};

/**
 * @return {boolean}
 * */
__pulsar_NodeExt.prototype.hasOverflowHidden = function() {
    return this.styles["overflow"] === "hidden";
};

/**
 * @return {boolean}
 * */
__pulsar_NodeExt.prototype.hasParent = function() {
    return this.node.parentElement != null && this.parent() != null;
};

/**
 * @return {__pulsar_NodeExt}
 * */
__pulsar_NodeExt.prototype.parent = function() {
    return this.node.parentElement.nodeExt;
};

/**
 * Get left
 * */
__pulsar_NodeExt.prototype.left = function() {
    return this.rect.left
};

/**
 * Get right
 * */
__pulsar_NodeExt.prototype.right = function() {
    return this.left() + this.width()
};

/**
 * Get top
 * */
__pulsar_NodeExt.prototype.top = function() {
    return this.rect.top
};

/**
 * Get bottom
 * */
__pulsar_NodeExt.prototype.bottom = function() {
    return this.top() + this.height()
};

/**
 * Get width
 * */
__pulsar_NodeExt.prototype.width = function() {
    return this.rect.width
};

/**
 * Get height
 * */
__pulsar_NodeExt.prototype.height = function() {
    return this.rect.height
};

/**
 * @param width {Number|null}
 * */
__pulsar_NodeExt.prototype.updateMaxWidth = function(width) {
    if (this.hasParent()) {
        this.maxWidth = Math.min(this.parent().maxWidth, width);
    }
};

/**
 * Get the attribute value
 * @param attrName {String}
 * @return {String|null}
 * */
__pulsar_NodeExt.prototype.attr = function(attrName) {
    if (this.node.isElement()) {
        return this.node.getAttribute(attrName)
    }
    return null
};

/**
 * Get the formatted rect
 * */
__pulsar_NodeExt.prototype.formatDOMRect = function() {
    return __pulsar_utils__.formatDOMRect(this.rect)
};

/**
 * Get the formatted rect
 * @return string
 * */
__pulsar_NodeExt.prototype.formatStyles = function() {
    return this.propertyNames.map(propertyName => this.styles[propertyName]).join(", ")
};

/**
 * Adjust the node's DOMRect
 * If the child element larger than the parent and the parent have overflow:hidden style,
 * the child element's DOMRect should be adjusted
 * */
__pulsar_NodeExt.prototype.adjustDOMRect = function() {
    if (this.rect) {
        this.rect.width = Math.min(this.rect.width, this.maxWidth);
    }
};
