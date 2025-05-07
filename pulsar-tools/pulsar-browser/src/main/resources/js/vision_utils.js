// VisionUtils.js
/**
 * 解析RGB或RGBA颜色字符串
 * 该函数接受一个颜色字符串作为输入，尝试解析它，并返回一个包含红、绿、蓝和透明度分量的对象
 * 如果输入字符串格式不正确，或者颜色值不在有效范围内，则返回null
 *
 * @param {string} colorStr - 颜色字符串，格式为 "rgb(r, g, b)" 或 "rgba(r, g, b, a)"
 * @returns {Object|null} - 返回一个包含颜色分量的对象，格式为 {r, g, b, a}，如果解析失败则返回null
 */
function parseRGB(colorStr) {
    if (colorStr == null || typeof colorStr !== "string") {
        return null
    }

    // 使用正则表达式匹配颜色字符串，支持 rgb 或 rgba，允许空格，并锚定整个字符串
    const match = colorStr.match(/^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)$/);
    if (!match) return null;

    // 提取匹配到的颜色分量，并将它们转换为数字
    const r = Number(match[1]);
    const g = Number(match[2]);
    const b = Number(match[3]);
    // 透明度分量是可选的，如果未提供，则默认为 1（完全不透明）
    const a = match[4] !== undefined ? Number(match[4]) : 1;

    // 校验颜色值是否合法，红、绿、蓝分量应为整数且在 0 到 255 之间
    if (![r, g, b].every(n => Number.isInteger(n) && n >= 0 && n <= 255)) {
        return null;
    }

    // 校验透明度值是否合法，透明度应在 0 到 1 之间
    if (a < 0 || a > 1) {
        return null;
    }

    // 返回包含颜色分量的对象
    return { r, g, b, a };
}

function isTransparent(rgb) {
    return !rgb || rgb.a === 0;
}

// --- 线性化 & 相对亮度 ---
function linearize(c) {
    c = c / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
}

function relativeLuminance({ r, g, b }) {
    return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b);
}

function contrastRatio(l1, l2) {
    const [bright, dark] = l1 > l2 ? [l1, l2] : [l2, l1];
    return (bright + 0.05) / (dark + 0.05);
}

/**
 * 获取元素的有效背景色
 *
 * 此函数旨在找出给定元素或其父元素中第一个非透明的背景色
 * 如果没有找到任何非透明背景色，则默认返回白色
 *
 * @param {Element} el - 需要检查的HTML元素
 * @returns {Object} - 返回一个包含rgba属性的对象，代表有效背景色
 */
function getEffectiveBgColor(el) {
    // 遍历元素及其所有父元素，直到达到文档根
    while (el && el !== document) {
        // 获取当前元素的背景色字符串
        const colorStr = getComputedStyle(el).backgroundColor;
        // 将背景色字符串解析为RGB对象
        const rgb = parseRGB(colorStr);
        // 如果RGB对象存在且颜色不透明，则返回该颜色
        if (rgb && !isTransparent(rgb)) return rgb;
        // 转到父元素继续检查
        el = el.parentElement;
    }
    // 如果没有找到非透明背景色，则返回白色作为默认值
    return { r: 255, g: 255, b: 255, a: 1 }; // fallback: white
}

// --- 可见性判断 + 视口内 + 面积范围 ---
function isVisible(el) {
    const style = getComputedStyle(el);
    return style.display !== "none" && style.visibility !== "hidden";
}

function isInViewport(el) {
    const rect = el.getBoundingClientRect();
    const maxSize = window.innerWidth * window.innerHeight * 0.8;
    const minSize = 4;
    const area = rect.width * rect.height;

    return (
        area > minSize &&
        area < maxSize &&
        rect.bottom > 0 &&
        rect.top < window.innerHeight &&
        rect.right > 0 &&
        rect.left < window.innerWidth
    );
}

// --- 主函数：惰性遍历视口元素，计算平均背景色 ---
function computeAverageBackgroundColorInViewport() {
    let totalR = 0, totalG = 0, totalB = 0, totalWeight = 0;

    const walker = document.createTreeWalker(
        document.body,
        NodeFilter.SHOW_ELEMENT,
        {
            acceptNode: node => {
                if (!isVisible(node)) return NodeFilter.FILTER_SKIP;
                if (!isInViewport(node)) return NodeFilter.FILTER_SKIP;
                return NodeFilter.FILTER_ACCEPT;
            }
        }
    );

    let node = walker.nextNode();
    while (node) {
        const rect = node.getBoundingClientRect();
        const area = rect.width * rect.height;

        //  && rect.width < 800 && rect.height < 800
        if (node instanceof Element && rect.width > 2 && rect.height > 2 && rect.height < 800) {
            const bg = getEffectiveBgColor(node);
            const weight = area * bg.a;

            totalR += bg.r * weight;
            totalG += bg.g * weight;
            totalB += bg.b * weight;
            totalWeight += weight;
        }

        node = walker.nextNode();
    }

    if (totalWeight === 0) return { r: 255, g: 255, b: 255 };

    return {
        r: Math.round(totalR / totalWeight),
        g: Math.round(totalG / totalWeight),
        b: Math.round(totalB / totalWeight),
    };
}
/**
 * Analyzes the contrast between a given foreground and background color.
 *
 * This function first calculates the relative luminance of both the foreground and background colors.
 * Then, it computes the contrast ratio using these luminance values.
 * Finally, it returns an object containing the contrast ratio and an accessibility level based on WCAG standards.
 *
 * @param {Array} foregroundRGB - An array representing the RGB values of the foreground color, e.g., [R, G, B]
 * @param {Array} backgroundRGB - An array representing the RGB values of the background color, e.g., [R, G, B]
 * @returns {Object} - Returns an object containing:
 *   - [ratio](file://D:\workspace\PulsarRPA\PulsarRPA-3.x\pulsar-tools\pulsar-browser\src\main\resources\js\vision_utils.js#L165-L165): The contrast ratio rounded to two decimal places
 *   - [level](file://D:\workspace\PulsarRPA\PulsarRPA-3.x\pulsar-tools\pulsar-browser\src\main\java\com\github\kklisura\cdt\protocol\v2023\types\log\LogEntry.java#L33-L33): A string indicating the accessibility level based on the contrast ratio:
 *         - "AAA（优秀）" for excellent (ratio ≥ 7)
 *         - "AA（合格）" for acceptable (ratio ≥ 4.5)
 *         - "⚠️ 仅适合大号文字" for suitable only for large text (ratio ≥ 3)
 *         - "❌ 不合格" for insufficient contrast (ratio < 3)
 */
function analyzeContrast(foregroundRGB, backgroundRGB) {
    // Calculate the relative luminance of the foreground color
    const l1 = relativeLuminance(foregroundRGB);

    // Calculate the relative luminance of the background color
    const l2 = relativeLuminance(backgroundRGB);

    // Compute the contrast ratio using the luminance values
    const ratio = contrastRatio(l1, l2);

    // Return the contrast analysis result
    return {
        // Contrast ratio, rounded to 2 decimal places
        ratio: ratio.toFixed(2),

        // Determine accessibility level based on the contrast ratio
        level:
            ratio >= 7
                ? "AAA（Excellent）"
                : ratio >= 4.5
                    ? "AA（Acceptable）"
                    : ratio >= 3
                        ? "⚠️ Suitable only for large text"
                        : "❌ Insufficient contrast",
    };
}

function isColorBright(rgb) {
    if (!rgb) return false;

    const brightness = (rgb.r * 299 + rgb.g * 587 + rgb.b * 114) / 1000;
    return brightness > 128; // 0-255 范围中认为大于128为亮
}

// --- 导出 API ---
window.VisionUtils = {
    parseRGB,
    getEffectiveBgColor,
    computeAverageBackgroundColorInViewport,
    analyzeContrast,
    relativeLuminance,
    contrastRatio,
    isColorBright,
};
