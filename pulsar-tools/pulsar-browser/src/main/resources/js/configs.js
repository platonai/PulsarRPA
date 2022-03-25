// The following variables should be initialized programmatically
META_INFORMATION_ID = META_INFORMATION_ID || "META_INFORMATION_ID";
SCRIPT_SECTION_ID = SCRIPT_SECTION_ID || "SCRIPT_SECTION_ID";
PULSAR_CONFIGS = PULSAR_CONFIGS || {};
ATTR_HIDDEN = ATTR_HIDDEN||'_h';
ATTR_OVERFLOW_HIDDEN = ATTR_OVERFLOW_HIDDEN||'_oh';

const DATA_VERSION = "0.2.3";

// the vision schema keeps consistent with DOMRect
const VISION_SCHEMA_STRING = "l-t-w-h";
const CODE_STRUCTURE_SCHEMA_STRING = "d-s";

const fineHeight = 4000;
const fineNumAnchor = 100;
const fineNumImage = 20;

class MultiStatus {
    /**
     * n: check count
     * scroll: scroll count
     * idl: idle count
     * st: document state
     * r: complete reason
     * ec: error code
     * */
    status =   { n: 0, scroll: 0, idl: 0, st: "", r: "", ec: "" };
    initStat = null;
    lastStat = { w: 0, h: 0, na: 0, ni: 0, nst: 0, nnm: 0};
    lastD =    { w: 0, h: 0, na: 0, ni: 0, nst: 0, nnm: 0};
    initD =    { w: 0, h: 0, na: 0, ni: 0, nst: 0, nnm: 0}
}

class ActiveUrls {
    URL = document.URL;
    baseURI = document.baseURI;
    location = "";
    documentURI = document.documentURI
}

class ActiveDomMessage {
    multiStatus = new MultiStatus();
    urls = new ActiveUrls()
}
