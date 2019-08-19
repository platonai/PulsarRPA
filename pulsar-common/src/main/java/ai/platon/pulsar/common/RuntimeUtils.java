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
import java.util.List;

public class RuntimeUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(RuntimeUtils.class);

    private static final Object COMMAND_FILE_LOCKER = new Object();
    private static final Path COMMAND_FILE = PulsarPaths.PATH_LOCAL_COMMAND;
    private static final Duration DEFAULT_COMMAND_FILE_CHECK_INTERVAL = Duration.ofSeconds(5);
    private static long commandFileLastModified = 0;

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
     * Check local command file
     */
    public static boolean hasLocalFileCommand(String command) {
        return hasLocalFileCommand(command, DEFAULT_COMMAND_FILE_CHECK_INTERVAL);
    }

    public static boolean hasLocalFileCommand(String command, Duration checkInterval) {
        boolean exist = false;

        if (Files.exists(COMMAND_FILE)) {
            try {
                synchronized (COMMAND_FILE_LOCKER) {
                    long modified = COMMAND_FILE.toFile().lastModified();
                    if (modified - commandFileLastModified >= checkInterval.toMillis()) {
                        commandFileLastModified = modified;

                        List<String> lines = Files.readAllLines(COMMAND_FILE);
                        exist = lines.stream().anyMatch(line -> line.startsWith(command));
                        if (exist) {
                            if (!StringUtils.containsIgnoreCase(command, " -keep")) {
                                lines.remove(command);
                                Files.write(COMMAND_FILE, lines);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error(e.toString());
            }
        }

        return exist;
    }
}
