package ai.platon.pulsar.ql.start;

import ai.platon.pulsar.PulsarEnv;
import ai.platon.pulsar.common.SystemKt;
import org.h2.tools.Console;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Server start port
 * */
@SpringBootApplication
public class H2DbConsole {
    private static PulsarEnv env = PulsarEnv.Companion.getOrCreate();
    private static ApplicationContext applicationContext;

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            System.out.println("Bean definition names: ");
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

            new Console().runTool(args);
        };
    }

    public static void main(String[] args) {
        SystemKt.setPropertyIfAbsent("h2.sessionFactory", ai.platon.pulsar.ql.h2.H2SessionFactory.class.getName());
        SpringApplication.run(H2DbConsole.class, args);
    }
}
