Convert the following URI description into a Kotlin-compatible regex pattern that matches valid URIs.

### Objective
Generate a practical, flexible regex pattern for matching URIs based on the provided description, prioritizing real-world usability over strict precision.

### 🔧 Requirements:
* **Single Match**: Pattern must match exactly one complete URI (not partial matches or multiple URIs in text)
* **Flexible Matching**: Pattern should match complete URIs that reasonably fit the description
* **Kotlin Compatibility**: Must work with Kotlin's `Regex` class and standard regex engine
* **Practical Focus**: Balance between accuracy and real-world URI variations
* **Complete URI**: Match entire URIs, but allow for common variations and edge cases

### 📋 Output Format:
* Start with the exact prefix: `Regex: `
* Follow with the regex pattern only
* No explanations, comments, or additional text
* Pattern should be ready to use in `Regex("your_pattern")`

### ⚠️ Important Notes:
* Escape special regex characters appropriately for Kotlin
* Consider URI components: scheme, authority, path, query, fragment
* Allow for reasonable flexibility in path structures and parameters
* Consider internationalized domain names and modern URI patterns
* Prefer capturing valid URIs over rejecting edge cases
* Prefer start with, contains, ends with
* Prefer numeric-alphabetical matching over word boundaries

### 💡 Flexibility Guidelines:
* Allow optional components where practical (e.g., www prefix, trailing slashes)
* Support common protocol variations (http, https, ftp, etc.)
* Handle both IP addresses and domain names
* Account for port numbers and common path patterns
* Support query parameters and fragments flexibly

### 📥 Input Description:

```text
{PLACEHOLDER_URI_DESCRIPTION}
```
