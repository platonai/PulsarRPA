package ai.platon.pulsar.boot.common

import ai.platon.pulsar.common.getLogger
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.util.ClassUtils
import java.io.IOException

class SpringPackageScanClassResolver {
    private val logger = getLogger(this)

    fun findAllClasses(packageName: String, loader: ClassLoader): Set<Class<*>> {
        val metadataReaderFactory = CachingMetadataReaderFactory(loader)
        val foundClasses = mutableSetOf<Class<*>>()

        try {
            val resources: Array<Resource> = scan(loader, packageName)
            for (resource in resources) {
                val clazz = loadClass(loader, metadataReaderFactory, resource)
                clazz?.let { foundClasses.add(it) }
            }
        } catch (ex: IOException) {
            throw IllegalStateException(ex)
        }

        return foundClasses
    }

    @Throws(IOException::class)
    private fun scan(loader: ClassLoader?, packageName: String): Array<Resource> {
        val resolver: ResourcePatternResolver = PathMatchingResourcePatternResolver(
            loader
        )
        val pattern = (ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                + ClassUtils.convertClassNameToResourcePath(packageName)) + "/**/*.class"
        return resolver.getResources(pattern)
    }

    private fun loadClass(
        loader: ClassLoader?, readerFactory: MetadataReaderFactory,
        resource: Resource
    ): Class<*>? {
        return try {
            val reader: MetadataReader = readerFactory.getMetadataReader(resource)
            ClassUtils.forName(reader.getClassMetadata().getClassName(), loader)
        } catch (ex: ClassNotFoundException) {
            handleFailure(resource, ex)
            null
        } catch (ex: LinkageError) {
            handleFailure(resource, ex)
            null
        } catch (ex: Throwable) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                    "Unexpected failure when loading class resource $resource", ex
                )
            }
            null
        }
    }

    private fun handleFailure(resource: Resource, ex: Throwable) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                "Ignoring candidate class resource $resource due to $ex"
            )
        }
    }
}
