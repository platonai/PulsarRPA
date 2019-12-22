package ai.platon.pulsar.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuntimeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeUtils.class);

    private static final Object COMMAND_FILE_LOCKER = new Object();
    private static final Path COMMAND_FILE = AppPaths.PATH_LOCAL_COMMAND;
    private static final Duration DEFAULT_COMMAND_FILE_CHECK_INTERVAL = Duration.ofSeconds(5);
    private static Map<String, Long> COMMAND_LAST_CHECK_TIME = new HashMap<>();

    static {
        if (!Files.exists(COMMAND_FILE)) {
            try {
                Files.createFile(COMMAND_FILE);
            } catch (IOException e) {
                LOG.error(StringUtil.stringifyException(e));
            }
        }
    }

    public static boolean checkIfProcessRunning(String regex) {
        try {
            Process proc = Runtime.getRuntime().exec("ps -ef");
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.matches(regex)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.error(e.toString());
        }

        return false;
    }

    /**
     * Check local command file to see if there are pending commands
     * Supported local file commands can be found in AppConstants CMD_*
     * General command options are supported:
     * -keep: the command should be always keep in the file and execute every time the command file is checked,
     *          otherwise the command is executed only
     */
    public static boolean hasLocalFileCommand(String command) {
        return hasLocalFileCommand(command, DEFAULT_COMMAND_FILE_CHECK_INTERVAL);
    }

    /**
     * Check local command file to see if there are pending commands
     * Supported local file commands can be found in AppConstants CMD_*
     * General command options are supported:
     * -keep: the command should be always keep in the file and execute every time the command file is checked,
     *          otherwise the command is executed only
     */
    public static boolean hasLocalFileCommand(String command, Duration checkInterval) {
        if (!Files.exists(COMMAND_FILE)) {
            return false;
        }

        boolean exist = false;

        try {
            synchronized (COMMAND_FILE_LOCKER) {
                long modifiedTime = COMMAND_FILE.toFile().lastModified();
                long lastCheckTime = COMMAND_LAST_CHECK_TIME.getOrDefault(command, 0L);
                long now = System.currentTimeMillis();

                if (lastCheckTime <= modifiedTime && now - lastCheckTime >= checkInterval.toMillis()) {
                    COMMAND_LAST_CHECK_TIME.put(command, now);

                    List<String> lines = Files.readAllLines(COMMAND_FILE);
                    exist = lines.stream().anyMatch(line -> line.contains(command));
                    if (exist) {
                        if (!StringUtils.containsIgnoreCase(command, " -perm")) {
                            lines.remove(command);
                            Files.write(COMMAND_FILE, lines);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        return exist;
    }
}
