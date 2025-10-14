package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.EnhancedDOMTreeNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HashUtilsTests {

    @BeforeEach
    fun setup() {
        // Clear any cached values between tests
        HashUtils::class.java.getDeclaredField("elementHashCache").apply {
            isAccessible = true
            (get(null) as java.util.concurrent.ConcurrentHashMap<*, *>).clear()
        }
        HashUtils::class.java.getDeclaredField("parentBranchHashCache").apply {
            isAccessible = true
            (get(null) as java.util.concurrent.ConcurrentHashMap<*, *>).clear()
        }
    }

    @Test
    fun `elementHash with default config includes all components`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            attributes = mapOf("id" to "submit-btn", "class" to "btn primary"),
            backendNodeId = 12345,
            sessionId = "session-123"
        )

        // Act
        val hash1 = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)
        val hash2 = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        assertNotNull(hash1)
        assertEquals(hash1, hash2) // Should be deterministic
        assertEquals(64, hash1.length) // SHA256 hex string length
    }

    @Test
    fun `elementHash with legacy config excludes backend and session IDs`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            attributes = mapOf("id" to "submit-btn", "class" to "btn primary"),
            backendNodeId = 12345,
            sessionId = "session-123"
        )

        // Act
        val legacyHash = HashUtils.elementHash(node, null, HashUtils.LEGACY_CONFIG)
        val defaultHash = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        assertNotEquals(legacyHash, defaultHash) // Should be different when backend/session exist
        assertEquals(64, legacyHash.length)
    }

    @Test
    fun `elementHash with backend node config uses only backend and session`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            attributes = mapOf("id" to "submit-btn", "class" to "btn primary"),
            backendNodeId = 12345,
            sessionId = "session-123"
        )

        // Act
        val backendHash = HashUtils.elementHash(node, null, HashUtils.BACKEND_NODE_CONFIG)
        val defaultHash = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert basic difference
        assertNotEquals(backendHash, defaultHash)
        assertEquals(64, backendHash.length)

        // Changing static attributes should NOT change the backend-node-config hash
        val nodeWithDifferentAttrs = node.copy(attributes = mapOf("id" to "x", "class" to "y z"))
        val backendHashAttrChanged = HashUtils.elementHash(nodeWithDifferentAttrs, null, HashUtils.BACKEND_NODE_CONFIG)
        assertEquals(backendHash, backendHashAttrChanged)

        // Changing backendNodeId SHOULD change the hash
        val nodeWithDifferentBackend = node.copy(backendNodeId = 54321)
        val backendHashBackendChanged = HashUtils.elementHash(nodeWithDifferentBackend, null, HashUtils.BACKEND_NODE_CONFIG)
        assertNotEquals(backendHash, backendHashBackendChanged)

        // Changing sessionId SHOULD change the hash (since config uses session)
        val nodeWithDifferentSession = node.copy(sessionId = "session-456")
        val backendHashSessionChanged = HashUtils.elementHash(nodeWithDifferentSession, null, HashUtils.BACKEND_NODE_CONFIG)
        assertNotEquals(backendHash, backendHashSessionChanged)
    }

    @Test
    fun `elementHash includes parent branch hash when provided`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            attributes = mapOf("id" to "submit-btn")
        )
        val parentBranchHash = "parent-hash-123"

        // Act
        val hashWithParent = HashUtils.elementHash(node, parentBranchHash, HashUtils.DEFAULT_CONFIG)
        val hashWithoutParent = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        assertNotEquals(hashWithParent, hashWithoutParent)
    }

    @Test
    fun `elementHash uses static attributes correctly`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "INPUT",
            attributes = mapOf(
                "id" to "username",
                "class" to "form-input",
                "type" to "text",
                "data-custom" to "ignored", // Should be ignored (not in STATIC_ATTRIBUTES)
            )
        )

        // Act
        val hash = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        // The hash should be deterministic based on the static attributes
        val hash2 = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)
        assertEquals(hash, hash2)
    }

    @Test
    fun `elementHash fallback strategy works for nodes without meaningful attributes`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "DIV",
            attributes = mapOf("data-random" to "value"), // Not in STATIC_ATTRIBUTES
            backendNodeId = 999
        )

        val config = HashUtils.HashConfig(
            useBackendNodeId = false,
            useStaticAttributes = true,
            fallbackToSimpleHash = true
        )

        // Act
        val hash = HashUtils.elementHash(node, null, config)

        // Assert
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun `elementHash caching works correctly`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            attributes = mapOf("id" to "test-btn")
        )

        // Act
        val hash1 = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)
        val hash2 = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG) // Should use cache

        // Assert
        assertEquals(hash1, hash2)
        assertSame(hash1, hash2) // Should be the same object due to caching
    }

    @Test
    fun `parentBranchHash creates consistent hash for ancestor path`() {
        // Arrange
        val ancestors = listOf(
            EnhancedDOMTreeNode(nodeId = 1, nodeName = "HTML"),
            EnhancedDOMTreeNode(nodeId = 2, nodeName = "BODY", attributes = mapOf("class" to "main-body")),
            EnhancedDOMTreeNode(nodeId = 3, nodeName = "DIV", attributes = mapOf("id" to "container", "class" to "wrapper extra")),
            EnhancedDOMTreeNode(nodeId = 4, nodeName = "BUTTON", attributes = mapOf("class" to "btn primary"))
        )

        // Act
        val hash1 = HashUtils.parentBranchHash(ancestors)
        val hash2 = HashUtils.parentBranchHash(ancestors)

        // Assert
        assertEquals(hash1, hash2) // Should be deterministic
        assertEquals(64, hash1.length) // SHA256 hex string length
    }

    @Test
    fun `parentBranchHash handles special elements correctly`() {
        // Arrange
        val shadowHost = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "DIV",
            shadowRoots = listOf(EnhancedDOMTreeNode(nodeId = 10, nodeName = "DIV"))
        )
        val iframe = EnhancedDOMTreeNode(nodeId = 2, nodeName = "IFRAME", attributes = mapOf("src" to "test.html"))
        val slot = EnhancedDOMTreeNode(nodeId = 3, nodeName = "SLOT", attributes = mapOf("name" to "content"))
        val ancestors = listOf(shadowHost, iframe, slot)

        // Act
        val hash = HashUtils.parentBranchHash(ancestors)

        // Assert
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun `parentBranchHash caching works correctly`() {
        // Arrange
        val ancestors = listOf(
            EnhancedDOMTreeNode(nodeId = 1, nodeName = "HTML"),
            EnhancedDOMTreeNode(nodeId = 2, nodeName = "BODY")
        )

        // Act
        val hash1 = HashUtils.parentBranchHash(ancestors)
        val hash2 = HashUtils.parentBranchHash(ancestors) // Should use cache

        // Assert
        assertEquals(hash1, hash2)
        assertSame(hash1, hash2) // Should be the same object due to caching
    }

    @Test
    fun `simpleElementHash uses default configuration`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "INPUT",
            attributes = mapOf("type" to "text", "placeholder" to "Enter text")
        )

        // Act
        val simpleHash = HashUtils.simpleElementHash(node)
        val defaultHash = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        assertEquals(defaultHash, simpleHash)
    }

    @Test
    fun `elementHash with custom sessionId override`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            sessionId = "original-session"
        )
        val customSessionId = "custom-session-123"

        // Act
        val hashWithCustomSession = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG, customSessionId)
        val hashWithOriginalSession = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        assertNotEquals(hashWithCustomSession, hashWithOriginalSession)
    }

    @Test
    fun `elementHash handles null and empty values gracefully`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "DIV",
            attributes = emptyMap(),
            backendNodeId = null,
            sessionId = null
        )

        // Act
        val hash = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)

        // Assert
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun `all hash configurations produce different hashes for same node`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "BUTTON",
            attributes = mapOf("id" to "test-btn", "class" to "btn"),
            backendNodeId = 12345,
            sessionId = "test-session"
        )

        // Act
        val legacyHash = HashUtils.elementHash(node, null, HashUtils.LEGACY_CONFIG)
        val defaultHash = HashUtils.elementHash(node, null, HashUtils.DEFAULT_CONFIG)
        val backendHash = HashUtils.elementHash(node, null, HashUtils.BACKEND_NODE_CONFIG)

        // Assert
        assertNotEquals(legacyHash, defaultHash)
        assertNotEquals(defaultHash, backendHash)
        assertNotEquals(legacyHash, backendHash)
    }

    @Test
    fun `hash configurations are deterministic`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "INPUT",
            attributes = mapOf("type" to "email", "required" to "true"),
            backendNodeId = 555,
            sessionId = "session-xyz"
        )

        // Act & Assert
        for (config in listOf(HashUtils.LEGACY_CONFIG, HashUtils.DEFAULT_CONFIG, HashUtils.BACKEND_NODE_CONFIG)) {
            val hash1 = HashUtils.elementHash(node, null, config)
            val hash2 = HashUtils.elementHash(node, null, config)
            val hash3 = HashUtils.elementHash(node, null, config)

            assertEquals(hash1, hash2)
            assertEquals(hash2, hash3)
        }
    }

    @Test
    fun `fallback identifier includes helpful attributes`() {
        // Arrange
        val node = EnhancedDOMTreeNode(
            nodeId = 1,
            nodeName = "INPUT",
            attributes = mapOf(
                "class" to "form-control",
                "role" to "textbox",
                "type" to "email",
                "data-custom" to "ignored", // Should not be included
                "backendNodeId" to "999" // This should be added separately
            ),
            backendNodeId = 999
        )

        val config = HashUtils.HashConfig(
            useBackendNodeId = false,
            useStaticAttributes = false,
            fallbackToSimpleHash = true
        )

        // Act
        val hash = HashUtils.elementHash(node, null, config)

        // Assert
        assertNotNull(hash)
        // The hash should contain fallback information
        assertTrue(hash.isNotEmpty())
    }
}
