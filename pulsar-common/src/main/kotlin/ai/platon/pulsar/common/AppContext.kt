package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

object AppContext {

    private val logger = getLogger(AppContext::class)

    enum class State {
        NEW, RUNNING, TERMINATING, TERMINATED
    }

    /**
     * The number of processors available to the Java virtual machine
     */
    val NCPU get() = Runtime.getRuntime().availableProcessors()

    // User's current working directory
    val HOST_NAME get() = InetAddress.getLocalHost().hostName

    val USER get() = SystemUtils.USER_NAME

    /**
     * Directories
     */
    val TMP_DIR get() = SystemUtils.JAVA_IO_TMPDIR

    // User's current working directory
    val USER_DIR get() = SystemUtils.USER_DIR

    // User's home directory
    val USER_HOME get() = SystemUtils.USER_HOME

    // windows subsystem for linux
    val OS_IS_WSL by lazy { checkIsWSL() }

    // The identity of this running instance
    val APP_VERSION by lazy { sniffVersion() }
    val APP_NAME = System.getProperty("app.name", "pulsar")
    val APP_IDENT = System.getProperty("app.id.str", USER)
    val APP_TMP_PROPERTY = System.getProperty("app.tmp.dir")
    val APP_TMP_DIR = if (APP_TMP_PROPERTY != null) Paths.get(APP_TMP_PROPERTY) else Paths.get(TMP_DIR).resolve(APP_NAME)
    val PROC_TMP_DIR = if (APP_TMP_PROPERTY != null) {
        Paths.get(APP_TMP_PROPERTY)
    } else {
        Paths.get(TMP_DIR).resolve("$APP_NAME-$APP_IDENT")
    }
    // Special users such as tomcat do not have it's own home
    val APP_DATA_DIR = listOf(USER_HOME, TMP_DIR).map { Paths.get(it) }
        .first { Files.isWritable(it) }.resolve(".$APP_NAME")

    val state = AtomicReference(State.NEW)

    val isActive get() = state.get().ordinal < State.TERMINATING.ordinal

    fun start() = state.set(State.RUNNING)

    fun beginTerminate() {
        if (state.get() != State.TERMINATED) {
            state.set(State.TERMINATING)
        }
    }

    fun terminate() = state.set(State.TERMINATED)

    private fun sniffVersion(): String {
        var version = System.getProperty("app.version")
        if (version == null) {
            version = Paths.get(USER_DIR).resolve("VERSION").takeIf { Files.exists(it) }
                ?.let { Files.readAllLines(it).firstOrNull() }
        }
        return version ?: "unknown"
    }

    private fun checkIsWSL(): Boolean {
        try {
            val path = Paths.get("/proc/version")
            if (Files.isReadable(path)) {
                val version = Files.readString(path)
                logger.info("Version: $version")

                if (version.contains("microsoft-*-WSL")) {
                    return true
                }
            }
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }

        return false
    }
}
