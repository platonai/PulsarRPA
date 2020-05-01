package ai.platon.pulsar.filter;

import ai.platon.pulsar.crawl.filter.UrlFilter;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

/**
 * Created by vincent on 17-2-23.
 */
public class TestAutomatonUrlFilter extends RegexUrlFilterBaseTest {

    private static String SAMPLES_DIR = Paths.get(TEST_DIR, "automaton", "sample").toString();

    public TestAutomatonUrlFilter() {
        super(SAMPLES_DIR);
    }

    protected UrlFilter getURLFilter(Reader reader) {
        return new AutomatonUrlFilter(reader, conf);
    }

    @Test
    public void test() {
        test("WholeWebCrawling");
        test("IntranetCrawling");
        bench(50, "Benchmarks");
        bench(100, "Benchmarks");
        bench(200, "Benchmarks");
        bench(400, "Benchmarks");
        bench(800, "Benchmarks");
    }
}
