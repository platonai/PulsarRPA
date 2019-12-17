package ai.platon.pulsar.jobs.app

import ai.platon.pulsar.common.AppFiles
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.jobs.JobEnv
import ai.platon.pulsar.jobs.app.fetch.FetchJob
import ai.platon.pulsar.jobs.app.generate.GenerateJob
import ai.platon.pulsar.jobs.app.homepage.TopPageHomeUpdateJob
import ai.platon.pulsar.jobs.app.inject.InjectJob
import ai.platon.pulsar.jobs.parse.ParserJob
import ai.platon.pulsar.jobs.app.update.In2OutGraphUpdateJob
import ai.platon.pulsar.jobs.app.update.Out2InGraphUpdateJob
import ai.platon.pulsar.jobs.core.AppContextAwareJob
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean

@SpringBootApplication
class JobRunner {
    private val log = LoggerFactory.getLogger(JobRunner::class.java)

    val jobs = mapOf(
            "inject" to InjectJob(),
            "fetch" to FetchJob(),
            "generate" to GenerateJob(),
            "parse" to ParserJob(),
            "updateoutgraph" to Out2InGraphUpdateJob(),
            "updateingraph" to In2OutGraphUpdateJob(),
            "homepage" to TopPageHomeUpdateJob()
    )

    @Bean
    fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner { args ->
            val beans = ctx.beanDefinitionNames.sorted()
            val s = beans.joinToString("\n") { it }
            val path = AppPaths.getTmp("spring-beans.txt")
            AppFiles.saveTo(s, path)
            log.info("Report of all active spring beans is written to $path")

            runJob(args)
        }
    }

    private fun runJob(args: Array<String>) {
        val jobName = args.intersect(jobs.keys).lastOrNull()?:"homepage"
        val args2 = args.filterNot { it == jobName }.toTypedArray()
        AppContextAwareJob.run(jobs[jobName], args2)
    }
}

fun main(args: Array<String>) {
    val application = SpringApplication(JobRunner::class.java)

    val event = ApplicationListener<ApplicationEnvironmentPreparedEvent> {
        JobEnv.initialize()
    }
    application.addListeners(event)

    application.run(*args)
}
