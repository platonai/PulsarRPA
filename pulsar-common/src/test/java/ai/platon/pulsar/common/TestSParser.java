package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.MutableConfig;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 17-1-14.
 */
public class TestSParser {
    private final MutableConfig conf = new MutableConfig();

    private final SParser parser = new SParser();

    @Test
    public void testParseCollection() {
        parser.set("a\nb,\nc,\nd");
        assertEquals(3, parser.getTrimmedStringCollection().size());

        parser.getTrimmedStringCollection().stream().map(l -> l + " -> " + l.length())
                .forEach(System.out::println);

        parser.set("a,\nb,\nc,\nd");
        assertEquals(4, parser.getTrimmedStringCollection().size());
    }

    @Test
    public void testParseDuration() {
        // Hadoop format
//        conf.set("t1", "1ms");
//        assertEquals(Duration.ofMillis(1).toMillis(), conf.getTimeDuration("t1", Integer.MIN_VALUE, TimeUnit.MILLISECONDS));

        parser.set("1s");
        assertEquals(Duration.ofSeconds(1), parser.getDuration());

        parser.set("pt1s");
        assertEquals(Duration.ofSeconds(1), parser.getDuration());

        parser.set("1s");
        assertEquals(Duration.ofSeconds(1), parser.getDuration());

        parser.set("1h");
        assertEquals(Duration.ofHours(1), parser.getDuration());

        parser.set("1ms");
        assertEquals(Duration.ofMillis(1), parser.getDuration());

        parser.set("500ms");
        assertEquals(Duration.ofMillis(500), parser.getDuration());
    }
}
