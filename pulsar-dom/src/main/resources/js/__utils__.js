let __utils__ = function () {};

/**
 * Check if the variables are defined
 * @param variables object with format : {varName1 : var1, varName2 : var2, ..., varNameN : varN}
 * @return {Object} A json object to report the existence of each variable
 * */
__utils__.checkVariables = function(variables) {
    "use strict";

    let report = {};
    for (let v in variables) {
        if (variables[v] !== undefined) {
            report[v] = typeof(variables[v]);
        }
        else {
            report[v] = false;
        }
    }
    return report;
};

__utils__.scrollToBottom = function() {
    let x = 0;
    let y = Math.max(
        document.documentElement.scrollHeight,
        document.body.scrollHeight,
        document.documentElement.clientHeight);

    window.scrollTo(x, Math.min(y, 15000));
};

__utils__.scrollToTop = function() {
    window.scrollTo(0, 0);
};

/**
 * Clones an object.
 *
 * @param  {Object} o
 * @return {Object}
 */
__utils__.clone = function(o) {
    "use strict";
    return JSON.parse(JSON.stringify(o));
};

/**
 * Object recursive merging utility.
 *
 * @param  {Object}  origin  the origin object
 * @param  {Object}  add     the object to merge data into origin
 * @param  {Object}  opts    optional options to be passed in
 * @return {Object}
 */
__utils__.mergeObjects = function(origin, add, opts) {
    "use strict";

    if (!add) {
        return origin;
    }

    let options = opts || {},
        keepReferences = options.keepReferences;

    for (let p in add) {
        if (add[p] && add[p].constructor === Object) {
            if (origin[p] && origin[p].constructor === Object) {
                origin[p] = mergeObjects(origin[p], add[p]);
            } else {
                origin[p] = keepReferences ? add[p] : clone(add[p]);
            }
        } else {
            origin[p] = add[p];
        }
    }

    return origin;
};

__utils__.createCaptureArea = function(captureAreaSelector) {
    // TODO : remove dependency to jQuery
    jQuery("<div class='WarpsCaptureArea' style='position:absolute; top:0; left:0; min-width:1000px; min-height:1000px; z-index:1000; display:block'>" +
        "<div class='holder' style='position:absolute; top:0px; left:20px; min-width:20px; min-height:20px; padding:0 40px; display:block'></div>" +
        "</div>")
        .prependTo('body');

    let holder = jQuery('.WarpsCaptureArea > div.holder');
    let target = jQuery(captureAreaSelector);
    // __utils__.echo("Target tag name " + target.prop('tagName'));
    let counter = 4;
    while (target.prop('tagName') !== 'DIV' && counter-- > 0) {
        target = target.parent();
    }
    holder.append(target.clone());
    holder.width(target.width());
    holder.height(target.height());
};

__utils__.cleanCaptureArea = function() {
    jQuery('.WarpsCaptureArea').remove();
};

__utils__.insertImage = function(nearBy, name, imagePath) {
    let counter = 4;
    let neighbor = jQuery(nearBy);
    while (neighbor.prop('tagName') !== 'DIV' && counter-- > 0) {
        neighbor = neighbor.parent();
    }

    let insert = neighbor.clone().html('')
        .attr('class', 'monitor-inserted')
        .attr('name', name)
        .attr('value', imagePath)
        .attr('label', 'Captured');
    let image = "<img src='" + imagePath + "' />";
    insert.append('<div>' + name + '</div>');
    insert.append('<div>' + image + '</div>');

    neighbor.after(insert);
};

/**
 * Get attribute as an integer
 * @param node {Element}
 * @param attrName {String}
 * @param defaultValue {Number}
 * @return {Number}
 * */
__utils__.getIntAttribute = function(node, attrName, defaultValue) {
    if (!defaultValue) {
        defaultValue = 0;
    }

    let value = node.getAttribute(attrName);
    if (!value) {
        value = defaultValue;
    }

    return parseInt(value);
};

/**
 * Increase the attribute value as if it's an integer
 * @param node {Element}
 * @param attrName {String}
 * @param add {Number}
 * */
__utils__.increaseIntAttribute = function(node, attrName, add) {
    let value = node.getAttribute(attrName);
    if (!value) {
        value = '0';
    }

    value = parseInt(value) + add;
    node.setAttribute(attrName, value)
};

/**
 * Get the sibling index
 * @param node {Node} the node
 * */
__utils__.siblingIndex = function(node) {
    let parent = node.parentNode;
    if (parent == null) {
        return false
    }

    let i = 0;
    let found = false;
    for (let n in parent.childNodes.values()) {
        if (n === node) {
            found = true;
            break
        }
        ++i
    }

    // assert(found)

    return found ? i : false;
};

/**
 * Get attribute as an integer
 * */
__utils__.getReadableNodeName = function(node) {
    let name = node.tagName
        + (node.id ? ("#" + node.id) : "")
        + (node.className ? ("#" + node.className) : "");

    let seq = this.getIntAttribute(node, "_seq", -1);
    if (seq >= 0) {
        name += "-" + seq;
    }

    return name;
};

/**
 * Clean node's textContent
 * @param textContent {String} the string to clean
 * @return {String} The clean string
 * */
__utils__.getCleanTextContent = function(textContent) {
    // all control characters
    // @see http://www.asciima.com/
    textContent = textContent.replace(/[\x00-\x1f]/g, " ");

    // combine all blanks into one " " character
    textContent = textContent.replace(/\s+/g, " ");

    return textContent.trim();
};

/**
 * Get clean, merged textContent from node list
 * @param nodeOrList {NodeList|Array|Node} the node from which we extract the content
 * @return {String} The clean string, "" if no text content available.
 * */
__utils__.getMergedTextContent = function(nodeOrList) {
    if (!nodeOrList) {
        return "";
    }

    if (nodeOrList instanceof  Node) {
        return this.getTextContent(nodeOrList);
    }

    let content = "";
    for (let i = 0; i < nodeOrList.length; ++i) {
        if (i > 0) {
            content += " ";
        }
        content += this.getTextContent(nodeOrList[i]);
    }

    return content;
};

/**
 * Get clean node's textContent
 * @param node {Node} the node from which we extract the content
 * @return {String} The clean string, "" if no text content available.
 * */
__utils__.getTextContent = function(node) {
    if (!node || !node.textContent) {
        return "";
    }

    return this.getCleanTextContent(node.textContent);
};

/**
 * Uses canvas.measureText to compute and return the width of the given text of given font in pixels.
 *
 * @param {String} text The text to be rendered.
 * @param {String} font The css font descriptor that text is to be rendered with (e.g. "bold 14px verdana").
 *
 * @see https://stackoverflow.com/questions/118241/calculate-text-width-with-javascript/21015393#21015393
 */
__utils__.getTextWidth = function(text, font) {
    // re-use canvas object for better performance
    let canvas = this.getTextWidth.canvas || (this.getTextWidth.canvas = document.createElement("canvas"));
    let context = canvas.getContext("2d");
    context.font = font;
    let metrics = context.measureText(text);

    return Math.round(metrics.width * 10) / 10
};

/**
 * Uses canvas.measureText to compute and return the width of the given text of given font in pixels.
 *
 * @param {String} text The text to be rendered.
 * @param {HTMLElement} ele The container element.
 * */
__utils__.getElementTextWidth = function(text, ele) {
    let style = window.getComputedStyle(ele);
    let font = style.getPropertyValue('font-weight') + ' '
        + style.getPropertyValue('font-size') + ' '
        + style.getPropertyValue('font-family');

    return this.getTextWidth(text, font);
};

/**
 * Format rectangle
 * @param top {Number}
 * @param left {Number}
 * @param width {Number}
 * @param height {Number}
 * @return {String|Boolean}
 * */
__utils__.formatRect = function(top, left, width, height) {
    if (width === 0 && height === 0) {
        return false;
    }

    return ''
        + Math.round(top * 10) / 10 + ' '
        + Math.round(left * 10) / 10 + ' '
        + Math.round(width * 10) / 10 + ' '
        + Math.round(height * 10) / 10;
};

/**
 * Format a DOMRect object
 * @param rect {DOMRect}
 * @return {String|Boolean}
 * */
__utils__.formatDOMRect = function(rect) {
    if (rect.width === 0 && rect.height === 0) {
        return false;
    }

    return ''
        + Math.round(rect.left * 10) / 10 + ' '
        + Math.round(rect.top * 10) / 10 + ' '
        + Math.round(rect.width * 10) / 10 + ' '
        + Math.round(rect.height * 10) / 10;
};

/**
 * The result is the smallest rectangle which contains the entire element, including the padding, border and margin.
 *
 * @param node {Node|Element|Text}
 * @return {DOMRect|Boolean}
 * */
__utils__.getClientRect = function(node) {
    if (node.nodeType === Node.TEXT_NODE) {
        return this.getTextNodeClientRect(node)
    } else if (node.nodeType === Node.ELEMENT_NODE) {
        return this.getElementClientRect(node)
    } else {
        return null
    }
};

/**
 * The computed style.
 *
 * @param node {Node|Element|Text}
 * @param propertyNames {Array}
 * @return {String|Boolean}
 * */
__utils__.getComputedStyle = function(node, propertyNames) {
    if (node.nodeType === Node.ELEMENT_NODE) {
        let style = window.getComputedStyle(node, null);
        // TODO: simplify the notation
        return propertyNames.map(propertyName => __utils__.getPropertyValue(style, propertyName)).join("; ")
    } else {
        return null
    }
};

/**
 * Get a simplified property value of computed style.
 *
 * @param style {CSSStyleDeclaration}
 * @param propertyName {String}
 * @return {String}
 * */
__utils__.getPropertyValue = function(style, propertyName) {
    let value = style.getPropertyValue(propertyName);

    if (!value || value === '') {
        return ''
    }

    if (propertyName === 'font-size') {
        value = value.substring(0, value.lastIndexOf('px'))
    } else if (propertyName === 'color' || propertyName === 'background-color') {
        value = __utils__.shortHex(__utils__.rgb2hex(value));
        // skip prefix '#'
        value = value.substring(1)
    }

    return value
};

/**
 * Color rgb(a) format to hex
 *
 * rgb(255, 255, 0) -> #
 *
 * @param rgb {String}
 * @return {String}
 * */
__utils__.rgb2hex = function(rgb) {
    rgb = rgb.match(/^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?/i);
    return (rgb && rgb.length === 4) ? "#" +
        ("0" + parseInt(rgb[1],10).toString(16)).slice(-2) +
        ("0" + parseInt(rgb[2],10).toString(16)).slice(-2) +
        ("0" + parseInt(rgb[3],10).toString(16)).slice(-2) : '';
};

/**
 * CSS Hex to Shorthand Hex conversion
 * @param hex {String}
 * @return {String}
 * */
__utils__.shortHex = function(hex) {
    if ((hex.charAt(1) === hex.charAt(2))
        && (hex.charAt(3) === hex.charAt(4))
        && (hex.charAt(5) === hex.charAt(6))) {
        hex = "#" + hex.charAt(1) + hex.charAt(3) + hex.charAt(5);
    }

    // the most simple case: all chars are the same
    if (hex.length === 4) {
        let c = hex.charAt(1);
        if (hex.charAt(2) === c && hex.charAt(3) === c) {
            return '#' + c
        }
    }

    return hex
};

/**
 * Add to attribute
 *
 * @param node {Node|Element|Text}
 * @param attributeName {String}
 * @param key {String}
 * @param value {Object}
 * */
__utils__.addTuple = function(node, attributeName, key, value) {
    let attributeValue = node.getAttribute(attributeName) || "";
    if (attributeValue.length > 0) {
        attributeValue += " "
    }
    attributeValue += key + ":" + value.toString();
    node.setAttribute(attributeName, attributeValue);
};

/**
 * The result is the smallest rectangle which contains the entire element, including the padding, border and margin.
 *
 * Properties other than width and height are relative to the top-left of the viewport.
 *
 * @see https://idiallo.com/javascript/element-postion
 * @see https://stackoverflow.com/questions/442404/retrieve-the-position-x-y-of-an-html-element
 *
 * @param ele {Node|Element}
 * @return {DOMRect|Boolean}
 * */
__utils__.getElementClientRect = function(ele) {
    let bodyRect = this.bodyRect || (this.bodyRect = document.body.getBoundingClientRect());
    let r = ele.getBoundingClientRect();

    if (r.width <= 0 || r.height <= 0) {
        return false
    }

    let top = r.top - bodyRect.top;
    let left = r.left - bodyRect.left;

    return new DOMRect(left, top, r.width, r.height);
};

/**
 * Get the client rect of a text node
 *
 * @param node {Node|Text}
 * @return {DOMRect|Boolean}
 * */
__utils__.getTextNodeClientRect = function(node) {
    let bodyRect = this.bodyRect || (this.bodyRect = document.body.getBoundingClientRect());

    let rect = false;
    let text = this.getTextContent(node);
    if (text.length > 0) {
        let range = document.createRange();
        range.selectNodeContents(node);
        let rects = range.getClientRects();
        if (rects.length > 0) {
            let r = rects[0];
            if (r.width > 0 && r.height > 0) {
                let top = r.top - bodyRect.top;
                let left = r.left - bodyRect.left;
                rect = new DOMRect(left, top, r.width, r.height);
            }
        }
    }

    return rect;
};

/**
 * Calculate visualization info and do human actions
 * */
__utils__.visualizeHumanize = function() {
    "use strict";

    // if (document.doctype.name !== "html") {
    //     return
    // }

    if (!document.body || !document.body.firstChild) {
        return
    }

    // traverse the DOM and compute necessary data, we must compute data before we perform humanization
    new WarpsNodeTraversor(new WarpsNodeVisitor(), {}).traverse(document.body);

    // do something like a human being
    // humanize(document.body);

    // if any script error occurs, the flag can NOT be seen
    document.body.setAttribute("data-error", '0');
};
