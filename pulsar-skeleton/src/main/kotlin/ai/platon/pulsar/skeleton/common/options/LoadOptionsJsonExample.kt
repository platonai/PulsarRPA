package ai.platon.pulsar.skeleton.common.options

import ai.platon.pulsar.common.config.VolatileConfig
import java.time.Duration

/**
 * Examples demonstrating usage of the LoadOptionsJson class
 * 
 * This class contains examples showing how to convert LoadOptions objects
 * to JSON format and back to LoadOptions again.
 */
object LoadOptionsJsonExample {

    /**
     * Main function demonstrating the LoadOptionsJson functionality
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // Example 1: Convert standard LoadOptions to JSON
        basicExample()

        // Example 2: Convert custom LoadOptions to JSON
        customOptionsExample()
        
        // Example 3: Load LoadOptions from JSON
        loadFromJsonExample()
    }

    /**
     * Example showing serialization of default LoadOptions
     */
    private fun basicExample() {
        println("==== Basic LoadOptions JSON Example ====")
        
        // Create default LoadOptions
        val options = LoadOptions.createUnsafe()
        
        // Convert to JSON
        val json = LoadOptionsJson.toJson(options)
        
        // Print the JSON
        println(json)
        println("========================================\n")
    }

    /**
     * Example showing serialization of customized LoadOptions
     */
    private fun customOptionsExample() {
        println("==== Custom LoadOptions JSON Example ====")
        
        // Create custom LoadOptions with command-line style parameters
        val options = LoadOptions.parse("-entity product -label electronics -expires 10m -scrollCount 5 -refresh -parse")
        
        // Convert to JSON
        val json = LoadOptionsJson.toJson(options)
        
        // Print the JSON
        println(json)
        println("=========================================\n")
    }

    /**
     * Example showing deserialization of JSON back to LoadOptions
     */
    private fun loadFromJsonExample() {
        println("==== Load From JSON Example ====")
        
        // Sample JSON representing LoadOptions
        val json = """
        {
          "entity": "product",
          "label": "electronics",
          "expires": "PT10M",
          "scrollCount": 5,
          "refresh": true,
          "parse": true,
          "itemOptions": {
            "expires": "PT1H",
            "scrollCount": 3
          }
        }
        """
        
        // Parse JSON into LoadOptions
        val options = LoadOptionsJson.fromJson(json)
        
        // Verify the parsed options
        println("Entity: ${options.entity}")
        println("Label: ${options.label}")
        println("Expires: ${options.expires}")
        println("Scroll Count: ${options.scrollCount}")
        println("Refresh: ${options.refresh}")
        println("Parse: ${options.parse}")
        println("Item Expires: ${options.itemExpires}")
        println("Item Scroll Count: ${options.itemScrollCount}")
        println("===================================\n")
        
        // Convert back to JSON to see the full representation
        println("Converted back to JSON:")
        println(LoadOptionsJson.toJson(options))
    }

    /**
     * Example showing programmatic creation of LoadOptions with various settings
     */
    fun programmaticOptionsExample() {
        // Create options with code rather than command-line parameters
        val options = LoadOptions.createUnsafe().apply {
            entity = "article"
            label = "news"
            expires = Duration.ofHours(1)
            scrollCount = 8
            parse = true
            storeContent = true
            outLinkSelector = "a.article-link"
            requireNotBlank = ".article-body"
            requireImages = 2
        }
        
        // Convert to JSON
        val json = LoadOptionsJson.toJson(options)
        println(json)
        
        // Create item-specific options
        val itemOptions = options.createItemOptions()
        itemOptions.apply {
            // Customize item options
            itemScrollCount = 3
            itemRequireImages = 5
        }
        
        // Convert item options to JSON
        val itemJson = LoadOptionsJson.toJson(itemOptions)
        println(itemJson)
    }
} 