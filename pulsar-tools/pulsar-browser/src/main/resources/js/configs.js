// The following variables should be initialized programmatically
META_INFORMATION_ID = META_INFORMATION_ID || "META_INFORMATION_ID";
SCRIPT_SECTION_ID = SCRIPT_SECTION_ID || "SCRIPT_SECTION_ID";
PULSAR_CONFIGS = PULSAR_CONFIGS || {};
ATTR_HIDDEN = ATTR_HIDDEN||'_h';
ATTR_OVERFLOW_HIDDEN = ATTR_OVERFLOW_HIDDEN||'_oh';
// TODO: reserved. visible flag overrides hidden flag
ATTR_VISIBLE = ATTR_HIDDEN||'_visible';

const DATA_VERSION = "0.2.3";

// the vision schema keeps consistent with DOMRect
const VISION_SCHEMA = ["left", "top", "width", "height"];
const VISION_SCHEMA_STRING = "l-t-w-h";
const CODE_STRUCTURE_SCHEMA_STRING = "d-s";

const ATTR_COMPUTED_STYLE = 'st';
const ATTR_ELEMENT_NODE_VI = 'vi';
const ATTR_TEXT_NODE_VI = 'tv';

const ATTR_DEBUG = '_debug';
