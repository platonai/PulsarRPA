package ai.platon.pulsar.parse.tika;

import ai.platon.pulsar.common.MimeUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.persist.WebPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by vincent on 17-1-26.
 */
@ContextConfiguration(locations = {"classpath:/test-context/parse-beans.xml"})
public class TikaTestBase {

    // This system property is defined in ./src/plugin/build-plugin.xml
    protected String sampleDir = System.getProperty("test.data", ".");

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected ImmutableConfig conf;

    @Autowired
    protected TikaParser parser;

    protected MimeUtil mimeutil = new MimeUtil(conf);

    public WebPage parse(String sampleFile) throws IOException {
        Path path = Paths.get(sampleDir, sampleFile);
        String baseUrl = "file:" + path.toAbsolutePath().toString();

        byte[] bytes = Files.readAllBytes(path);

        WebPage page = WebPage.newWebPage(baseUrl);
        page.setBaseUrl(baseUrl);
        page.setContent(bytes);
        String mtype = mimeutil.getMimeType(baseUrl);
        page.setContentType(mtype);

        parser.parse(page);

        return page;
    }
}
