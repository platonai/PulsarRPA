package ai.platon.pulsar.skeleton.crawl.fetch.driver

/**
 * Dispatches web driver commands in text format.
 *
 * This class is responsible for parsing and executing WebDriver commands provided as strings.
 * It converts these string-based commands into executable actions.
 *
 * @author Vincent Zhang
 */
class SimpleCommandDispatcher {

    /**
     * Dispatches a WebDriver command in text format.
     *
     * This method takes a string that describes how to invoke a function and returns the actual
     * executable action. For example, the command "driver.open('https://t.tt/')" will be parsed
     * and executed accordingly.
     *
     * @param command The command in text format.
     * @param driver The WebDriver instance to execute the command on.
     */
    suspend fun dispatch(command: String, driver: WebDriver): Any? {
        // Use regex to extract the function name and argument list from the command string
        val (objectName, functionName, args) = parseSimpleFunctionCall(command) ?: return null

        // Execute the command based on the extracted function name
        return when (functionName) {
            "open" -> {
                // Execute the open command with the provided arguments
                driver.open(args[0])
            }
            "navigateTo" -> {
                // Execute the navigateTo command with the provided arguments
                if (args.size == 1) {
                    driver.navigateTo(args[0])
                } else if (args.size == 2) {
                    driver.navigateTo(NavigateEntry(args[0], pageUrl = args[1]))
                } else {
                    null
                }
            }
            "click" -> {
                // Execute the click command with the provided arguments
                when (args.size) {
                    1 -> driver.click(args[0])
                    2 -> driver.click(args[0], args[1].toInt())
                    else -> null
                }
            }
            "type" -> {
                // Execute the type command with the provided arguments
                driver.type(args[0], args[1])
            }
            "waitForNavigation" -> {
                // Execute the wait command with the provided arguments
                if (args.isEmpty()) {
                    driver.waitForNavigation()
                } else if (args.size == 1) {
                    driver.waitForNavigation(args[0])
                } else if (args.size == 2) {
                    driver.waitForNavigation(args[0], args[1].toLong())
                } else {
                    null
                }
            }
            "captureScreenshot" -> {
                // Execute the screenshot command with the provided arguments
                if (args.isEmpty()) {
                    driver.captureScreenshot()
                } else if (args.size == 1) {
                    driver.captureScreenshot(args[0])
                } else {
                    null
                }
            }
            "scrollDown" -> {
                // Execute the scrollDown command with the provided arguments
                if (args.isEmpty()) {
                    driver.scrollDown()
                } else if (args.size == 1) {
                    driver.scrollDown(args[0].toInt())
                } else {
                    null
                }
            }
            "scrollUp" -> {
                // Execute the scrollUp command with the provided arguments
                if (args.isEmpty()) {
                    driver.scrollUp()
                } else if (args.size == 1) {
                    driver.scrollUp(args[0].toInt())
                } else {
                    null
                }
            }
            "scrollToTop" -> {
                // Execute the scrollToTop command with the provided arguments
                driver.scrollToTop()
            }
            "scrollToBottom" -> {
                // Execute the scrollToBottom command with the provided arguments
                driver.scrollToBottom()
            }
            "scrollToMiddle" -> {
                // Execute the scrollToMiddle command with the provided arguments
                if (args.size == 1) {
                    driver.scrollToMiddle(args[0].toDouble())
                } else {
                    null
                }
            }
            "mouseWheelDown" -> {
                // Execute the mouseWheelDown command with the provided arguments
                if (args.isEmpty()) {
                    driver.mouseWheelDown()
                } else if (args.size == 1) {
                    driver.mouseWheelDown(args[0].toInt())
                } else if (args.size == 2) {
                    driver.mouseWheelDown(args[0].toInt(), args[1].toDouble())
                } else if (args.size == 3) {
                    driver.mouseWheelDown(args[0].toInt(), args[1].toDouble(), args[2].toDouble())
                } else if (args.size == 4) {
                    driver.mouseWheelDown(args[0].toInt(), args[1].toDouble(), args[2].toDouble(), args[3].toLong())
                } else {
                    null
                }
            }
            "mouseWheelUp" -> {
                // Execute the mouseWheelUp command with the provided arguments
                if (args.isEmpty()) {
                    driver.mouseWheelUp()
                } else if (args.size == 1) {
                    driver.mouseWheelUp(args[0].toInt())
                } else if (args.size == 2) {
                    driver.mouseWheelUp(args[0].toInt(), args[1].toDouble())
                } else if (args.size == 3) {
                    driver.mouseWheelUp(args[0].toInt(), args[1].toDouble(), args[2].toDouble())
                } else if (args.size == 4) {
                    driver.mouseWheelUp(args[0].toInt(), args[1].toDouble(), args[2].toDouble(), args[3].toLong())
                } else {
                    null
                }
            }
            "moveMouseTo" -> {
                // Execute the moveMouseTo command with the provided arguments
                if (args.size == 2) {
                    driver.moveMouseTo(args[0].toDouble(), args[1].toDouble())
                } else if (args.size == 3) {
                    driver.moveMouseTo(args[0], args[1].toInt(), args[2].toInt())
                } else {
                    null
                }
            }
            "dragAndDrop" -> {
                // Execute the dragAndDrop command with the provided arguments
                if (args.size == 3) {
                    driver.dragAndDrop(args[0], args[1].toInt(), args[2].toInt())
                } else {
                    null
                }
            }
            "outerHTML" -> {
                // Execute the outerHTML command with the provided arguments
                if (args.isEmpty()) {
                    driver.outerHTML()
                } else if (args.size == 1) {
                    driver.outerHTML(args[0])
                } else {
                    null
                }
            }
            "selectFirstTextOrNull" -> {
                // Execute the selectFirstTextOrNull command with the provided arguments
                driver.selectFirstTextOrNull(args[0])
            }
            "selectTextAll" -> {
                // Execute the selectTextAll command with the provided arguments
                driver.selectTextAll(args[0])
            }
            "selectFirstAttributeOrNull" -> {
                // Execute the selectFirstAttributeOrNull command with the provided arguments
                if (args.size == 2) {
                    driver.selectFirstAttributeOrNull(args[0], args[1])
                } else {
                    null
                }
            }
            "selectAttributes" -> {
                // Execute the selectAttributes command with the provided arguments
                driver.selectAttributes(args[0])
            }
            "selectAttributeAll" -> {
                // Execute the selectAttributeAll command with the provided arguments
                if (args.size == 2) {
                    driver.selectAttributeAll(args[0], args[1])
                } else if (args.size == 4) {
                    driver.selectAttributeAll(args[0], args[1], args[2].toInt(), args[3].toInt())
                } else {
                    null
                }
            }
            "setAttribute" -> {
                // Execute the setAttribute command with the provided arguments
                if (args.size == 3) {
                    driver.setAttribute(args[0], args[1], args[2])
                } else {
                    null
                }
            }
            "setAttributeAll" -> {
                // Execute the setAttributeAll command with the provided arguments
                if (args.size == 3) {
                    driver.setAttributeAll(args[0], args[1], args[2])
                } else {
                    null
                }
            }
            "selectHyperlinks" -> {
                // Execute the selectHyperlinks command with the provided arguments
                if (args.size == 1) {
                    driver.selectHyperlinks(args[0])
                } else if (args.size == 3) {
                    driver.selectHyperlinks(args[0], args[1].toInt(), args[2].toInt())
                } else {
                    null
                }
            }
            "selectAnchors" -> {
                // Execute the selectAnchors command with the provided arguments
                if (args.size == 1) {
                    driver.selectAnchors(args[0])
                } else if (args.size == 3) {
                    driver.selectAnchors(args[0], args[1].toInt(), args[2].toInt())
                } else {
                    null
                }
            }
            "selectImages" -> {
                // Execute the selectImages command with the provided arguments
                if (args.size == 1) {
                    driver.selectImages(args[0])
                } else if (args.size == 3) {
                    driver.selectImages(args[0], args[1].toInt(), args[2].toInt())
                } else {
                    null
                }
            }
            "evaluate" -> {
                // Execute the evaluate command with the provided arguments
                if (args.size == 1) {
                    driver.evaluate(args[0])
                } else if (args.size == 2) {
                    driver.evaluate(args[0], args[1])
                } else {
                    null
                }
            }
            "evaluateDetail" -> {
                // Execute the evaluateDetail command with the provided arguments
                driver.evaluateDetail(args[0])
            }
            "clickablePoint" -> {
                // Execute the clickablePoint command with the provided arguments
                driver.clickablePoint(args[0])
            }
            "boundingBox" -> {
                // Execute the boundingBox command with the provided arguments
                driver.boundingBox(args[0])
            }
            "newJsoupSession" -> {
                // Execute the newJsoupSession command with the provided arguments
                driver.newJsoupSession()
            }
            "loadJsoupResource" -> {
                // Execute the loadJsoupResource command with the provided arguments
                driver.loadJsoupResource(args[0])
            }
            "loadResource" -> {
                // Execute the loadResource command with the provided arguments
                driver.loadResource(args[0])
            }
            "pause" -> {
                // Execute the pause command with the provided arguments
                driver.pause()
            }
            "stop" -> {
                // Execute the stop command with the provided arguments
                driver.stop()
            }
            else -> {
                // Handle unsupported commands or add more command cases here
                null
            }
        }
    }

    companion object {
        /**
         * Parses a kotlin function call from the input string.
         *
         * This helper function extracts the function name and its arguments from a command string.
         * Arguments are trimmed and split by commas, and each argument is parsed into a string.
         * The resulting list of strings represents the arguments, regardless of their actual types.
         *
         * For example:
         *
         * ```kotlin
         * driver.open("https://t.tt")
         * driver.navigateTo("https://t.tt")
         * driver.selectTextAll("div")
         * driver.scrollToMiddle(0.4)
         * driver.mouseWheelUp(2, 200, 200)
         * driver.mouseWheelUp(2, 200, 200, 100)
         * ```
         *
         * For simplification, the following malform cases are also supported:
         *
         * ```kotlin
         * driver.open(https://t.tt)
         * driver.open('https://t.tt')
         * driver.open(https://t.tt, )
         * driver.mouseWheelUp(2, 200, 200, )
         * ```
         *
         * @see <a href="https://github.com/Kotlin/grammar-tools">Kotlin Grammar Tools</a>
         *
         * @param input The command string to parse.
         * @return A pair of function name and argument list, or null if the input is not a valid function call.
         */
        fun parseSimpleFunctionCall(input: String): Triple<String, String, List<String>>? {
            val regex = """(\w+)\.(\w+)\((.*?)\)""".toRegex()
            val match = regex.find(input) ?: return null

            val (objectName, functionName, argsString) = match.destructured
            if (argsString.isBlank()) {
                return Triple(objectName, functionName, emptyList())
            }

            val args = argsString.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }
                .map { it.removeSurrounding("\'") }
                .map { it.removeSurrounding("\"") }

            return Triple(objectName, functionName, args)
        }
    }
}
