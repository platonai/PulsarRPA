Convert the following URI description into a Kotlin-compatible regex pattern that matches exactly one valid URI.

### 🎯 Objective:
Generate a precise regex pattern that captures URIs based on the provided description.

### 🔧 Requirements:
* **Single Match**: Pattern must match exactly one complete URI (not partial matches or multiple URIs in text)
* **Kotlin Compatibility**: Must work with Kotlin's `Regex` class and standard regex engine
* **Exact Matching**: Pattern should precisely match the URI format described in the input
* **Complete URI**: Match the entire URI from start to end (use anchors if needed)

### 📋 Output Format:
* Start with the exact prefix: `Regex: `
* Follow with the regex pattern only
* No explanations, comments, or additional text
* Pattern should be ready to use in `Regex("your_pattern")`

### ⚠️ Important Notes:
* Escape special regex characters appropriately for Kotlin
* Consider URI components: scheme, authority, path, query, fragment
* Ensure the pattern is neither too restrictive nor too permissive
* Test mentally against common URI variations if applicable

### 📥 Input Description:

```text
{PLACEHOLDER_URI_DESCRIPTION}
```
