// Test script to see what interactive elements are extracted
(function() {
    return Array.from(document.querySelectorAll('*')).map(el => ({
        id: el.id,
        tagName: el.tagName,
        selector: el.tagName.toLowerCase() + (el.id ? ('#' + el.id) : ''),
        text: el.innerText || '',
        type: el.type || null,
        href: el.href || null,
        className: el.className || null,
        placeholder: el.placeholder || null,
        value: el.value || null,
        isVisible: !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length),
        bounds: el.getBoundingClientRect()
    }));
})();
