package ai.platon.pulsar.common.browser

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.CapabilityTypes
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Browsers {

    val CHROME_BINARY_SEARCH_PATHS = arrayOf(
        "/usr/bin/google-chrome-stable",
        "/usr/bin/google-chrome",
        "/opt/google/chrome/chrome",
        "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
        "C:/Program Files/Google/Chrome/Application/chrome.exe",
        // Windows 7, see https://github.com/platonai/pulsar/issues/9
        AppContext.USER_HOME + "/AppData/Local/Google/Chrome/Application/chrome.exe",
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
        "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
        "/Applications/Chromium.app/Contents/MacOS/Chromium",
        "/usr/bin/chromium",
        "/usr/bin/chromium-browser"
    )

    /**
     * Returns the chrome binary path.
     *
     * @return Chrome binary path.
     */
    fun searchChromeBinary(): Path {
        val path = System.getProperty(CapabilityTypes.BROWSER_CHROME_PATH)
        if (path != null) {
            return Paths.get(path).takeIf { Files.isExecutable(it) }?.toAbsolutePath()
                ?: throw RuntimeException("CHROME_PATH is not executable | $path")
        }

        return CHROME_BINARY_SEARCH_PATHS.map { Paths.get(it) }
            .firstOrNull { Files.isExecutable(it) }
            ?.toAbsolutePath()
            ?: throw RuntimeException("Could not find chrome binary in search path. Try setting CHROME_PATH environment value")
    }

    fun searchChromeBinaryOrNull() = kotlin.runCatching { searchChromeBinary() }.getOrNull()
}
