package fun.platonic.pulsar.common;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Set;

import static fun.platonic.pulsar.common.config.PulsarConstants.PULSAR_TMP_DIR;
import static org.junit.Assert.assertTrue;

/**
 * Created by vincent on 16-7-20.
 */
public class TestFilesystem {
    private String readableTime = new SimpleDateFormat("MMdd.HHmmss").format(System.currentTimeMillis());
    private Path commandFile = Paths.get(PULSAR_TMP_DIR.toString(), "command", "test." + readableTime + ".sh");

    @Test
    public void testCreateFiles() throws IOException {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");

        Files.createDirectories(commandFile.getParent());
        Files.createFile(commandFile, PosixFilePermissions.asFileAttribute(permissions));
        Files.write(commandFile, "#bin\necho hello world".getBytes());
        assertTrue(Files.exists(commandFile));
        Files.deleteIfExists(commandFile);
    }

    @Test
    public void testCreateFiles2() throws IOException {
        Files.createDirectories(commandFile.getParent());
        Files.write(commandFile, "#bin\necho hello world".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        Files.setPosixFilePermissions(commandFile, PosixFilePermissions.fromString("rwxrw-r--"));
        assertTrue(Files.exists(commandFile));
        Files.deleteIfExists(commandFile);
    }
}
