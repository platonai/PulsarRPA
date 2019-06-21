"use strict";

/**
 * Set attribute if it's not blank
 * @param attrName {String}
 * @param attrValue {String}
 * */
Node.prototype.setAttributeIfNotBlank = function(attrName, attrValue) {
    if (this.isElement() && attrValue && attrValue.trim().length > 0) {
        this.setAttribute(attrName, attrValue.trim())
    }
};

/**
 * @param predicate The predicate
 * */
Node.prototype.count = function(predicate) {
    let c = 0;
    let visitor = function () {};
    visitor.head = function (node, depth) {
        if (predicate(node)) {
            ++c;
        }
    };

    new PlatonNodeTraversor(visitor).traverse(this);
    return c;
};

/**
 * @param action The action applied to each node
 * */
Node.prototype.forEach = function(action) {
    let visitor = {};
    visitor.head = function (node, depth) {
        action(node)
    };
    new PlatonNodeTraversor(visitor).traverse(this);
};

/**
 * @param action The action applied to each node
 * */
Node.prototype.forEachElement = function(action) {
    let visitor = {};
    visitor.head = function (node, depth) {
        if (node.nodeType === Node.ELEMENT_NODE) {
            action(node)
        }
    };
    new PlatonNodeTraversor(visitor).traverse(this);
};

/**
 * @return {boolean}
 * */
Node.prototype.isText = function() {
    return this.nodeType === Node.TEXT_NODE;
};

/**
 * @return {boolean}
 * */
Node.prototype.isElement = function() {
    return this.nodeType === Node.ELEMENT_NODE;
};

/**
 * @return {boolean}
 * */
Node.prototype.isTextOrElement = function() {
    return this.isText() || this.isElement();
};

/**
 * @return {boolean}
 * */
Node.prototype.isImage = function() {
    // HTML-uppercased qualified name
    return this.nodeName === "IMG";
};

/**
 * @return {boolean}
 * */
Node.prototype.isAnchor = function() {
    // HTML-uppercased qualified name
    return this.nodeName === "A";
};

/**
 * @return {boolean}
 * */
Node.prototype.isTile = function() {
    return this.isImage() || this.isText();
};

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
 * Check if it's visible
 * https://stackoverflow.com/questions/19669786/check-if-element-is-visible-in-dom
 * @return {boolean}
 * */
NodeExt.prototype.isVisible = function() {
    let hidden = this.node.offsetParent === null;

    if (hidden) {
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
    return p != null && p.maxWidth < maxWidth && (this.left() >= p.right() || this.right() <= p.left());

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

    if (this.node.isImage()) {
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
    if (this.node.isElement()) {
        return this.node.getAttribute(attrName)
    }
    return null
};

/**
 * Get the formatted rect
 * */
NodeExt.prototype.formatDOMRect = function() {
    return __utils__.formatDOMRect(this.rect)
};

/**
 * Get the formatted rect
 * @return string
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
