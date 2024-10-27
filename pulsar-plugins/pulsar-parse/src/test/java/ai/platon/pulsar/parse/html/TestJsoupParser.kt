
package ai.platon.pulsar.parse.html

import kotlin.test.*
import java.io.IOException
import java.nio.file.Paths

/**
 * Unit tests for PrimerParser
 */
class TestJsoupParser {
    @Test
    @Throws(IOException::class)
    fun test1() {
        val path = Paths.get(SAMPLES_DIR, "selector/1/pages/html_example_3_news.html")
        // Jsoup.parse(path.toFile(), Charset.defaultCharset().toString());
//    JsoupParser parser = new JsoupParser(path);
//    parser.css("author", ".editer!")
//      .css(".art_tit!", ".art_txt!", ".art_info!", ".not-exist")
//      .as(parser)
//      .extract()
//      .assertContains("author", "（责任编辑：刘洋）")
//      .assertContainsValue("（责任编辑：刘洋）");
    }

    companion object {
        val SAMPLES_DIR = System.getProperty("test.data", ".")
    }
}