# About jvm.config

This file is used to configure the JVM options for the Maven build. The default options are:

```
--add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
```

The options are used to fix "Kapt is not compatible with JDK 16+" issue.

See also:

* [Kapt is not compatible with JDK 16+](https://youtrack.jetbrains.com/issue/KT-45545#focus=Comments-27-4773544.0-0)
* [Kapt is not working properly with OpenJDK 16](https://stackoverflow.com/questions/67509099/kapt-is-not-working-properly-with-openjdk-16)