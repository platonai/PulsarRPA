package ai.platon.pulsar.common.code

class SimpleKtParser {
    /**
     * Parses a Kotlin file and extracts the interface declarations.
     *
     * This function scans the provided Kotlin file content for interface declarations,
     * extracts their names, signatures, and comments, and returns a list of `KtInterfaceDesc` objects.
     *
     * Only handle simple cases of Kotlin interfaces, such as:
     *
     * ```kotlin
     * interface MyInterface {
     *    /**
     *     * This is a simple function that does something.
     *     */
     *    fun myFunction(param: String): Int
     * }
     * ```
     *
     * @param fileContent The content of the Kotlin file as a string.
     * @return A list of `KtInterfaceDesc` objects representing the interfaces found in the file.
     */
    fun parseKotlinFile(fileContent: String): List<KtInterfaceDesc> {
        val interfaceDescs = mutableListOf<KtInterfaceDesc>()
        val lines = fileContent.lines()

        var currentInterfaceName: String? = null
        var currentInterfaceComment: String? = null
        var currentFunctions = mutableListOf<String>()
        var pendingComment: StringBuilder? = null
        var insideInterface = false
        var insideComment = false
        var braceCount = 0

        for (i in lines.indices) {
            val line = lines[i]
            val trimmedLine = line.trim()

            when {
                // Start of multi-line comment
                trimmedLine.startsWith("/**") -> {
                    insideComment = true
                    pendingComment = StringBuilder()
                    val commentContent = trimmedLine.substringAfter("/**").substringBefore("*/")
                    if (trimmedLine.contains("*/")) {
                        // Single line comment
                        pendingComment?.append(commentContent.trim())
                        insideComment = false
                    } else {
                        pendingComment?.append(commentContent.trim())
                    }
                }

                // End of multi-line comment
                insideComment && trimmedLine.contains("*/") -> {
                    val commentContent = trimmedLine.substringBefore("*/").removePrefix("*").trim()
                    if (commentContent.isNotEmpty()) {
                        if (pendingComment?.isNotEmpty() == true) {
                            pendingComment?.append(" ")
                        }
                        pendingComment?.append(commentContent)
                    }
                    insideComment = false
                }

                // Continuation of multi-line comment
                insideComment -> {
                    val commentContent = trimmedLine.removePrefix("*").trim()
                    if (commentContent.isNotEmpty()) {
                        if (pendingComment?.isNotEmpty() == true) {
                            pendingComment?.append(" ")
                        }
                        pendingComment?.append(commentContent)
                    }
                }

                // Interface declaration
                trimmedLine.startsWith("interface ") -> {
                    currentInterfaceName = extractInterfaceName(trimmedLine)
                    // Assign pending comment to interface (if any)
                    currentInterfaceComment = pendingComment?.toString()
                    pendingComment = null
                    insideInterface = true
                    currentFunctions.clear()

                    // Handle single-line interface declaration
                    if (trimmedLine.contains("{") && trimmedLine.contains("}")) {
                        // Single line interface like: interface Test { fun test(): String }
                        val functionPart = trimmedLine.substringAfter("{").substringBeforeLast("}")
                        if (isFunctionDeclaration(functionPart.trim())) {
                            currentFunctions.add(functionPart.trim())
                        }

                        // Complete the interface immediately
                        val signature = if (currentFunctions.isNotEmpty()) {
                            currentFunctions.joinToString("\n")
                        } else {
                            ""
                        }
                        interfaceDescs.add(KtInterfaceDesc(currentInterfaceName!!, signature, currentInterfaceComment))

                        // Reset state
                        currentInterfaceName = null
                        currentInterfaceComment = null
                        currentFunctions.clear()
                        insideInterface = false
                        braceCount = 0
                    } else {
                        // Multi-line interface, initialize brace count
                        braceCount = trimmedLine.count { it == '{' }
                    }
                }

                // Function declaration inside interface (including suspend functions)
                insideInterface && isFunctionDeclaration(trimmedLine) && currentInterfaceName != null -> {
                    // Just add the function signature, not the comment
                    currentFunctions.add(trimmedLine)
                    // Clear pending comment as it was for this function (we don't store per-function comments in this simple parser)
                    pendingComment = null
                }

                // Count braces to track interface scope (multi-line interfaces)
                insideInterface && currentInterfaceName != null -> {
                    braceCount += trimmedLine.count { it == '{' }
                    braceCount -= trimmedLine.count { it == '}' }

                    // End of interface
                    if (braceCount <= 0) {
                        val signature = if (currentFunctions.isNotEmpty()) {
                            currentFunctions.joinToString("\n")
                        } else {
                            ""
                        }
                        interfaceDescs.add(KtInterfaceDesc(currentInterfaceName!!, signature, currentInterfaceComment))

                        // Reset state
                        currentInterfaceName = null
                        currentInterfaceComment = null
                        currentFunctions.clear()
                        insideInterface = false
                        braceCount = 0
                    }
                }
            }
        }

        return interfaceDescs
    }

    private fun extractInterfaceName(line: String): String {
        return line.substringAfter("interface ")
            .substringBefore("{")
            .substringBefore(":")  // Handle inheritance
            .trim()
    }

    /**
     * Checks if a line contains a function declaration (including suspend functions).
     * Supports:
     * - Regular functions: fun myFunction()
     * - Suspend functions: suspend fun myFunction()
     * - Generic functions: fun <T> myFunction()
     * - Suspend generic functions: suspend fun <T> myFunction()
     */
    private fun isFunctionDeclaration(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("fun ") ||
                trimmed.startsWith("suspend fun ") ||
                trimmed.contains(Regex("^suspend\\s+fun\\s+")) ||
                trimmed.contains(Regex("^fun\\s+<.*>\\s+")) ||
                trimmed.contains(Regex("^suspend\\s+fun\\s+<.*>\\s+"))
    }
}