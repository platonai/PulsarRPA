package ai.platon.pulsar.common;

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

    private static final Object commandFileLocker = new Object();
    private static final Path commandFile = PulsarPaths.PATH_LOCAL_COMMAND;
    private static long commandFileLastModified = 0;
    private static final Duration commandFileMinCheckInterval = Duration.ofSeconds(5);

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
        boolean exist = false;

        if (Files.exists(commandFile)) {
            try {
                synchronized (commandFileLocker) {
                    long modified = commandFile.toFile().lastModified();
                    if (modified - commandFileLastModified > commandFileMinCheckInterval.toMillis()) {
                        commandFileLastModified = modified;

                        List<String> lines = Files.readAllLines(commandFile);
                        exist = lines.stream().anyMatch(line -> line.equals(command));
                        if (exist) {
                            lines.remove(command);
                            Files.write(commandFile, lines);
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
