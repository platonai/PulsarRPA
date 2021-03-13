package ai.platon.pulsar.ql.h2

import ai.platon.pulsar.ql.h2.udfs.StringFunctions
import org.h2.util.Utils

/**
 * Register Apache commons utility, required by [StringFunctions]
 * */
//class ApacheStringUtilsClassFactory : Utils.ClassFactory {
//    override fun match(name: String): Boolean {
//        return name.startsWith("org.apache.commons.lang3.StringUtils")
//    }
//
//    @Throws(ClassNotFoundException::class)
//    override fun loadClass(name: String): Class<*> {
//        return org.apache.commons.lang3.StringUtils::class.java.classLoader.loadClass(name)
//    }
//}
