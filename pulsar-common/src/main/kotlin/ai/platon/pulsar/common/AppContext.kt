package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import java.net.InetAddress
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

object AppContext {

    enum class State {
        NEW, RUNNING, TERMINATING, TERMINATED
    }

    /**
     * Date time
     */
    val startTime = Instant.now()
    val defaultZoneId = ZoneId.systemDefault()

    val midnight get() = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
    val startOfHour get() = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)

    val elapsed get() = Duration.between(startTime, Instant.now())
    val elapsedToday get() = Duration.between(midnight, LocalDateTime.now())
    val elapsedThisHour get() = Duration.between(startOfHour, LocalDateTime.now())

    /**
     * The number of processors available to the Java virtual machine
     */
    val NCPU = Runtime.getRuntime().availableProcessors()

    // User's current working directory
    val HOST_NAME = InetAddress.getLocalHost().hostName

    val USER = SystemUtils.USER_NAME

    /**
     * Directories
     */
    val TMP_DIR = SystemUtils.JAVA_IO_TMPDIR
    // User's home directory
    val USER_HOME = SystemUtils.USER_HOME
    // User's current working directory
    val USER_DIR = SystemUtils.USER_DIR
    // The identity of this running instance
    val APP_NAME = System.getProperty("app.name", "pulsar")
    val APP_IDENT = System.getProperty("app.id.str", USER)
    val APP_TMP_PROPERTY = System.getProperty("app.tmp.dir")
    val APP_TMP_DIR = if (APP_TMP_PROPERTY != null) Paths.get(APP_TMP_PROPERTY) else Paths.get(TMP_DIR).resolve("$APP_NAME-$APP_IDENT")
    val APP_DATA_DIR = Paths.get(USER_HOME).resolve(".$APP_NAME")

    val state = AtomicReference(State.NEW)

    val isActive get() = state.get().ordinal < State.TERMINATING.ordinal

    fun start() = state.set(State.RUNNING)

    fun tryTerminate() {
        if (state.get() != State.TERMINATED) {
            state.set(State.TERMINATING)
        }
    }

    fun terminate() = state.set(State.TERMINATED)
}
