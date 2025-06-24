package ai.platon.pulsar.common.code

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach

class SimpleKtParserTest {

    private lateinit var parser: SimpleKtParser

    @BeforeEach
    fun setUp() {
        parser = SimpleKtParser()
    }

    @Nested
    @DisplayName("Basic Interface Parsing")
    inner class BasicInterfaceParsing {

        @Test
        @DisplayName("Should parse empty interface")
        fun shouldParseEmptyInterface() {
            val content = """
                interface EmptyInterface {
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("EmptyInterface", result[0].name)
            assertEquals("", result[0].signature)
            assertNull(result[0].comment)
        }

        @Test
        @DisplayName("Should parse interface with single function")
        fun shouldParseInterfaceWithSingleFunction() {
            val content = """
                interface MyInterface {
                    fun doSomething(): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("MyInterface", result[0].name)
            assertEquals("fun doSomething(): String", result[0].signature)
            assertNull(result[0].comment)
        }

        @Test
        @DisplayName("Should parse interface with multiple functions")
        fun shouldParseInterfaceWithMultipleFunctions() {
            val content = """
                interface Calculator {
                    fun add(a: Int, b: Int): Int
                    fun subtract(a: Int, b: Int): Int
                    fun multiply(a: Int, b: Int): Int
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("Calculator", result[0].name)
            assertTrue(result[0].signature.contains("fun add(a: Int, b: Int): Int"))
            assertTrue(result[0].signature.contains("fun subtract(a: Int, b: Int): Int"))
            assertTrue(result[0].signature.contains("fun multiply(a: Int, b: Int): Int"))
        }
    }

    @Nested
    @DisplayName("Comment Parsing")
    inner class CommentParsing {

        @Test
        @DisplayName("Should parse single line KDoc comment")
        fun shouldParseSingleLineKDocComment() {
            val content = """
                interface MyInterface {
                    /** This is a simple function */
                    fun doSomething(): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertTrue(result[0].signature.contains("This is a simple function"))
        }

        @Test
        @DisplayName("Should parse multi-line KDoc comment")
        fun shouldParseMultiLineKDocComment() {
            val content = """
                interface MyInterface {
                    /**
                     * This is a multi-line comment
                     * that describes the function
                     * in detail
                     */
                    fun complexFunction(param: String): Int
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            val signature = result[0].signature
            assertTrue(signature.contains("This is a multi-line comment"))
            assertTrue(signature.contains("that describes the function"))
            assertTrue(signature.contains("in detail"))
        }

        @Test
        @DisplayName("Should handle function without comment")
        fun shouldHandleFunctionWithoutComment() {
            val content = """
                interface MyInterface {
                    fun simpleFunction(): Unit
                    
                    /** This has a comment */
                    fun commentedFunction(): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            val signature = result[0].signature
            assertTrue(signature.contains("fun simpleFunction(): Unit"))
            assertTrue(signature.contains("This has a comment"))
            assertTrue(signature.contains("fun commentedFunction(): String"))
        }
    }

    @Nested
    @DisplayName("Multiple Interfaces")
    inner class MultipleInterfaces {

        @Test
        @DisplayName("Should parse multiple interfaces in same file")
        fun shouldParseMultipleInterfaces() {
            val content = """
                interface FirstInterface {
                    fun first(): String
                }
                
                interface SecondInterface {
                    fun second(): Int
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(2, result.size)
            assertEquals("FirstInterface", result[0].name)
            assertEquals("SecondInterface", result[1].name)
            assertTrue(result[0].signature.contains("fun first(): String"))
            assertTrue(result[1].signature.contains("fun second(): Int"))
        }

        @Test
        @DisplayName("Should handle interfaces with inheritance")
        fun shouldHandleInterfacesWithInheritance() {
            val content = """
                interface BaseInterface {
                    fun base(): String
                }
                
                interface ExtendedInterface : BaseInterface {
                    fun extended(): Int
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(2, result.size)
            assertEquals("BaseInterface", result[0].name)
            assertEquals("ExtendedInterface", result[1].name)
        }
    }

    @Nested
    @DisplayName("Complex Function Signatures")
    inner class ComplexFunctionSignatures {

        @Test
        @DisplayName("Should parse function with generic parameters")
        fun shouldParseFunctionWithGenericParameters() {
            val content = """
                interface GenericInterface {
                    fun <T> process(item: T): List<T>
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertTrue(result[0].signature.contains("fun <T> process(item: T): List<T>"))
        }

        @Test
        @DisplayName("Should parse function with nullable parameters")
        fun shouldParseFunctionWithNullableParameters() {
            val content = """
                interface NullableInterface {
                    fun processNullable(input: String?): String?
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertTrue(result[0].signature.contains("fun processNullable(input: String?): String?"))
        }

        @Test
        @DisplayName("Should parse function with default parameters")
        fun shouldParseFunctionWithDefaultParameters() {
            val content = """
                interface DefaultInterface {
                    fun processWithDefaults(input: String, count: Int = 5): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertTrue(result[0].signature.contains("fun processWithDefaults(input: String, count: Int = 5): String"))
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle empty file")
        fun shouldHandleEmptyFile() {
            val content = ""

            val result = parser.parseKotlinFile(content)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should handle file with only comments")
        fun shouldHandleFileWithOnlyComments() {
            val content = """
                // This is a comment
                /* This is a block comment */
                /**
                 * This is a KDoc comment
                 */
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should handle interface with extra whitespace")
        fun shouldHandleInterfaceWithExtraWhitespace() {
            val content = """
                
                interface    SpacedInterface   {
                    
                    fun    spacedFunction(  param: String  ) :   String
                    
                }
                
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("SpacedInterface", result[0].name)
            assertTrue(result[0].signature.contains("fun    spacedFunction(  param: String  ) :   String"))
        }

        @Test
        @DisplayName("Should handle nested braces in function parameters")
        fun shouldHandleNestedBracesInFunctionParameters() {
            val content = """
                interface ComplexInterface {
                    fun complexFunction(callback: (String) -> Unit): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertTrue(result[0].signature.contains("fun complexFunction(callback: (String) -> Unit): String"))
        }

        @Test
        @DisplayName("Should handle interface declaration on single line")
        fun shouldHandleInterfaceDeclarationOnSingleLine() {
            val content = """
                interface SingleLineInterface { fun doSomething(): String }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("SingleLineInterface", result[0].name)
            assertTrue(result[0].signature.contains("fun doSomething(): String"))
        }
    }

    @Nested
    @DisplayName("Real World Example")
    inner class RealWorldExample {

        @Test
        @DisplayName("Should parse complete interface example from documentation")
        fun shouldParseCompleteInterfaceExample() {
            val content = """
                package ai.platon.pulsar.common.code
                
                import kotlin.collections.List
                
                interface MyInterface {
                    /**
                     * This is a simple function that does something.
                     * It takes a string parameter and returns an integer.
                     */
                    fun myFunction(param: String): Int
                    
                    /** Another function without parameters */
                    fun anotherFunction(): String
                    
                    fun functionWithoutComment(): Unit
                }
                
                interface AnotherInterface {
                    fun process(): Boolean
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(2, result.size)

            // First interface
            assertEquals("MyInterface", result[0].name)
            val firstSignature = result[0].signature
            assertTrue(firstSignature.contains("This is a simple function that does something"))
            assertTrue(firstSignature.contains("fun myFunction(param: String): Int"))
            assertTrue(firstSignature.contains("Another function without parameters"))
            assertTrue(firstSignature.contains("fun anotherFunction(): String"))
            assertTrue(firstSignature.contains("fun functionWithoutComment(): Unit"))

            // Second interface
            assertEquals("AnotherInterface", result[1].name)
            assertTrue(result[1].signature.contains("fun process(): Boolean"))
        }
    }
}