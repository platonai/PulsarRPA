package ai.platon.pulsar.common

object PropertyNameCases {
    /**
     * Converts environment variable names to Spring Boot property format.
     * Example:
     * * 'SPRING_PROFILES_ACTIVE' -> 'spring.profiles.active'
     * * 'server.servlet.contextPath' -> 'server.servlet.context-path'
     *
     * | 设置方式              | 写法                               | 说明 |
     * |---------------------|----------------------------------- |------|
     * | 🖥️ 命令行参数         | `--spring.profiles.active=prod`   | 优先级最高，常用于容器运行时 |
     * | 🏁 JVM 参数          | `-Dspring.profiles.active=prod`   | 适合在运行 jar 时传参 |
     * | ☁️ 环境变量           | `SPRING_PROFILES_ACTIVE=prod`     | Spring Boot 会自动将环境变量名转为配置 key |
     * | 🔧 application.yml  | `spring.profiles.active: prod`    | 推荐用于本地默认配置 |
     *
     * * [relaxed-binding](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding)
     * */
    fun toDotSeparatedKebabCase(input: String): String {
        return input
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")     // camelCase → camel-Case
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1.$2")   // ABCWord → ABC.Word
            .replace(Regex("[_\\s]+"), ".")                 // _  空格 → .
            .lowercase()
    }

    /**
     * Converts a property name in canonical form to an environment variable name.
     *
     * This function takes a string input representing a property name in canonical form and converts it
     * to a string representing an environment variable name. The conversion follows these rules:
     * * Replace dots (.) with underscores (_).
     * * Convert to uppercase.
     * * Remove any dashes (-).
     *
     * @param input The property name in canonical form.
     * @return The converted environment variable name.
     *
     * Example:
     * val envVarName = toUpperUnderscoreCase("example.property-name")
     * // envVarName is "EXAMPLE_PROPERTYNAME"
     *
     * Note:
     * * Canonical form refers to the standard naming format for properties, which typically uses dots to separate
     *   different parts of the name.
     * * Environment variable names usually use underscores to separate words and are entirely in uppercase.
     */
    fun toUpperUnderscoreCase(input: String): String {
        return input
            .replace(Regex("[.\\s]+"), "_")              // . 空格 → _
            .uppercase()                                                   // convert to uppercase
            .replace(Regex("-+"), "")                   // remove any dashes (-)
    }
}
