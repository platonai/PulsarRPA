(() => {
    // auxiliary-ts/dom/elementCheckUtils.ts
    function isElementNode(node) {
        return node.nodeType === Node.ELEMENT_NODE;
    }

    function isTextNode(node) {
        return node.nodeType === Node.TEXT_NODE && Boolean(node.textContent?.trim());
    }

    // auxiliary-ts/dom/xpathUtils.ts
    function getParentElement(node) {
        return isElementNode(node) ? node.parentElement : node.parentNode;
    }

    function getCombinations(attributes, size) {
        const results = [];

        function helper(start, combo) {
            if (combo.length === size) {
                results.push([...combo]);
                return;
            }
            for (let i = start; i < attributes.length; i++) {
                combo.push(attributes[i]);
                helper(i + 1, combo);
                combo.pop();
            }
        }

        helper(0, []);
        return results;
    }

    function isXPathFirstResultElement(xpath, target) {
        try {
            const result = document.evaluate(
                xpath,
                document.documentElement,
                null,
                XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
                null
            );
            return result.snapshotItem(0) === target;
        } catch (error) {
            console.warn(`Invalid XPath expression: ${xpath}`, error);
            return false;
        }
    }

    function escapeXPathString(value) {
        if (value.includes("'")) {
            if (value.includes('"')) {
                return "concat(" + value.split(/('+)/).map((part) => {
                    if (part === "'") {
                        return `"'"`;
                    } else if (part.startsWith("'") && part.endsWith("'")) {
                        return `"${part}"`;
                    } else {
                        return `'${part}'`;
                    }
                }).join(",") + ")";
            } else {
                return `"${value}"`;
            }
        } else {
            return `'${value}'`;
        }
    }

    async function generateXPathsForElement(element) {
        // Delegate to sync implementation for single source of truth
        return Promise.resolve(generateXPathsForElementSync(element));
    }

    async function generateComplexXPath(element) {
        // Delegate to sync implementation for single source of truth
        return Promise.resolve(generateComplexXPathSync(element));
    }

    async function generateStandardXPath(element) {
        // Delegate to sync implementation for single source of truth
        return Promise.resolve(generateStandardXPathSync(element));
    }

    async function generatedIdBasedXPath(element) {
        // Delegate to sync implementation for single source of truth
        return Promise.resolve(generatedIdBasedXPathSync(element));
    }

  // BEGIN: Synchronous counterparts
  function generateComplexXPathSync(element) {
    const parts = [];
    let currentElement = element;
    while (currentElement && (isTextNode(currentElement) || isElementNode(currentElement))) {
      if (isElementNode(currentElement)) {
        const el = currentElement;
        let selector = el.tagName.toLowerCase();
        const attributePriority = [
          "data-qa",
          "data-component",
          "data-role",
          "role",
          "aria-role",
          "type",
          "name",
          "aria-label",
          "placeholder",
          "title",
          "alt"
        ];
        const attributes = attributePriority.map((attr) => {
          let value = el.getAttribute(attr);
          if (attr === "href-full" && value) {
            value = el.getAttribute("href");
          }
          return value ? { attr: attr === "href-full" ? "href" : attr, value } : null;
        }).filter((attr) => attr !== null);
        let uniqueSelector = "";
        for (let i = 1; i <= attributes.length; i++) {
          const combinations = getCombinations(attributes, i);
          for (const combo of combinations) {
            const conditions = combo.map((a) => `@${a.attr}=${escapeXPathString(a.value)}`).join(" and ");
            const xpath2 = `//${selector}[${conditions}]`;
            if (isXPathFirstResultElement(xpath2, el)) {
              uniqueSelector = xpath2;
              break;
            }
          }
          if (uniqueSelector)
            break;
        }
        if (uniqueSelector) {
          parts.unshift(uniqueSelector.replace("//", ""));
          break;
        } else {
          const parent = getParentElement(el);
          if (parent) {
            const siblings = Array.from(parent.children).filter(
              (sibling) => sibling.tagName === el.tagName
            );
            const index = siblings.indexOf(el) + 1;
            selector += siblings.length > 1 ? `[${index}]` : "";
          }
          parts.unshift(selector);
        }
      }
      currentElement = getParentElement(currentElement);
    }
    const xpath = "//" + parts.join("/");
    return xpath;
  }
  function generateStandardXPathSync(element) {
    const parts = [];
    while (element && (isTextNode(element) || isElementNode(element))) {
      let index = 0;
      let hasSameTypeSiblings = false;
      const siblings = element.parentElement ? Array.from(element.parentElement.childNodes) : [];
      for (let i = 0; i < siblings.length; i++) {
        const sibling = siblings[i];
        if (sibling.nodeType === element.nodeType && sibling.nodeName === element.nodeName) {
          index = index + 1;
          hasSameTypeSiblings = true;
          if (sibling.isSameNode(element)) {
            break;
          }
        }
      }
      if (element.nodeName !== "#text") {
        const tagName = element.nodeName.toLowerCase();
        const pathIndex = hasSameTypeSiblings ? `[${index}]` : "";
        parts.unshift(`${tagName}${pathIndex}`);
      }
      element = element.parentElement;
    }
    return parts.length ? `/${parts.join("/")}` : "";
  }

  function generatedIdBasedXPathSync(element) {
    if (isElementNode(element) && element.id) {
      return `//*[@id='${element.id}']`;
    }
    return null;
  }

  function generateXPathsForElementSync(element) {
    if (!element) return [];
    const complexXPath = generateComplexXPathSync(element);
    const standardXPath = generateStandardXPathSync(element);
    const idBasedXPath = generatedIdBasedXPathSync(element);
    return [standardXPath, ...(idBasedXPath ? [idBasedXPath] : []), complexXPath];
  }

  function getScrollableElementXpathsSync(topN) {
    const scrollableElems = getScrollableElements(topN);
    const xpaths = [];
    for (const elem of scrollableElems) {
      const allXPaths = generateXPathsForElementSync(elem);
      const firstXPath = allXPaths?.[0] || "";
      xpaths.push(firstXPath);
    }
    return xpaths;
  }
  // END: Synchronous counterparts

  // auxiliary-ts/dom/utils.ts
  function canElementScroll(elem) {
    if (typeof elem.scrollTo !== "function") {
      console.warn("canElementScroll: .scrollTo is not a function.");
      return false;
    }
    try {
      const originalTop = elem.scrollTop;
      elem.scrollTo({
        top: originalTop + 100,
        left: 0,
        behavior: "instant"
      });
      if (elem.scrollTop === originalTop) {
        throw new Error("scrollTop did not change");
      }
      elem.scrollTo({
        top: originalTop,
        left: 0,
        behavior: "instant"
      });
      return true;
    } catch (error) {
      console.warn("canElementScroll error:", error.message || error);
      return false;
    }
  }

  function getNodeFromXpath(xpath) {
    const node = document.evaluate(
      xpath,
      document.documentElement,
      null,
      XPathResult.FIRST_ORDERED_NODE_TYPE,
      null
    ).singleNodeValue;

    if (!node) return null;

    const allXPaths = generateXPathsForElementSync(node);
    return allXPaths?.[0] || null;
  }

  function waitForElementScrollEnd(element, idleMs = 100) {
    return new Promise((resolve) => {
      let scrollEndTimer;
      const handleScroll = () => {
        clearTimeout(scrollEndTimer);
        scrollEndTimer = window.setTimeout(() => {
          element.removeEventListener("scroll", handleScroll);
          resolve();
        }, idleMs);
      };
      element.addEventListener("scroll", handleScroll, { passive: true });
      handleScroll();
    });
  }

  // auxiliary-ts/dom/process.ts
  function getScrollableElements(topN) {
    const docEl = document.documentElement;
    const scrollableElements = [docEl];
    const allElements = document.querySelectorAll("*");
    for (const elem of allElements) {
      const style = window.getComputedStyle(elem);
      const overflowY = style.overflowY;
      const isPotentiallyScrollable = overflowY === "auto" || overflowY === "scroll" || overflowY === "overlay";
      if (isPotentiallyScrollable) {
        const candidateScrollDiff = elem.scrollHeight - elem.clientHeight;
        if (candidateScrollDiff > 0 && canElementScroll(elem)) {
          scrollableElements.push(elem);
        }
      }
    }
    scrollableElements.sort((a, b) => b.scrollHeight - a.scrollHeight);
    if (topN !== void 0) {
      return scrollableElements.slice(0, topN);
    }
    return scrollableElements;
  }
  async function getScrollableElementXpaths(topN) {
    const scrollableElems = getScrollableElements(topN);
    const xpaths = [];
    for (const elem of scrollableElems) {
      const allXPaths = await generateXPathsForElement(elem);
      const firstXPath = allXPaths?.[0] || "";
      xpaths.push(firstXPath);
    }
    return xpaths;
  }
  (() => {
    const closedRoots = /* @__PURE__ */ new WeakMap();
    const nativeAttachShadow = Element.prototype.attachShadow;
    Element.prototype.attachShadow = function(init) {
      const root = nativeAttachShadow.call(this, init);
      if (init.mode === "closed")
        closedRoots.set(this, root);
      return root;
    };
    const backdoor = {
      getClosedRoot: (host) => closedRoots.get(host),
      queryClosed: (host, selector) => {
        const root = closedRoots.get(host);
        return root ? Array.from(root.querySelectorAll(selector)) : [];
      },
      xpathClosed: (host, xp) => {
        const root = closedRoots.get(host);
        if (!root)
          return [];
        const it = document.evaluate(
          xp,
          root,
          null,
          XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
          null
        );
        const out = [];
        for (let i = 0; i < it.snapshotLength; ++i) {
          const n = it.snapshotItem(i);
          if (n)
            out.push(n);
        }
        return out;
      }
    };
    if (!("__stagehand__" in window)) {
      Object.defineProperty(window, "__stagehand__", {
        value: backdoor,
        enumerable: false,
        writable: false,
        configurable: false
      });
    }
  })();

  window.getScrollableElementXpaths = getScrollableElementXpaths;
  window.getScrollableElementXpathsSync = getScrollableElementXpathsSync;
  window.getNodeFromXpath = getNodeFromXpath;
  window.waitForElementScrollEnd = waitForElementScrollEnd;
})();
