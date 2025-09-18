/**
 * Administrative and utility functions for X-SQL query management in Pulsar QL.
 *
 * This object provides administrative functions for managing SQL sessions, debugging,
 * and system operations within X-SQL queries. It includes functions for session control,
 * message output, and basic utility operations.
 *
 * ## Function Categories
 *
 * ### Session Management
 * - [sessionCount] - Get the number of active SQL sessions
 * - [closeSession] - Close the current SQL session
 *
 * ### Debug and Output
 * - [echo] - Return input messages (useful for debugging)
 * - [print] - Print messages to console output
 *
 * ### Data Persistence
 * - [save] - Save web pages to local cache with custom naming
 *
 * ## Usage Examples
 *
 * ```sql
 * -- Get session information
 * SELECT ADMIN.sessionCount();
 *
 * -- Debug with echo
 * SELECT ADMIN.echo('Hello World'); -- returns 'Hello World'
 * SELECT ADMIN.echo('Value:', 'Test'); -- returns 'Value:, Test'
 *
 * -- Print to console
 * CALL ADMIN.print('Processing started...');
 *
 * -- Save a web page
 * SELECT ADMIN.save('https://example.com', '.html');
 *
 * -- Close session
 * SELECT ADMIN.closeSession();
 * ```
 *
 * ## X-SQL Integration
 *
 * All admin functions are automatically registered as H2 database functions under the
 * "ADMIN" namespace. They can be used directly in X-SQL queries for system management
 * and debugging purposes.
 *
 * ## Performance Notes
 *
 * - Session operations are lightweight and fast
 * - Print operations use system console output
 * - Save operations respect existing cache policies
 * - All functions are deterministic where applicable
 *
 * ## Thread Safety
 *
 * All functions in this object are thread-safe and can be safely used
 * in concurrent query execution contexts.
 *
 * @author Pulsar AI
 * @since 1.0.0
 * @see UDFGroup
 * @see UDFunction
 * @see H2SessionFactory
 * @see SQLContexts
 */
package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.common.annotation.H2Context
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.slf4j.LoggerFactory
import java.sql.Connection

@UDFGroup(namespace = "ADMIN")
object AdminFunctions {
    val log = LoggerFactory.getLogger(AdminFunctions::class.java)
    private val sqlContext get() = SQLContexts.create()

    /**
     * Returns the input message unchanged (useful for debugging and testing).
     *
     * This function simply returns the input string as-is. It's primarily used for
     * debugging queries, testing function calls, and verifying data flow in X-SQL queries.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Basic echo
     * SELECT ADMIN.echo('Hello World'); -- returns 'Hello World'
     *
     * -- Debug variable values
     * SELECT ADMIN.echo(variable_value) FROM my_table;
     *
     * -- Test function composition
     * SELECT ADMIN.echo(re1('text', 'pattern'));
     * ```
     *
     * ## Use Cases
     * - Debugging query execution
     * - Testing function calls
     * - Verifying data values
     * - Query development and troubleshooting
     *
     * ## Performance Notes
     * - Extremely lightweight operation
     * - No side effects
     * - Useful for query optimization testing
     *
     * @param conn The H2 database connection context
     * @param message The string message to echo back
     * @return The input message unchanged
     * @see echo with two parameters
     */
    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context conn: Connection, message: String): String {
        return message
    }

    /**
     * Returns the concatenation of two input messages (useful for debugging and testing).
     *
     * This function concatenates two input strings with a comma separator. It's used for
     * debugging queries that need to combine multiple values or test multi-parameter functions.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Concatenate two values
     * SELECT ADMIN.echo('Hello', 'World'); -- returns 'Hello, World'
     *
     * -- Debug multiple variables
     * SELECT ADMIN.echo(name, value) FROM config_table;
     *
     * -- Test with function results
     * SELECT ADMIN.echo(re1(text1, 'pattern1'), re1(text2, 'pattern2'));
     * ```
     *
     * ## Use Cases
     * - Debugging multiple values
     * - Testing multi-parameter functions
     * - Combining debug output
     * - Query development
     *
     * ## Performance Notes
     * - Lightweight string concatenation
     * - No side effects
     * - Deterministic operation
     *
     * @param conn The H2 database connection context
     * @param message The first string message
     * @param message2 The second string message
     * @return The concatenated string "message, message2"
     * @see echo with single parameter
     */
    @UDFunction(deterministic = true) @JvmStatic
    fun echo(@H2Context conn: Connection, message: String, message2: String): String {
        return "$message, $message2"
    }

    /**
     * Prints a message to the console output.
     *
     * This function outputs the input message to the system console (standard output).
     * It's useful for debugging, monitoring query execution, and logging important events.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Simple print
     * CALL ADMIN.print('Query execution started');
     *
     * -- Print with query results
     * CALL ADMIN.print(CONCAT('Processing ', COUNT(*), ' records'))
     * FROM my_table;
     *
     * -- Debug output
     * CALL ADMIN.print(CONCAT('Extracted value: ', re1(text, 'pattern')));
     * ```
     *
     * ## Use Cases
     * - Query execution monitoring
     * - Debug output during development
     * - Progress indication for long queries
     * - Simple logging and auditing
     *
     * ## Output Location
     * - Messages are printed to system console (System.out)
     * - Visible in application logs if console output is captured
     * - Useful for development and debugging
     *
     * @param message The message to print to console
     * @see println
     */
    @UDFunction
    @JvmStatic
    fun print(message: String) {
        println(message)
    }

    /**
     * Returns the number of active SQL sessions in the current context.
     *
     * This function provides insight into the current session usage and can be used
     * for monitoring system load and resource utilization.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Get session count
     * SELECT ADMIN.sessionCount(); -- returns integer count
     *
     * -- Monitor in complex queries
     * SELECT
     *   ADMIN.sessionCount() as active_sessions,
     *   COUNT(*) as processed_records
     * FROM my_table;
     * ```
     *
     * ## Use Cases
     * - System monitoring and diagnostics
     * - Resource usage tracking
     * - Performance analysis
     * - Session management verification
     *
     * ## Performance Notes
     * - Fast operation with minimal overhead
     * - Real-time session count
     * - No side effects
     *
     * @param conn The H2 database connection context
     * @return The number of active SQL sessions
     * @see SQLContexts
     */
    @UDFunction
    @JvmStatic
    fun sessionCount(@H2Context conn: Connection): Int {
        return sqlContext.sessionCount()
    }

    /**
     * Closes the current SQL session and returns session information.
     *
     * This function terminates the current H2 SQL session, cleans up associated
     * resources, and returns a string representation of the closed session.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Close current session
     * SELECT ADMIN.closeSession(); -- returns session info string
     *
     * -- Use in session management
     * CALL ADMIN.print('Closing session...');
     * SELECT ADMIN.closeSession();
     * ```
     *
     * ## Use Cases
     * - Explicit session cleanup
     * - Resource management
     * - Session rotation
     * - System maintenance
     *
     * ## Effects
     * - Terminates the current H2 session
     * - Cleans up session resources
     * - Returns session information for confirmation
     *
     * @param conn The H2 database connection context
     * @return String representation of the closed session
     * @see H2SessionFactory
     */
    @UDFunction
    @JvmStatic
    fun closeSession(@H2Context conn: Connection): String {
        val h2session = H2SessionFactory.getH2Session(conn)
        H2SessionFactory.closeSession(h2session.serialId)
        return h2session.toString()
    }

    /**
     * Loads and saves a web page to the local cache with a custom file extension.
     *
     * This function loads a web page (respecting cache policies) and saves it to the
     * local web cache directory with the specified file extension. Useful for preserving
     * page content with specific naming conventions.
     *
     * ## X-SQL Usage
     * ```sql
     * -- Save with default .htm extension
     * SELECT ADMIN.save('https://example.com'); -- returns saved file path
     *
     * -- Save with custom extension
     * SELECT ADMIN.save('https://example.com', '.html'); -- returns saved file path
     *
     * -- Save with .json extension for API responses
     * SELECT ADMIN.save('https://api.example.com/data', '.json');
     * ```
     *
     * ## Use Cases
     * - Preserving web page snapshots
     * - Creating local copies with specific extensions
     * - Archiving scraped content
     * - Debug and development data capture
     *
     * ## File Organization
     * - Files are saved to the configured web cache directory
     * - File names are derived from URL structure
     * - Custom extensions allow proper file type identification
     * - Respects existing cache policies
     *
     * ## Performance Notes
     * - Uses existing session cache if available
     * - Respects cache expiration policies
     * - Creates directories as needed
     * - Returns absolute file path
     *
     * @param conn The H2 database connection context
     * @param url The URL of the web page to save
     * @param postfix The file extension to use (default: ".htm")
     * @return String representation of the saved file path
     * @see AppPaths
     * @see AppFiles
     */
    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun save(@H2Context conn: Connection, url: String, postfix: String = ".htm"): String {
        val page = H2SessionFactory.getSession(conn).load(url)
        val path = AppPaths.WEB_CACHE_DIR.resolve(AppPaths.fromUri(url, "", postfix))
        return AppFiles.saveTo(page, path).toString()
    }
}
