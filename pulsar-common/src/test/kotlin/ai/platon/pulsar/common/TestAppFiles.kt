package ai.platon.pulsar.common

import ai.platon.pulsar.common.AppPaths.get
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.text.SimpleDateFormat

/**
 * Created by vincent on 16-7-20.
 */
class TestAppFiles {
    private val readableTime = SimpleDateFormat("MMdd.HHmmss").format(System.currentTimeMillis())
    private val commandFile = get("command", "test.$readableTime.sh")
    @Test
    @Throws(IOException::class)
    fun testCreateFiles() {
        val permissions = PosixFilePermissions.fromString("rwx------")
        Files.createDirectories(commandFile.parent)
        Files.createFile(commandFile, PosixFilePermissions.asFileAttribute(permissions))
        Files.write(commandFile, "#bin\necho hello world".toByteArray())
        Assert.assertTrue(Files.exists(commandFile))
        Files.deleteIfExists(commandFile)
    }

    @Test
    @Throws(IOException::class)
    fun testCreateFiles2() {
        Files.createDirectories(commandFile.parent)
        Files.write(commandFile, "#bin\necho hello world".toByteArray(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        Files.setPosixFilePermissions(commandFile, PosixFilePermissions.fromString("rwxrw-r--"))
        Assert.assertTrue(Files.exists(commandFile))
        Files.deleteIfExists(commandFile)
    }
}
