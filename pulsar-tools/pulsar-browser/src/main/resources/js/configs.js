Object.defineProperty(navigator, 'webdriver', { get: () => false, });

// The following variables should be initialized programmatically
META_INFORMATION_ID = META_INFORMATION_ID || "META_INFORMATION_ID";
SCRIPT_SECTION_ID = SCRIPT_SECTION_ID || "SCRIPT_SECTION_ID";
PULSAR_CONFIGS = PULSAR_CONFIGS || {};
ATTR_HIDDEN = ATTR_HIDDEN||'_h';
ATTR_OVERFLOW_HIDDEN = ATTR_OVERFLOW_HIDDEN||'_oh';
ATTR_VISIBLE = ATTR_HIDDEN||'_visible';

const DATA_VERSION = "0.2.3";
