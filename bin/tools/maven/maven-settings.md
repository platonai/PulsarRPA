# Maven 配置说明

For Chinese developers, we suggest that you add the domestic maven mirror in the global maven settings to speed up the build.

对于中国开发者，我们建议您在全局 maven 设置中添加国内的 maven 镜像，以加快构建速度。

On Linux, the path of the global maven settings is:

    ~/.m2/settings.xml

on Windows,

    %UserProfile%\.m2\settings.xml

If the file doesn't exist, just copy link:cn/settings.xml[settings.xml] to the proper directory.

On Linux:

    cp bin/tools/maven/cn/settings.xml ~/.m2

On Windows:

    copy ".\bin\tools\maven\cn\settings.xml" "%UserProfile%\.m2"

If the file exists, add the following code to the file:

    <mirror>
        <id>huaweicloud</id>
        <url>https://repo.huaweicloud.com/repository/maven/</url>
        <mirrorOf>central</mirrorOf>
    </mirror>
