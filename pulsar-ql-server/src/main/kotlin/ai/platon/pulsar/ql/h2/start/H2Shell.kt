package ai.platon.pulsar.ql.h2.start

import ai.platon.pulsar.ql.H2Config
import com.google.common.collect.Lists
import org.h2.tools.Shell
import java.util.*

internal open class LocalConsole: Shell() {
    /**
     * Run the shell tool with the default command line settings.
     *
     * @param args the command line settings
     */
    override fun runTool(vararg args: String) {
        H2Config.config()

        var url = "jdbc:h2:tcp://localhost/~/test"
        var user = "sa"
        var password = "sa"
        var driver = "org.h2.Driver"
        val options = LinkedList<String>()
        var i = 0
        while (args != null && i < args.size) {
            val arg = args[i]
            if (arg == "-url") {
                url = args[++i]
            } else if (arg == "-user") {
                user = args[++i]
            } else if (arg == "-password") {
                password = args[++i]
            } else if (arg == "-driver") {
                driver = args[++i]
            } else {
                options.add(arg)
            }
            i++
        }

        val l = Lists.newArrayList("-url", url, "-user", user, "-password", password, "-driver", driver)
        options.addAll(0, l)
        super.runTool(*options.toTypedArray())
    }
}

object H2Shell {
    @JvmStatic
    fun main(args: Array<String>) {
        LocalConsole().runTool(*args)
    }
}
