
(() => {
    try {
        const w = window;
        if (!w.__pulsar_DomObserver) {
            w.__pulsar_DomStamp = 0;
            w.__pulsar_DomLastTs = (performance && performance.now) ? performance.now() : Date.now();
            const obs = new MutationObserver(() => {
                w.__pulsar_DomStamp++;
                w.__pulsar_DomLastTs = (performance && performance.now) ? performance.now() : Date.now();
            });
            // Observe subtree text/content/node additions; attributes are OFF to reduce noise
            const opts = { subtree: true, childList: true, characterData: true };
            // Intentionally DO NOT observe attributes or set attributeFilter
            // This avoids counting class/style/aria toggles as instability
            obs.observe(document, opts);
            w.__pulsar_DomObserver = obs;
        }
        // Bind lifecycle/navigation-ish events once to bump the stamp on non-mutation transitions
        if (!w.__pulsar_DomEventsBound) {
            const bump = () => { try { w.__pulsar_DomStamp++; } catch(_) {} };
            document.addEventListener('readystatechange', bump, { once: false, passive: true });
            document.addEventListener('DOMContentLoaded', bump, { once: true, passive: true });
            window.addEventListener('load', bump, { once: false, passive: true });
            window.addEventListener('pageshow', bump, { once: false, passive: true });
            window.addEventListener('hashchange', bump, { once: false, passive: true });
            window.addEventListener('popstate', bump, { once: false, passive: true });
            document.addEventListener('visibilitychange', bump, { once: false, passive: true });
            w.__pulsar_DomEventsBound = true;
        }
        if (!w.__pulsar_GetDomSignature) {
            w.__pulsar_GetDomSignature = function () {
                const rs = document.readyState;
                const rsCode = rs === 'complete' ? 2 : (rs === 'interactive' ? 1 : 0);
                // Pack into a 53-bit safe integer: (stamp << 2) | rsCode
                return (w.__pulsar_DomStamp * 4) + rsCode;
            }
        }
        return 1;
    } catch (e) {
        return -1;
    }
})()
