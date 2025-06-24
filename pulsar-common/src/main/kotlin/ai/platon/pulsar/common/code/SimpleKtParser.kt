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
        var currentFunctions = mutableListOf<String>()
        var currentComment: StringBuilder? = null
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
                    currentComment = StringBuilder()
                    val commentContent = trimmedLine.substringAfter("/**").substringBefore("*/")
                    if (trimmedLine.contains("*/")) {
                        // Single line comment
                        currentComment?.append(commentContent.trim())
                        insideComment = false
                    } else {
                        currentComment?.append(commentContent.trim())
                    }
                }

                // End of multi-line comment
                insideComment && trimmedLine.contains("*/") -> {
                    val commentContent = trimmedLine.substringBefore("*/").removePrefix("*").trim()
                    if (commentContent.isNotEmpty()) {
                        if (currentComment?.isNotEmpty() == true) {
                            currentComment?.append(" ")
                        }
                        currentComment?.append(commentContent)
                    }
                    insideComment = false
                }

                // Continuation of multi-line comment
                insideComment -> {
                    val commentContent = trimmedLine.removePrefix("*").trim()
                    if (commentContent.isNotEmpty()) {
                        if (currentComment?.isNotEmpty() == true) {
                            currentComment?.append(" ")
                        }
                        currentComment?.append(commentContent)
                    }
                }

                // Interface declaration
                trimmedLine.startsWith("interface ") -> {
                    currentInterfaceName = extractInterfaceName(trimmedLine)
                    insideInterface = true
                    currentFunctions.clear()

                    // Handle single-line interface declaration
                    if (trimmedLine.contains("{") && trimmedLine.contains("}")) {
                        // Single line interface like: interface Test { fun test(): String }
                        val functionPart = trimmedLine.substringAfter("{").substringBeforeLast("}")
                        if (functionPart.trim().startsWith("fun ")) {
                            val functionSignature = if (currentComment?.isNotEmpty() == true) {
                                "/**\n * ${currentComment}\n */\n${functionPart.trim()}"
                            } else {
                                functionPart.trim()
                            }
                            currentFunctions.add(functionSignature)
                        }

                        // Complete the interface immediately
                        val signature = if (currentFunctions.isNotEmpty()) {
                            currentFunctions.joinToString("\n")
                        } else {
                            ""
                        }
                        interfaceDescs.add(KtInterfaceDesc(currentInterfaceName!!, signature, null))

                        // Reset state
                        currentInterfaceName = null
                        currentFunctions.clear()
                        insideInterface = false
                        currentComment = null
                        braceCount = 0
                    } else {
                        // Multi-line interface, initialize brace count
                        braceCount = trimmedLine.count { it == '{' }
                    }
                }

                // Function declaration inside interface (multi-line interfaces)
                insideInterface && trimmedLine.startsWith("fun ") && currentInterfaceName != null -> {
                    val functionSignature = if (currentComment?.isNotEmpty() == true) {
                        "/**\n * ${currentComment}\n */\n$trimmedLine"
                    } else {
                        trimmedLine
                    }
                    currentFunctions.add(functionSignature)
                    currentComment = null
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
                        interfaceDescs.add(KtInterfaceDesc(currentInterfaceName!!, signature, null))

                        // Reset state
                        currentInterfaceName = null
                        currentFunctions.clear()
                        insideInterface = false
                        currentComment = null
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
}