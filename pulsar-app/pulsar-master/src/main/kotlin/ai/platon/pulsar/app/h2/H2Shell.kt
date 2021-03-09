package ai.platon.pulsar.app.h2

import ai.platon.pulsar.ql.H2Config
import org.h2.tools.Shell
import java.util.*

open class H2Shell: Shell() {
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
        while (i < args.size) {
            when (val arg = args[i]) {
                "-url" -> url = args[++i]
                "-user" -> user = args[++i]
                "-password" -> password = args[++i]
                "-driver" -> driver = args[++i]
                else -> options.add(arg)
            }
            i++
        }

        val l = listOf("-url", url, "-user", user, "-password", password, "-driver", driver)
        options.addAll(0, l)
        super.runTool(*options.toTypedArray())
    }
}

fun main(args: Array<String>) {
    H2Shell().runTool(*args)
}
