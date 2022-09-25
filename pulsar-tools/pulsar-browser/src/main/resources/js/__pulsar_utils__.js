"use strict";

let __pulsar_utils__ = function () {
    this.config = __pulsar_CONFIGS || __pulsar_DEFAULT_CONFIGS;

    this.fineHeight = 4000;
    this.fineNumAnchor = 100;
    this.fineNumImage = 20;
};

/**
 * @param scroll The count to scroll down
 * @return {Object|boolean}
 * */
__pulsar_utils__.waitForReady = function(scroll = 3) {
    return this.checkStatus(scroll);
};

/**
 * @param scroll The count to scroll down
 * @return {Object|boolean}
 * */
__pulsar_utils__.checkStatus = function(scroll = 3) {
    if (!document) {
        return false
    }

    if (!document.__pulsar__Data) {
        // initialization
        this.createDataIfAbsent();
        this.updateStat(true);
    }

    let status = document.__pulsar__Data.trace.status;
    status.n += 1;

    if (status.scroll < scroll) {
        window.scrollBy(0, 500);
        status.scroll += 1;
    }

    let ready = this.isActuallyReady();
    if (!ready) {
        return false
    }

    if (this.isBrowserError()) {
        document.__pulsar__Data.trace.status.ec = document.querySelector(".error-code").textContent
    }

    // The document is ready
    return JSON.stringify(document.__pulsar__Data)
};

__pulsar_utils__.isBrowserError = function () {
    if (document.documentURI.startsWith("chrome-error")) {
        return true
    }

    return false
};

__pulsar_utils__.createDataIfAbsent = function() {
    if (!document.__pulsar__Data) {
        let location;
        if (window.location instanceof Location) {
            location = window.location.href
        } else {
            location = window.location
        }

        document.__pulsar__Data = {
            trace: {
                status: { n: 0, scroll: 0, idl: 0, st: "", r: "", ec: "" },
                initStat: null,
                lastStat: {w: 0, h: 0, na: 0, ni: 0, nst: 0, nnm: 0},
                lastD:    {w: 0, h: 0, na: 0, ni: 0, nst: 0, nnm: 0},
                initD:    {w: 0, h: 0, na: 0, ni: 0, nst: 0, nnm: 0}
            },
            urls: {
                URL: document.URL,
                baseURI: document.baseURI,
                location: location,
                documentURI: document.documentURI,
                referrer: document.referrer
            }
        };
    }
};

__pulsar_utils__.writeData = function() {
    if (!document.body) {
        return false
    }

    let script = document.getElementById(__pulsar_CONFIGS.SCRIPT_SECTION_ID);
    if (script != null) {
        return
    }

    script = document.createElement('script');
    script.id = __pulsar_CONFIGS.SCRIPT_SECTION_ID;
    script.type = 'text/javascript';

    let pulsarData = JSON.stringify(document.__pulsar__Data, null, 3);
    script.textContent = "\n" + `;let __pulsar__Data = ${pulsarData};\n`;

    document.body.appendChild(script);
};

/**
 * Check if the document is ready to analyze.
 * A document is hardly be perfect ready in time, since it's very common there are very slow sub resources to wait for.
 * */
__pulsar_utils__.isActuallyReady = function() {
    // unexpected
    if (!document.body) {
        return false
    }

    this.updateStat();

    if (!document.__pulsar__Data) {
        return false
    }

    let ready = false;
    let trace = document.__pulsar__Data.trace;
    let status = trace.status;
    let d = trace.lastD;

    // all sub resources are loaded, the document is ready now
    if (status.st === "c") {
        // assert(document.readyState === "complete")
        status.r = "st";
        ready = true
    }

    // The DOM is very good for analysis, no wait for more information
    let stat = trace.lastStat;
    if (status.n > 20 && stat.h >= this.fineHeight
        && stat.na >= this.fineNumAnchor
        && stat.ni >= this.fineNumImage
    ) {
        if (d.h < 10 && d.na === 0 && d.ni === 0 && d.nst === 0 && d.nnm === 0) {
            // DOM changed since last check, store the latest stat and return false to wait for the next check
            ++status.idl;
            if (status.idl > 10) {
                // idle for 10 seconds
                status.r = "ct";
                ready = true;
            }
        }
    }

    return ready;
};

__pulsar_utils__.isIdle = function(init = false) {
    let idle = false;
    let trace = document.__pulsar__Data.trace;
    let status = trace.status;
    let d = trace.lastD;
    if (d.h < 10 && d.na === 0 && d.ni === 0 && d.nst === 0 && d.nnm === 0) {
        // DOM changed since last check, store the latest stat and return false to wait for the next check
        ++status.idl;
        if (status.idl > 5) {
            // idle for 5 seconds
            idle = true;
        }
    }
    return idle
};

/**
 * @return {Object}
 * */
__pulsar_utils__.updateStat = function(init = false) {
    if (!document.body) {
        return
    }

    const config = __pulsar_CONFIGS;
    const viewPortWidth = config.viewPortWidth;
    const viewPortHeight = config.viewPortHeight;
    const maxWidth = 1.2 * viewPortWidth;
    const fineWidth = 300;

    let width = 0;
    let height = 0;
    let na = 0;  // anchor
    let ni = 0;  // image
    let nst = 0; // short text in first screen
    let nnm = 0; // number like text in first screen

    if (!this.isBrowserError()) {
        document.body.__pulsar_forEach((node) => {
            if (node.__pulsar_isIFrame()) {
                return
            }

            if (node.__pulsar_isAnchor()) ++na;
            if (node.__pulsar_isImage() && !node.__pulsar_isSmallImage()) ++ni;

            if (node.__pulsar_isText() && node.__pulsar_nScreen() <= 20) {
                let isShortText = node.__pulsar_isShortText();
                let isNumberLike = isShortText && node.__pulsar_isNumberLike();
                if (isShortText) {
                    ++nst;
                    if (isNumberLike) {
                        ++nnm;
                    }

                    let ele = node.__pulsar_bestElement();
                    if (ele != null && !init && !ele.hasAttribute("_ps_tp")) {
                        // not set at initialization, it's lazy loaded
                        ele.setAttribute("_ps_lazy", "1")
                    }

                    if (ele != null) {
                        let type = isNumberLike ? "nm" : "st";
                        ele.setAttribute("_ps_tp", type);
                    }
                }
            }

            if (node.__pulsar_isDiv() && node.scrollWidth > width && node.scrollWidth < maxWidth) width = node.scrollWidth;
            if (node.__pulsar_isDiv() && node.scrollWidth >= fineWidth && node.scrollHeight > height) height = node.scrollHeight;
        });
    }

    // unexpected but occurs when do performance test to parallel harvest Web sites
    if (!document.__pulsar__Data) {
        return
    }

    let trace = document.__pulsar__Data.trace;
    let initStat = trace.initStat;
    if (!initStat) {
        initStat = { w: width, h: height, na: na, ni: ni, nst: nst, nnm: nnm };
        trace.initStat = initStat
    }
    let lastStat = trace.lastStat;
    let lastStatus = trace.status;
    let state = document.readyState.substr(0, 1);
    let newMultiStatus = {
        status: {n: lastStatus.n, scroll: lastStatus.scroll, idl: lastStatus.idl, st: state, r: lastStatus.r},
        lastStat: {w: width, h: height, na: na, ni: ni, nst: nst, nnm: nnm},
        // changes from last round
        lastD: {
            w: width - lastStat.w,
            h: height - lastStat.h,
            na: na - lastStat.na,
            ni: ni - lastStat.ni,
            nst: nst - lastStat.nst,
            nnm: nnm - lastStat.nnm
        },
        // changes from the initialization
        initD: {
            w: width - initStat.w,
            h: height - initStat.h,
            na: na - initStat.na,
            ni: ni - initStat.ni,
            nst: nst - initStat.nst,
            nnm: nnm - initStat.nnm
        }
    };

    document.__pulsar__Data.trace = Object.assign(trace, newMultiStatus)
};

/**
 * @param {Number} ratio The ratio of the page's height to scroll to, default is 0.5
 * */
__pulsar_utils__.scrollToMiddle = function(ratio = 0.5) {
    if (!document || !document.documentElement || !document.body) {
        return
    }

    if (ratio < 0 || ratio > 1) {
        ratio = 0.5
    }

    let x = 0;
    let y = Math.max(
        document.documentElement.scrollHeight,
        document.documentElement.clientHeight,
        document.body.scrollHeight
    );
    y = Math.min(y, 15000) * ratio

    window.scrollTo(x, y)
};

__pulsar_utils__.scrollToBottom = function() {
    if (!document || !document.documentElement || !document.body) {
        return
    }

    let x = 0;
    let y = Math.max(
        document.documentElement.scrollHeight,
        document.documentElement.clientHeight,
        document.body.scrollHeight
    );
    y = Math.min(y, 15000)

    window.scrollTo(x, y)
};

__pulsar_utils__.scrollUp = function() {
    if (!document.__pulsar__Data) {
        // TODO: this occurs when do performance test, but the reason is not investigated
        // return false
    }

    window.scrollBy(0, -500);
};

__pulsar_utils__.scrollToTop = function() {
    window.scrollTo(0, 0)
};

__pulsar_utils__.scrollDown = function() {
    if (!document.__pulsar__Data) {
        // TODO: this occurs when do performance test, but the reason is not investigated
        // return false
    }

    window.scrollBy(0, 500);
};

__pulsar_utils__.scrollDownN = function(scrollCount = 5) {
    if (!document.__pulsar__Data) {
        // TODO: this occurs when do performance test, but the reason is not investigated
        // return false
    }

    let status = document.__pulsar__Data.trace.status;

    window.scrollBy(0, 500);
    status.scroll += 1;

    return status.scroll >= scrollCount
};

/**
 * Check if a element be visible
 *
 * @param  {String} selector
 * @return boolean
 */
__pulsar_utils__.isVisible = function(selector) {
    let ele = document.querySelector(selector)
    if (ele == null) {
        return false
    }
    return this.isElementVisible(ele)
}

/**
 * Check if a element be visible.
 *
 * @param  {Element} element
 * @return boolean
 */
__pulsar_utils__.isElementVisible = function(element) {
    if (!element.ownerDocument || !element.ownerDocument.defaultView)
        return true;

    const style = element.ownerDocument.defaultView.getComputedStyle(element);
    if (!style || style.visibility === 'hidden')
        return false;
    if (style.display === 'contents') {
        // display:contents is not rendered itself, but its child nodes are.
        for (let child = element.firstChild; child; child = child.nextSibling) {
            if (child.nodeType === 1 /* Node.ELEMENT_NODE */ && this.isElementVisible(child))
            return true;
            if (child.nodeType === 3 /* Node.TEXT_NODE */ && this.isVisibleTextNode(child))
            return true;
        }
        return false;
    }
    const rect = element.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
}

/**
 * Check if a text node be visible.
 *
 * @param  {Node} node
 * @return boolean
 */
__pulsar_utils__.isVisibleTextNode = function (node) {
    // https://stackoverflow.com/questions/1461059/is-there-an-equivalent-to-getboundingclientrect-for-text-nodes
    const range = document.createRange();
    range.selectNode(node);
    const rect = range.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
}

/**
 * Test if a element is checked.
 *
 * @param  {String} selector
 * @return boolean
 */
__pulsar_utils__.isChecked = function(selector) {
    let ele = document.querySelector(selector)
    if (ele == null) {
        return false
    }
    return this.isElementChecked(ele)
}

/**
 * Test if a element is checked.
 *
 * @param  {Element} element
 * @return boolean
 */
__pulsar_utils__.isElementChecked = function(element) {
    if (['checkbox', 'radio'].includes(element.getAttribute('role') || '')) {
        return element.getAttribute('aria-checked') === 'true';
    }

    if (element.nodeName !== 'INPUT') {
        throw this.createStacklessError('Not a checkbox or radio button');
    }
    if (element instanceof HTMLInputElement) {
        if (!['radio', 'checkbox'].includes(element.type.toLowerCase())) {
            throw this.createStacklessError('Not a checkbox or radio button');
        }

        return element.checked
    }

    return false
}

/**
 * Scroll into view.
 *
 * @param {String} selector The element to scroll to
 * */
__pulsar_utils__.scrollIntoView = function(selector) {
    let ele = document.querySelector(selector)
    if (ele) {
        ele.scrollIntoView(
            {
                block: "start",
                inline: "nearest",
                behavior: 'auto',
            }
        )
    }
};

/**
 * Select the first element and click it
 *
 * @param  {String} selector
 * @return
 */
__pulsar_utils__.click = function(selector) {
    let ele = document.querySelector(selector)
    if (ele instanceof HTMLElement) {
        ele.click()
    }
}

/**
 * Select the first element and click it
 *
 * @param  {String} selector
 * @param  {String} pattern
 * @return
 */
__pulsar_utils__.clickMatches = function(selector, pattern) {
    let elements = document.querySelectorAll(selector)
    for (let ele of elements) {
        if (ele instanceof HTMLElement) {
            let text = ele.textContent
            if (text.match(pattern)) {
                ele.scrollIntoView()
                ele.click()
            }
        }
    }
}

/**
 * Select the first element and click it.
 *
 * @param  {String} selector
 * @param  {String} attrName
 * @param  {String} pattern
 * @return
 */
__pulsar_utils__.clickMatches = function(selector, attrName, pattern) {
    let elements = document.querySelectorAll(selector)
    for (let ele of elements) {
        if (ele instanceof HTMLElement) {
            let attrValue = ele.getAttribute(attrName)
            if (attrValue.match(pattern)) {
                ele.scrollIntoView()
                ele.click()
                return
            }
        }
    }
}

/**
 * Select the first element and click it.
 *
 * @param  {number} n The n-th anchor.
 * @param  {string|null} rootSelector The n-th anchor.
 * @return {string|null}
 */
__pulsar_utils__.clickNthAnchor = function(n, rootSelector) {
    let rootNode
    if (!rootSelector) {
        rootNode = document.body
    } else {
        rootNode = document.querySelector(rootSelector)
    }

    if (!rootNode) {
        return null
    }

    let c = 0
    let href = null
    let visitor = function () {}

    visitor.head = function (node, depth) {
        if (node instanceof HTMLElement
            && node.__pulsar_isAnchor()
            && node.__pulsar_maybeClickable()
        ) {
            ++c;
            if (c === n) {
                visitor.stopped = true
                node.scrollIntoView()
                href = node.getAttribute("href")
                node.click()
            }
        }
    };

    new __pulsar_NodeTraversor(visitor).traverse(rootNode)

    return href
}

/**
 * Select the first element and check it if not checked.
 *
 * @param  {String} selector
 * @return
 */
__pulsar_utils__.check = function(selector) {
    if (!this.isChecked(selector)) {
        let ele = document.querySelector(selector)
        if (ele instanceof HTMLElement) {
            ele.click()
        }
    }
}

/**
 * Select the first element and uncheck it if checked.
 *
 * @param  {String} selector
 * @return
 */
__pulsar_utils__.uncheck = function(selector) {
    if (this.isChecked(selector)) {
        let ele = document.querySelector(selector)
        if (ele instanceof HTMLElement) {
            ele.click()
        }
    }
}

/**
 * Select the first element and extract the text
 *
 * @param  {String} selector
 * @return {String}
 */
__pulsar_utils__.outerHTML = function(selector) {
    let element = document.querySelector(selector)
    if (element != null) {
        return element.outerHTML
    }
    return null
};

/**
 * Select the first element and extract the text
 *
 * @param  {String} selector
 * @return {String}
 */
__pulsar_utils__.firstText = function(selector) {
    let element = document.querySelector(selector)
    if (element != null) {
        return element.textContent
    }
    return null
};

/**
 * Select elements and extract the texts
 *
 * @param  {String} selector
 * @return {Array}
 */
__pulsar_utils__.allTexts = function(selector) {
    let elements = document.querySelectorAll(selector)
    return elements.map(e => e.textContent)
};

/**
 * Select the first element and extract the text
 *
 * @param  {String} selector
 * @param  {String} attrName
 * @return {String}
 */
__pulsar_utils__.firstAttr = function(selector, attrName) {
    let element = document.querySelector(selector)
    if (element != null) {
        return element.getAttribute(attrName)
    }
    return null
};

/**
 * Select elements and extract the texts
 *
 * @param  {String} selector
 * @param  {String} attrName
 * @return {Array}
 */
__pulsar_utils__.allAttrs = function(selector, attrName) {
    let elements = document.querySelectorAll(selector)
    return elements.map(e => e.getAttribute(attrName))
};

/**
 * Select elements and extract the texts
 *
 * @param  {string} pattern
 * @param  {string} frameNameOrId
 * @return {string|null}
 */
__pulsar_utils__.findMatches = function(pattern, frameNameOrId) {
    let expression = `(//frame|//iframe)[@name="${frameNameOrId}" or @id="${frameNameOrId}"]`
    let frameContext = document.evaluate(expression, document,null,
        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null
    ).snapshotItem(0).contentDocument;
    let element = frameContext.__pulsar_findMatches(pattern)
    return element?.textContent
};

/**
 * Select elements and extract the texts
 *
 * @param  {String} selector
 * @param  {String} attrName
 * @return {Array}
 */
__pulsar_utils__.findMatchesForAttrs = function(selector, attrName) {
    let elements = document.querySelectorAll(selector)
    return elements.map(e => e.getAttribute(attrName))
};

/**
 * Select elements and extract the texts
 *
 * @param  {string} selector
 * @param  {string} attrName
 * @return {any}
 */
__pulsar_utils__.doForAllFrames = function(selector, attrName) {
    this.__doForAllFramesRecursively(window, 0, selector, attrName)
    return "hello"
};

/**
 * Select elements and extract the texts.
 *
 * @param  {Window} rootFrame
 * @param  {string} selector
 * @param  {string} attrName
 * @return {any}
 */
__pulsar_utils__.__doForAllFramesRecursively = function(rootFrame, depth, selector, attrName) {
    let doc = rootFrame.document
    doc.body.style.background = "green";
    doc.body.setAttribute("data-user", "vincent")
    doc.body.setAttribute("data-depth", depth.toString())
    doc.body.setAttribute("data-frames", rootFrame.frames.length.toString())

    console.log(rootFrame.name)

    // doc.body.__pulsar_forEachElement(e => {
    //     let textContent = e.textContent
    //     if (textContent.contains("PRESS")) {
    //         // e.textContent = "PRESSED"
    //     }
    // })

    let dep = window.document.body.getAttribute("data-depth") || 0
    if (depth > dep) {
        window.document.body.setAttribute("data-depth", depth.toString())
    }

    const frames = rootFrame.frames; // or const frames = window.parent.frames;
    for (let i = 0; i < frames.length; i++) {
        // do something with each subframe as frames[i]
        let frame = frames[i]
        let doc1 = frame.document

        this.__doForAllFramesRecursively(frame, depth + 1, selector, attrName)
    }

    return frames.length
};

/**
 * Clones an object.
 *
 * @param  {Object} o
 * @return {Object}
 */
__pulsar_utils__.clone = function(o) {
    return JSON.parse(JSON.stringify(o))
};

/**
 * Get attribute as an integer
 * @param node {Element}
 * @param attrName {String}
 * @param defaultValue {Number}
 * @return {Number}
 * */
__pulsar_utils__.getIntAttribute = function(node, attrName, defaultValue) {
    if (!defaultValue) {
        defaultValue = 0;
    }

    let value = node.getAttribute(attrName);
    if (!value) {
        return defaultValue;
    }

    return parseInt(value);
};

/**
 * Increase the attribute value as if it's an integer
 * @param node {Element}
 * @param attrName {String}
 * @param add {Number}
 * */
__pulsar_utils__.increaseIntAttribute = function(node, attrName, add) {
    let value = node.getAttribute(attrName);
    if (!value) {
        value = '0';
    }

    value = parseInt(value) + add;
    node.setAttribute(attrName, value.toString())
};

/**
 * Get attribute as an integer
 * */
__pulsar_utils__.getReadableNodeName = function(node) {
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
__pulsar_utils__.getCleanTextContent = function(textContent) {
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
__pulsar_utils__.getMergedTextContent = function(nodeOrList) {
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
__pulsar_utils__.getTextContent = function(node) {
    if (!node || !node.textContent || node.textContent.length === 0) {
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
__pulsar_utils__.getTextWidth = function(text, font) {
    // re-use canvas object for better performance
    let canvas = this.getTextWidth.canvas || (__pulsar_utils__.getTextWidth.canvas = document.createElement("canvas"));
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
__pulsar_utils__.getElementTextWidth = function(text, ele) {
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
__pulsar_utils__.formatRect = function(top, left, width, height) {
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
__pulsar_utils__.formatDOMRect = function(rect) {
    if (!rect || (rect.width === 0 && rect.height === 0)) {
        return false;
    }

    return ''
        + Math.round(rect.left * 10) / 10 + ' '
        + Math.round(rect.top * 10) / 10 + ' '
        + Math.round(rect.width * 10) / 10 + ' '
        + Math.round(rect.height * 10) / 10;
};

/**
 * Format a DOMRectList object
 * @param rectList {DOMRectList}
 * @return {String}
 * */
__pulsar_utils__.formatDOMRectList = function(rectList) {
    if (!rectList) {
        return '[]';
    }

    let r = "["
    for (let i = 0; i < rectList.length; ++i) {
        r += "{"
        r += this.formatDOMRect(rectList.item(i))
        r += "}, "
    }
    r += "]"

    return r
};

/**
 * The result is the smallest rectangle which contains the entire element, including the padding, border and margin.
 *
 * @param selector {string} The selector to get the element from.
 * @return {String}
 * */
__pulsar_utils__.queryClientRects = function(selector) {
    let ele = document.querySelector(selector);
    if (!ele) {
        return null;
    }

    return this.formatDOMRectList(ele.getClientRects())
};

/**
 * The result is the smallest rectangle which contains the entire element, including the padding, border and margin.
 *
 * @param selector {string} The selector to get the element from.
 * @return {DOMRect|String|Boolean}
 * */
__pulsar_utils__.queryClientRect = function(selector) {
    let ele = document.querySelector(selector);
    if (!ele) {
        return null;
    }

    let rect = ele.__pulsar_getRect()
    return this.formatDOMRect(rect)
};

/**
 * The result is the smallest rectangle which contains the entire element, including the padding, border and margin.
 *
 * @param node {Node|Element}
 * @return {DOMRect|Boolean|null}
 * */
__pulsar_utils__.getClientRect = function(node) {
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
 * @return {Object|Boolean|null}
 * */
__pulsar_utils__.getComputedStyle = function(node, propertyNames) {
    if (node.nodeType === Node.ELEMENT_NODE) {
        let styles = {};
        let computedStyle = window.getComputedStyle(node, null);
        propertyNames.forEach(propertyName =>
            styles[propertyName] = this.getPropertyValue(computedStyle, propertyName)
        );
        return styles
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
__pulsar_utils__.getPropertyValue = function(style, propertyName) {
    let value = style.getPropertyValue(propertyName);

    if (!value || value === '') {
        return ''
    }

    if (propertyName === 'font-size') {
        value = value.substring(0, value.lastIndexOf('px'))
    } else if (propertyName === 'color' || propertyName === 'background-color') {
        value = this.shortenHex(__pulsar_utils__.rgb2hex(value));
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
__pulsar_utils__.rgb2hex = function(rgb) {
    let parts = rgb.match(/^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?/i);
    return (parts && parts.length === 4) ? "#" +
        ("0" + parseInt(parts[1],10).toString(16)).slice(-2) +
        ("0" + parseInt(parts[2],10).toString(16)).slice(-2) +
        ("0" + parseInt(parts[3],10).toString(16)).slice(-2) : '';
};

/**
 * CSS Hex to Shorthand Hex conversion
 * @param hex {String}
 * @return {String}
 * */
__pulsar_utils__.shortenHex = function(hex) {
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
__pulsar_utils__.addTuple = function(node, attributeName, key, value) {
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
__pulsar_utils__.getElementClientRect = function(ele) {
    let bodyRect = this.bodyRect || (__pulsar_utils__.bodyRect = document.body.getBoundingClientRect());
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
 * @return {DOMRect|null}
 * */
__pulsar_utils__.getTextNodeClientRect = function(node) {
    let bodyRect = this.bodyRect || (__pulsar_utils__.bodyRect = document.body.getBoundingClientRect());

    let rect = null;
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
 * The full page metrics
 * */
__pulsar_utils__.getFullPageMetrics = function() {
    let metrics = {
        width: Math.max(window.innerWidth, document.body.scrollWidth, document.documentElement.scrollWidth) | 0,
        height: Math.max(window.innerHeight, document.body.scrollHeight, document.documentElement.scrollHeight) | 0,
        deviceScaleFactor: window.devicePixelRatio || 1,
        mobile: typeof window.orientation !== 'undefined'
    };

    return JSON.stringify(metrics)
};

/**
 * Generate meta data
 * */
__pulsar_utils__.generateMetadata = function() {
    let config = __pulsar_CONFIGS
    let meta = document.getElementById(config.META_INFORMATION_ID);
    if (meta != null) {
        // already generated
        return
    }

    let date = new Date();

    let ele = document.createElement("input");
    ele.setAttribute("type", "hidden");
    ele.setAttribute("id", config.META_INFORMATION_ID);
    ele.setAttribute("domain", document.domain);
    ele.setAttribute("view-port", config.viewPortWidth + "x" + config.viewPortHeight);
    ele.setAttribute("date-time", date.toLocaleDateString() + " " + date.toLocaleTimeString());
    ele.setAttribute("timestamp", date.getTime().toString());

    document.body.appendChild(ele);
};

/**
 * Calculate visualization info and do human actions
 * */
__pulsar_utils__.compute = function() {
    if (!document.body || !document.body.firstChild) {
        return
    }

    const DATA_ERROR = "data-error";

    let done = document.body.hasAttribute(DATA_ERROR);
    if (done) {
        return
    }

    this.scrollToTop();

    // calling window.stop will pause all resource loading
    window.stop();

    this.updateStat();
    this.writeData();

    // remove temporary flags
    document.body.__pulsar_forEachElement(ele => {
        ele.removeAttribute("_ps_tp")
    });

    // traverse the DOM and compute necessary data, we must compute data before we perform humanization
    new __pulsar_NodeTraversor(new __pulsar_NodeFeatureCalculator()).traverse(document.body);

    this.generateMetadata();

    this.addProjectSpecifiedData();

    // if any script error occurs, the flag can NOT be seen
    document.body.setAttribute(DATA_ERROR, '0');

    return JSON.stringify(document.__pulsar__Data)
};

__pulsar_utils__.addProjectSpecifiedData = function() {

};

/**
 * Create a error without stack trace
 *
 * @param {String} message
 * @return {Error}
 * */
__pulsar_utils__.createStacklessError = function (message) {
    const error = new Error(message);
    // Chromium/WebKit should delete the stack instead.
    delete error.stack;
    return error;
}

/**
 * Return a + b.
 *
 * This simple function is used for a smoke test.
 *
 * @param a {Number}
 * @param b {Number}
 * @return {Number}
 * */
__pulsar_utils__.add = function(a, b) {
    return a + b
};

/**
 * Return type infomation.
 *
 * @return {Object}
 * */
__pulsar_utils__.typeInfo = function() {
    let fields = Object.keys(__pulsar_utils__).join(", ")
    return fields
};
