package ai.platon.pulsar.parse.tika

import ai.platon.pulsar.common.MimeUtil
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by vincent on 17-1-26.
 */
@ContextConfiguration(locations = ["classpath:/test-context/parse-beans.xml"])
open class TikaTestBase {
    // This system property is defined in ./src/plugin/build-plugin.xml
    protected var sampleDir = System.getProperty("test.data", ".")
    @Autowired
    protected var applicationContext: ApplicationContext? = null
    @Autowired
    protected var conf: ImmutableConfig? = null
    @Autowired
    protected var parser: TikaParser? = null
    protected var mimeutil = MimeUtil(conf)
    @Throws(IOException::class)
    fun parse(sampleFile: String?): WebPage {
        val path = Paths.get(sampleDir, sampleFile)
        val baseUrl = "file:" + path.toAbsolutePath().toString()
        val bytes = Files.readAllBytes(path)
        val page = WebPage.newWebPage(baseUrl)
        page.location = baseUrl
        page.setContent(bytes)
        val mtype = mimeutil.getMimeType(baseUrl)
        page.contentType = mtype
        parser!!.parse(page)
        return page
    }
}