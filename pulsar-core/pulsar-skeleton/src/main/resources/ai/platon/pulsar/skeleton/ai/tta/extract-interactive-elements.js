(function() {
    const elements = [];
    const interactiveSelectors = [
        'a[href]', 'button', 'input', 'select', 'textarea', 
        '[onclick]', '[role="button"]', '[tabindex]',
        'label[for]', '[contenteditable="true"]', 
        '[type="submit"]', '[type="button"]', '[type="search"]',
        '.btn', '.button', '.clickable'
    ];
    
    // 收集所有交互元素
    const seenElements = new Set();
    
    interactiveSelectors.forEach(selector => {
        try {
            document.querySelectorAll(selector).forEach((el, globalIndex) => {
                // 避免重复元素
                const elementKey = el.tagName + (el.id || '') + (el.className || '') + (el.textContent || '').slice(0, 20);
                if (seenElements.has(elementKey)) return;
                seenElements.add(elementKey);
                
                // 只收集可见元素
                const rect = el.getBoundingClientRect();
                const isVisible = rect.width > 0 && rect.height > 0 && 
                                el.offsetParent !== null && 
                                getComputedStyle(el).visibility !== 'hidden';
                
                if (isVisible) {
                    const id = el.id || `elem-${el.tagName.toLowerCase()}-${globalIndex}`;
                    
                    elements.push({
                        id: id,
                        tagName: el.tagName.toLowerCase(),
                        selector: generateBestSelector(el),
                        text: (el.textContent || el.value || '').trim().substring(0, 100),
                        type: el.type || null,
                        href: el.href || null,
                        className: el.className || null,
                        placeholder: el.placeholder || null,
                        value: el.value || null,
                        isVisible: isVisible,
                        bounds: {
                            x: Math.round(rect.x),
                            y: Math.round(rect.y),
                            width: Math.round(rect.width),
                            height: Math.round(rect.height)
                        }
                    });
                }
            });
        } catch (e) {
            console.warn('Error processing selector:', selector, e);
        }
    });
    
    // 生成最佳选择器
    function generateBestSelector(element) {
        // 1. 优先使用 ID
        if (element.id) {
            return '#' + element.id;
        }
        
        // 2. 使用 name 属性
        if (element.name) {
            return `[name="${element.name}"]`;
        }
        
        // 3. 使用类名组合
        if (element.className) {
            const classes = element.className.trim().split(/\s+/).filter(c => 
                c.length > 0 && 
                !c.match(/^(ng-|js-|css-|style-|temp-|dynamic-)/) && // 排除动态类名
                c.length < 20 // 排除过长的类名
            );
            if (classes.length > 0) {
                return element.tagName.toLowerCase() + '.' + classes.slice(0, 2).join('.');
            }
        }
        
        // 4. 使用属性选择器
        if (element.getAttribute('data-testid')) {
            return `[data-testid="${element.getAttribute('data-testid')}"]`;
        }
        
        if (element.getAttribute('aria-label')) {
            return `[aria-label="${element.getAttribute('aria-label')}"]`;
        }
        
        // 5. 使用文本内容 (适用于链接和按钮)
        if ((element.tagName === 'A' || element.tagName === 'BUTTON') && element.textContent) {
            const text = element.textContent.trim();
            if (text.length > 0 && text.length < 30) {
                return `${element.tagName.toLowerCase()}:contains("${text}")`;
            }
        }
        
        // 6. 使用 nth-child 选择器
        const parent = element.parentElement;
        if (parent) {
            const siblings = Array.from(parent.children).filter(el => el.tagName === element.tagName);
            if (siblings.length > 1) {
                const index = siblings.indexOf(element) + 1;
                return `${element.tagName.toLowerCase()}:nth-of-type(${index})`;
            }
        }
        
        // 7. 最后使用标签名
        return element.tagName.toLowerCase();
    }
    
    // 按照重要性排序：表单元素 > 按钮 > 链接 > 其他
    elements.sort((a, b) => {
        const getElementPriority = (el) => {
            if (['input', 'textarea', 'select'].includes(el.tagName)) return 1;
            if (el.tagName === 'button' || el.type === 'submit') return 2;
            if (el.tagName === 'a') return 3;
            return 4;
        };
        return getElementPriority(a) - getElementPriority(b);
    });
    
    return elements;
})();

