package ai.platon.pulsar.common

object PropertyNameStyle {
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
     * With the preceding code, the following properties names can all be used:
     *
     * my.main-project.person.first-name | Kebab case, which is recommended for use in .properties and .yml files.
     * my.main-project.person.firstName | Standard camel case syntax.
     * my.main-project.person.first_name | Underscore notation, which is an alternative format for use in .properties and .yml files.
     * MY_MAINPROJECT_PERSON_FIRSTNAME | Upper case format, which is recommended when using system environment variables.
     *
     * * [relaxed-binding](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding)
     * */
    fun toDotSeparatedKebabCase(input: String): String {
        return input
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")     // camelCase → camel-Case
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1.$2")   // ABCWord → ABC.Word
            .replace(Regex("[_\\s]+"), ".")                   // _  空格 → .
            .lowercase()
    }

    /**
     * Most operating systems impose strict rules around the names that can be used for environment variables.
     * For example, Linux shell variables can contain only letters (a to z or A to Z), numbers (0 to 9) or the
     * underscore character (_). By convention, Unix shell variables will also have their names in UPPERCASE.
     *
     * This method converts a property name in canonical form to an environment variable name.
     *
     * This function takes a string input representing a property name in canonical form and converts it
     * to a string representing an environment variable name.
     *
     * To convert a property name in the canonical-form to an environment variable name you can follow these rules:
     * * Replace dots (.) with underscores (_).
     * * Remove any dashes (-).
     * * Convert to uppercase.
     *
     * For example, the configuration property spring.main.log-startup-info would be an environment
     * variable named SPRING_MAIN_LOGSTARTUPINFO.
     *
     * Example:
     * val envVarName = toUpperUnderscoreCase("example.property-name")
     * // envVarName is "EXAMPLE_PROPERTYNAME"
     *
     * Note:
     * * Canonical form refers to the standard naming format for properties, which typically uses dots to separate
     *   different parts of the name.
     * * Environment variable names usually use underscores to separate words and are entirely in uppercase.
     *
     * [relaxed-binding.environment-variables](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)
     *
     * @param input The property name in canonical form.
     * @return The converted environment variable name.
     */
    fun toUpperUnderscoreCase(input: String): String {
        return input
            .replace(Regex("[.\\s]+"), "_")              // . 空格 → _
            .replace(Regex("-+"), "")                   // remove any dashes (-)
            .uppercase()                                                   // convert to uppercase
    }
}
