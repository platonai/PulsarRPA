package ai.platon.pulsar.common.code

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach

class SimpleKtParserCorrectedTest {

    private lateinit var parser: SimpleKtParser

    @BeforeEach
    fun setUp() {
        parser = SimpleKtParser()
    }

    @Nested
    @DisplayName("Comment Extraction Verification")
    inner class CommentExtractionVerification {

        @Test
        @DisplayName("Should assign interface comment to comment field, not signature")
        fun shouldAssignInterfaceCommentToCommentField() {
            val content = """
                /**
                 * This is an interface comment
                 * that describes the interface purpose
                 */
                interface CommentedInterface {
                    fun doSomething(): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("CommentedInterface", result[0].name)
            assertEquals("fun doSomething(): String", result[0].signature)
            assertEquals("This is an interface comment that describes the interface purpose", result[0].comment)
        }

        @Test
        @DisplayName("Should handle interface without comment")
        fun shouldHandleInterfaceWithoutComment() {
            val content = """
                interface SimpleInterface {
                    fun doSomething(): String
                    suspend fun doAsync(): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("SimpleInterface", result[0].name)
            val expectedSignature = "fun doSomething(): String\nsuspend fun doAsync(): String"
            assertEquals(expectedSignature, result[0].signature)
            assertNull(result[0].comment)
        }

        @Test
        @DisplayName("Should handle single line KDoc comment")
        fun shouldHandleSingleLineKDocComment() {
            val content = """
                /** Single line interface comment */
                interface SingleLineCommentInterface {
                    suspend fun suspendFunctionWithoutComment(): Unit
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("SingleLineCommentInterface", result[0].name)
            assertEquals("suspend fun suspendFunctionWithoutComment(): Unit", result[0].signature)
            assertEquals("Single line interface comment", result[0].comment)
        }

        @Test
        @DisplayName("Should not include function comments in signature")
        fun shouldNotIncludeFunctionCommentsInSignature() {
            val content = """
                interface TestInterface {
                    /**
                     * This function comment should be ignored
                     * by our simple parser
                     */
                    fun regularFunction(): String
                    
                    /** Another function comment */
                    suspend fun suspendFunction(): Unit
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("TestInterface", result[0].name)
            val expectedSignature = "fun regularFunction(): String\nsuspend fun suspendFunction(): Unit"
            assertEquals(expectedSignature, result[0].signature)
            assertNull(result[0].comment) // No interface-level comment

            // Verify that function comments are NOT in the signature
            assertFalse(result[0].signature.contains("This function comment should be ignored"))
            assertFalse(result[0].signature.contains("Another function comment"))
        }

        @Test
        @DisplayName("Should handle multiple interfaces with different comment scenarios")
        fun shouldHandleMultipleInterfacesWithDifferentCommentScenarios() {
            val content = """
                /**
                 * First interface with comment
                 */
                interface FirstInterface {
                    fun first(): String
                }
                
                interface SecondInterface {
                    suspend fun second(): Int
                }
                
                /** Third interface single line comment */
                interface ThirdInterface {
                    suspend fun third(): Boolean
                    fun another(): Unit
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(3, result.size)

            // First interface
            assertEquals("FirstInterface", result[0].name)
            assertEquals("fun first(): String", result[0].signature)
            assertEquals("First interface with comment", result[0].comment)

            // Second interface (no comment)
            assertEquals("SecondInterface", result[1].name)
            assertEquals("suspend fun second(): Int", result[1].signature)
            assertNull(result[1].comment)

            // Third interface
            assertEquals("ThirdInterface", result[2].name)
            assertEquals("suspend fun third(): Boolean\nfun another(): Unit", result[2].signature)
            assertEquals("Third interface single line comment", result[2].comment)
        }

        @Test
        @DisplayName("Should handle complex interface with susp-nd functions and comment")
        fun shouldHandleComplexInterfaceWithSuspendFunctionsAndComment() {
            val content = """
                /**
                 * Repository interface for async operations
                 * Provides CRUD operations for user data
                 */
                interface UserRepository {
                    suspend fun getUserById(id: String): User?
                    suspend fun saveUser(user: User): Boolean
                    fun validateUser(user: User): Boolean
                    suspend fun deleteUser(id: String): Boolean
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("UserRepository", result[0].name)

            val expectedSignature = """suspend fun getUserById(id: String): User?
suspend fun saveUser(user: User): Boolean
fun validateUser(user: User): Boolean
suspend fun deleteUser(id: String): Boolean""".trimIndent()

            assertEquals(expectedSignature, result[0].signature)
            assertEquals("Repository interface for async operations Provides CRUD operations for user data", result[0].comment)
        }

        @Test
        @DisplayName("Should handle single-line interface with comment")
        fun shouldHandleSingleLineInterfaceWithComment() {
            val content = """
                /** Quick interface comment */
                interface QuickInterface { suspend fun quick(): String }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("QuickInterface", result[0].name)
            assertEquals("suspend fun quick(): String", result[0].signature)
            assertEquals("Quick interface comment", result[0].comment)
        }
    }

    @Nested
    @DisplayName("Suspend Function Verification")
    inner class SuspendFunctionVerification {

        @Test
        @DisplayName("Should properly parse suspe functions without embedding comments")
        fun shouldProperlyParseSuspendFunctionsWithoutEmbeddingComments() {
            val content = """
                interface AsyncService {
                    suspend fun suspendFunctionWithoutComment(): Unit
                    suspend fun fetchData(url: String): String
                    fun regularFunction(): Int
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("AsyncService", result[0].name)

            val expectedSignature = """suspend fun suspendFunctionWithoutComment(): Unit
suspend fun fetchData(url: String): String
fun regularFunction(): Int""".trimIndent()

            assertEquals(expectedSignature, result[0].signature)
            assertNull(result[0].comment)
        }

        @Test
        @DisplayName("Should verify signature contains only function declarations")
        fun shouldVerifySignatureContainsOnlyFunctionDeclarations() {
            val content = """
                /**
                 * Service interface for data operations
                 */
                interface DataService {
                    /**
                     * Fetches user data - this comment should be ignored
                     */
                    suspend fun fetchUser(id: String): User
                    
                    /** Regular function comment - also ignored */
                    fun processUser(user: User): String
                }
            """.trimIndent()

            val result = parser.parseKotlinFile(content)

            assertEquals(1, result.size)
            assertEquals("DataService", result[0].name)
            assertEquals("Service interface for data operations", result[0].comment)

            val signature = result[0].signature
            assertTrue(signature.contains("suspend fun fetchUser(id: String): User"))
            assertTrue(signature.contains("fun processUser(user: User): String"))

            // Verify function comments are NOT in signature
            assertFalse(signature.contains("Fetches user data"))
            assertFalse(signature.contains("Regular function comment"))
            assertFalse(signature.contains("/**"))
            assertFalse(signature.contains("*/"))
        }
    }
}