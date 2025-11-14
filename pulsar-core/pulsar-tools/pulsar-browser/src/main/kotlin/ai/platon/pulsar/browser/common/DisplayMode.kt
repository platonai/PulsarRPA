package ai.platon.pulsar.browser.common

/**
 * The browser display mode.
 *
 * Three display modes are supported:
 * 1. GUI: open as a normal browser
 * 2. HEADLESS: open in headless mode
 * 3. SUPERVISED: supervised by other programs
 * */
enum class DisplayMode { SUPERVISED, GUI, HEADLESS }