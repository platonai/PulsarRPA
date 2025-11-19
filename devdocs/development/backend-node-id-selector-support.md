# Backend Node ID Support in Selectors

## Overview

PageHandler now supports using backend node IDs in addition to CSS selectors for all selector-based methods.

## Feature Description

Backend node IDs are stable identifiers that persist across DevTools sessions within the same page lifecycle. This makes them ideal for automation scenarios where you need to reference the same DOM element multiple times.

## Syntax

```kotlin
// Regular CSS selector
pageHandler.focusOnSelector("input#username")

// Backend node ID selector
pageHandler.focusOnSelector("backend:123")
```

## Format

- **Prefix**: `backend:`
- **ID**: Integer backend node ID
- **Example**: `backend:123`, `backend:456`

## Supported Methods

All methods that accept a `selector: String` parameter now support backend node ID format:

### Query Methods
- `querySelector(selector: String): Int?`
- `querySelectorAll(selector: String): List<Int>`

### Attribute Methods
- `getAttributes(selector: String): Map<String, String>`
- `getAttribute(selector: String, attrName: String): String?`
- `getAttributeAll(selector: String, attrName: String, start: Int, limit: Int): List<String>`
- `setAttribute(nodeId: Int, attrName: String, attrValue: String)` *(uses nodeId directly)*

### Interaction Methods
- `focusOnSelector(selector: String): Int`
- `scrollIntoViewIfNeeded(selector: String, rect: Rect? = null): Int?`

### State Check Methods
- `visible(selector: String): Boolean`

## How It Works

1. **Parse**: When a selector starts with `"backend:"`, it's recognized as a backend node ID
2. **Extract**: The numeric ID is extracted from the string
3. **Resolve**: Uses `DOM.resolveNode()` to convert backend node ID → runtime object
4. **Convert**: Uses `DOM.requestNode()` to convert runtime object → frontend node ID
5. **Execute**: Performs the requested operation using the resolved node ID
6. **Cleanup**: Releases the runtime object to prevent memory leaks

## Benefits

### 1. Stability Across Sessions
```kotlin
// Backend node IDs remain valid even if you disconnect and reconnect DevTools
val backendId = 123
driver.focusOnSelector("backend:$backendId")  // Works reliably
```

### 2. No DOM Traversal
```kotlin
// Direct node reference, no CSS query needed
// Faster for repeated operations on the same element
val nodeId = pageHandler.querySelector("backend:456")
```

### 3. Cross-Frame References
```kotlin
// Backend node IDs can reference nodes in different frames
// (assuming you have the correct backend ID)
```

## Obtaining Backend Node IDs

Backend node IDs can be obtained from various CDP operations:

```kotlin
// Method 1: From Node object
val node = domAPI.describeNode(nodeId, null, null, null, false)
val backendNodeId = node?.backendNodeId

// Method 2: From DOM events
pageHandler.onDOMNodeInserted { event ->
    val backendId = event.node.backendNodeId
    println("New node with backend ID: $backendId")
}

// Method 3: From snapshot
val snapshot = domAPI.captureSnapshot()
val backendIds = snapshot.nodes.map { it.backendNodeId }
```

## Usage Examples

### Example 1: Focus Using Backend Node ID
```kotlin
val backendNodeId = 123
val result = pageHandler.focusOnSelector("backend:$backendNodeId")
if (result > 0) {
    println("Successfully focused element with backend ID $backendNodeId")
}
```

### Example 2: Get Attributes
```kotlin
val backendNodeId = 456
val attributes = pageHandler.getAttributes("backend:$backendNodeId")
println("Element attributes: $attributes")
```

### Example 3: Check Visibility
```kotlin
val backendNodeId = 789
val isVisible = pageHandler.visible("backend:$backendNodeId")
println("Element visible: $isVisible")
```

### Example 4: Scroll Into View
```kotlin
val backendNodeId = 999
pageHandler.scrollIntoViewIfNeeded("backend:$backendNodeId")
```

### Example 5: Mixed Usage
```kotlin
// You can mix regular selectors and backend node IDs
val cssNodeId = pageHandler.querySelector("div.container")
val backendNodeId = pageHandler.querySelector("backend:123")

// Both return regular node IDs that can be used interchangeably
```

## Important Notes

### querySelectorAll Behavior
When using backend node ID with `querySelectorAll`, it returns a single-element list:

```kotlin
val nodeIds = pageHandler.querySelectorAll("backend:123")
// Returns: [resolvedNodeId] (single element)

// vs regular CSS selector:
val nodeIds = pageHandler.querySelectorAll("div.item")
// Returns: [nodeId1, nodeId2, nodeId3, ...] (multiple elements)
```

### Error Handling

```kotlin
// Invalid formats return null
pageHandler.querySelector("backend:")       // null (missing ID)
pageHandler.querySelector("backend:abc")    // null (non-numeric)
pageHandler.querySelector("backend:12.34")  // null (decimal)

// Non-existent backend IDs may throw or return null
try {
    pageHandler.querySelector("backend:99999")
} catch (e: ChromeDriverException) {
    println("Backend node not found")
}
```

### Performance Considerations

- Backend node ID resolution involves 2 RPC calls (resolveNode + requestNode)
- Regular CSS selectors involve 1 RPC call (querySelector)
- For one-time operations, CSS selectors are faster
- For repeated operations on the same element, storing the backend ID can be beneficial

## Migration Guide

### Before (CSS only)
```kotlin
// Had to query every time
driver.focusOnSelector("input#username")
driver.scrollIntoViewIfNeeded("input#username")
driver.getAttribute("input#username", "value")
```

### After (with backend node ID)
```kotlin
// Get backend ID once
val node = domAPI.describeNode(nodeId, null, null, null, false)
val backendId = node.backendNodeId

// Reuse for multiple operations
driver.focusOnSelector("backend:$backendId")
driver.scrollIntoViewIfNeeded("backend:$backendId")
driver.getAttribute("backend:$backendId", "value")
```

## Implementation Details

### Key Components

1. **`BACKEND_NODE_PREFIX`**: Constant `"backend:"` used to identify backend node ID selectors
2. **`parseSelector()`**: Determines if selector is CSS or backend node ID format
3. **`resolveBackendNodeId()`**: Converts backend node ID to regular node ID
4. **Memory Management**: Automatically releases runtime objects after use

### Code Flow
```
Selector "backend:123"
    ↓
parseSelector() detects prefix
    ↓
Extract ID: 123
    ↓
resolveBackendNodeId(123)
    ↓
DOM.resolveNode(backendNodeId=123) → RemoteObject
    ↓
DOM.requestNode(objectId) → nodeId
    ↓
Runtime.releaseObject(objectId)  // Cleanup
    ↓
Return nodeId
```

## Testing

Run the test suite:
```bash
./mvnw test -Dtest=PageHandlerBackendNodeIdTests
```

## Bug Fixes

This feature also includes a bug fix for `focusOnSelector()`:

### Before (Bug)
```kotlin
domAPI?.focus(nodeId, rootId, null)  // ❌ Wrong: rootId should not be passed
```

### After (Fixed)
```kotlin
domAPI?.focus(nodeId, null, null)  // ✅ Correct: only nodeId parameter
```

The `DOM.focus()` method accepts three optional parameters (nodeId, backendNodeId, objectId), but only one should be provided to identify the target element.

## References

- [Chrome DevTools Protocol - DOM Domain](https://chromedevtools.github.io/devtools-protocol/tot/DOM/)
- CDP `DOM.resolveNode` method
- CDP `DOM.requestNode` method
- CDP `DOM.focus` method

## Version

- **Feature Added**: 2025-10-20
- **Branch**: feat/webdriver-improve-2
