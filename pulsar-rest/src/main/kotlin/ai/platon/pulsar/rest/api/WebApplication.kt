package ai.platon.pulsar.rest.api

import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ImportResource

@ImportResource("classpath:rest-beans/app-context.xml")
class WebApplication : SpringBootServletInitializer()
