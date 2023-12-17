package ai.platon.pulsar.common

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

object FileCommand {
    private val log = getLogger(FileCommand::class.java)

    val COMMAND_FILE: Path = AppPaths.PATH_LOCAL_COMMAND
    val CHECK_INTERVAL: Duration = Duration.ofSeconds(15)
    val LAST_CHECK_TIME: MutableMap<String, Long> = HashMap()

    /**
     * Check local command file to see if there are pending commands.
     * Supported local file commands can be found in AppConstants CMD_*
     * General command options are supported:
     * -perm: the command should be always keep in the file and execute every time the command file is checked,
     * otherwise the command is executed only
     *
     * @param command Check if the [command] exists
     * @param checkInterval The check interval in seconds
     * @return true if the command is exists during this check period
     */
    fun check(command: String, checkInterval: Long): Boolean {
        return check(command, Duration.ofSeconds(checkInterval))
    }

    /**
     * Check local command file to see if there are pending commands
     * Supported local file commands can be found in AppConstants CMD_*
     * General command options are supported:
     * -perm: the command should be always keep in the file and execute every time the command file is checked,
     * otherwise the command is executed only
     *
     * @param command Check if the [command] exists
     * @param checkInterval The check interval
     * @return true if the command is exists during this check period
     */
    @JvmOverloads
    fun check(command: String, checkInterval: Duration = CHECK_INTERVAL, action: () -> Unit = {}): Boolean {
        return try {
            doCheckFile(command, checkInterval, action)
        } catch (e: IOException) {
            log.error(e.toString())
            false
        }
    }
    
    @Synchronized
    private fun doCheckFile(command: String, checkInterval: Duration = CHECK_INTERVAL, action: () -> Unit = {}): Boolean {
        val lastCheckTime = LAST_CHECK_TIME.getOrDefault(command, 0L)
        if (DateTimes.elapsedTime(lastCheckTime) < checkInterval) {
            // do not check
            return false
        }
        
        if (!Files.exists(COMMAND_FILE)) {
            // do not check
            return false
        }
        
        val now = System.currentTimeMillis()
        val modifiedTime = COMMAND_FILE.toFile().lastModified()
        var exist = false
        if (lastCheckTime <= modifiedTime) {
            LAST_CHECK_TIME[command] = now
            val lines = Files.readAllLines(COMMAND_FILE)
            exist = lines.any { it.contains(command) }
            if (exist) {
                if (!command.contains( "-perm", ignoreCase = true)) {
                    lines.remove(command)
                    Files.write(COMMAND_FILE, lines)
                }
                action()
            }
        }
        
        return exist
    }
}
