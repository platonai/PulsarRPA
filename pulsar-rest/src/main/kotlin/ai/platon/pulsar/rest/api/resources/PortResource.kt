package ai.platon.pulsar.rest.api.resources

import ai.platon.pulsar.rest.api.entities.ServerInstance
import ai.platon.pulsar.rest.api.common.PortManager
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/port")
class PortResource {

    private val portManagers = ConcurrentHashMap<String, PortManager>()

    init {
        var portManager = PortManager(ServerInstance.Type.FetchService.name)
        portManagers[portManager.type] = portManager

        portManager = PortManager(ServerInstance.Type.PulsarMaster.name)
        portManagers[portManager.type] = portManager
    }

    @GetMapping("/get-empty-port-manager")
    fun getManager(@PathVariable("type") type: String): PortManager {
        return PortManager(type, 0, 0)
    }

    @GetMapping("/report")
    fun report(): String {
        val report = portManagers.values
                .joinToString { p -> "\tPorts for " + p.type + " :\n" + p.toString() }
        return "PortResource #" + hashCode() + report
    }

    // TODO : Failed to return a list of integer, we do not if it's a jersey bug or dependency issue
    @GetMapping("/active")
    fun activePorts(@PathVariable("type") type: String): List<Int> {
        return portManagers[type]?.getActivePorts()?:listOf()
    }

    @GetMapping("/free")
    fun getFreePorts(@PathVariable("type") type: String): List<Int> {
        return portManagers[type]?.getFreePorts()?: listOf()
    }

    @GetMapping("/acquire")
    fun acquire(@PathVariable("type") type: String): Int? {
        return portManagers[type]?.acquire()?:-1
    }

    @PutMapping("/recycle")
    fun recycle(@PathVariable("type") type: String, @PathVariable("port") port: Int) {
        portManagers[type]?.recycle(port)
    }
}
