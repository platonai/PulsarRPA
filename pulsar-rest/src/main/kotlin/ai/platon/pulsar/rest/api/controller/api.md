# API Documentation

Looking at your `AiController` class again, I see several opportunities for improvement to make your API more consistent, intuitive, and RESTful:

### Main Issues

1. **Inconsistent Path Naming**: Your endpoints mix verb-based (`/chat`, `/extract`) and noun-based (`/command`) paths
2. **Overlapping Functionality**: Multiple endpoints for chat with different signatures
3. **Inconsistent Async Pattern**: Only some operations have async counterparts
4. **Mixed Content Type Handling**: Same endpoints accepting different content types
5. **Unclear Resource Hierarchy**: Relationship between resources not clearly expressed in URLs

### Recommendations

1. **Standardize on Resource-Based Paths**:
    - Use nouns for resources (`/conversations`, `/extractions`, `/commands`)
    - Use HTTP methods to indicate actions (GET, POST, PUT, DELETE)

2. **Consistent Async Pattern**:
    - Implement same pattern across all async operations
    - Use `/resource/{uuid}/status` and `/resource/{uuid}/stream` consistently

3. **Separate Content Type Variants**:
    - Use different endpoint paths for different input/output formats
    - Example: `/commands/text` for text-based commands

4. **Revised API Structure**:

```
/ai
  /conversations
    POST - Start a new conversation (returns UUID)
    /{uuid}
      GET - Get conversation result
      /status - Get conversation status
      /stream - Stream conversation updates
  
  /extractions
    POST - Extract data from a webpage (returns UUID)
    /{uuid}
      GET - Get extraction result
      /status - Get extraction status
      /stream - Stream extraction updates
  
  /commands
    POST - Execute structured command (returns UUID or direct result)
    /text
      POST - Execute text command (returns UUID or markdown)
    /{uuid}
      GET - Get command result
      /status - Get command status
      /stream - Stream command updates
```

5. **Rename Methods to Match RESTful Pattern**:
    - Use method names that reflect the resource and action
    - Make async behavior explicit in method names
    - For example, rename `simpleChatGet` to `getConversation` or `createConversation`

This structure makes the API more predictable, follows RESTful principles better, and provides a clearer mental model for consumers. Each resource type follows the same pattern of creation, status checking, and streaming updates.
