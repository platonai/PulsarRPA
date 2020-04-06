package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.util.*

object FileCommand {
    private val log = LoggerFactory.getLogger(FileCommand::class.java)
    private val COMMAND_FILE = AppPaths.PATH_LOCAL_COMMAND
    private val CHECK_INTERVAL = Duration.ofSeconds(15)
    private val LAST_CHECK_TIME: MutableMap<String, Long> = HashMap()

    /**
     * Check local command file to see if there are pending commands
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
        if (!Files.exists(COMMAND_FILE)) {
            return false
        }

        var exist = false
        try {
            synchronized(FileCommand::class.java) {
                val modifiedTime = COMMAND_FILE.toFile().lastModified()
                val lastCheckTime = LAST_CHECK_TIME.getOrDefault(command, 0L)
                val now = System.currentTimeMillis()
                if (lastCheckTime <= modifiedTime && now - lastCheckTime >= checkInterval.toMillis()) {
                    LAST_CHECK_TIME[command] = now
                    val lines = Files.readAllLines(COMMAND_FILE)
                    exist = lines.stream().anyMatch { line: String -> line.contains(command) }
                    if (exist) {
                        if (!StringUtils.containsIgnoreCase(command, " -perm")) {
                            lines.remove(command)
                            Files.write(COMMAND_FILE, lines)
                        }
                        action()
                    }
                }
            }
        } catch (e: IOException) {
            log.error(e.toString())
        }
        return exist
    }
}
