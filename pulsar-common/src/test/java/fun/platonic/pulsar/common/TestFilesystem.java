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

import static org.junit.Assert.assertTrue;

/**
 * Created by vincent on 16-7-20.
 */
public class TestFilesystem {

    @Test
    public void testFiles() throws IOException {
        String readableTime = new SimpleDateFormat("MMdd.HHmmss").format(System.currentTimeMillis());

        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");

        Path commandFile = Paths.get("/tmp/pulsar/command/test." + readableTime + ".sh");
        Files.createFile(commandFile, PosixFilePermissions.asFileAttribute(permissions));
        Files.write(commandFile, "#bin\necho hello world".getBytes());
        assertTrue(Files.exists(commandFile));
        Files.deleteIfExists(commandFile);
    }

    @Test
    public void testCreateFiles() throws IOException {
        String readableTime = new SimpleDateFormat("MMdd.HHmmss").format(System.currentTimeMillis());

        Path commandFile = Paths.get("/tmp/pulsar/command/test." + readableTime + ".sh");
        Files.createDirectories(commandFile.getParent());
        Files.write(commandFile, "#bin\necho hello world".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        Files.setPosixFilePermissions(commandFile, PosixFilePermissions.fromString("rwxrw-r--"));
        assertTrue(Files.exists(commandFile));
        Files.deleteIfExists(commandFile);
    }
}
