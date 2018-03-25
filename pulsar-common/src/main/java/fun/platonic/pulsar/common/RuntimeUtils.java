package fun.platonic.pulsar.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RuntimeUtils {

    protected static final Logger LOG = LoggerFactory.getLogger(RuntimeUtils.class);

    public static boolean checkIfJavaProcessRunning(String imageName) {
        try {
            Process proc = Runtime.getRuntime().exec("jps");
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
                if (line.contains(imageName)) {
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
    public static boolean hasLocalFileCommand(String commandFile, String command) {
        boolean exist = false;

        Path path = Paths.get(commandFile);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                exist = lines.stream().anyMatch(line -> line.equals(command));
                lines.remove(command);
                Files.write(path, lines);
            } catch (IOException e) {
                LOG.error(e.toString());
            }
        }

        return exist;
    }
}
