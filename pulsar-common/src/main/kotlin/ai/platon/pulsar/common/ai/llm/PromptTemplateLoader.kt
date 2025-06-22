package ai.platon.pulsar.common.ai.llm

import ai.platon.pulsar.common.code.ProjectUtils
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText

open class PromptTemplateLoader(
    val resource: String,
    private val fallbackTemplate: String,
    private val variables: Map<String, Any> = emptyMap(),
    private val reservedVariables: List<String> = emptyList(),
) {
    val templatePath: Path? by lazy { searchForTemplate() }

    fun load(): PromptTemplate {
        return PromptTemplate(
            template = templatePath?.readText() ?: fallbackTemplate,
            variables = variables,
            reservedVariables = reservedVariables
        )
    }

    /**
     * Search for the template file in the project root directory, then in the current directory,
     * and finally in the parent directories until the root directory is reached.
     * */
    private fun searchForTemplate(): Path? {
        var path = ProjectUtils.findProjectRootDir()?.resolve(resource)
        if (path != null && path.exists()) {
            return path
        }

        path = Paths.get(".").toAbsolutePath().resolve(resource)
        if (path != null && path.exists()) {
            return path
        }

        path = Paths.get(".")
        while (path != null && path.toAbsolutePath().toString() != "/") {
            val templatePath = path.resolve(resource)
            if (templatePath.exists()) {
                return templatePath
            }
            path = path.parent
        }

        return null
    }
}
