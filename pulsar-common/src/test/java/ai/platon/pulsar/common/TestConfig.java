package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.common.config.VolatileConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.BufferedReader;
import java.time.Duration;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 17-1-14.
 */
public class TestConfig {

    @Test
    public void testConfig() {
        ImmutableConfig conf = new ImmutableConfig(false);
        System.out.println(conf.toString());
//    assertFalse(conf.toString().contains("pulsar-default.xml"));
//
//    conf = new ImmutableConfig();
//    assertTrue(conf.toString().contains("pulsar-default.xml"));

        new BufferedReader(conf.getConfResourceAsReader("log4j.properties")).lines().forEach(System.out::println);
    }

    @Test
    public void testDuration() {
        MutableConfig conf = new MutableConfig();
        // ISO-8601 format
        conf.set("d1", "p3d");
        conf.set("d2", "pt2h");
        conf.set("d3", "pt3m");
        conf.set("d4", "-pt3s");
        assertEquals("PT72H", conf.getDuration("d1", Duration.ZERO).toString());
        assertEquals("PT2H", conf.getDuration("d2", Duration.ZERO).toString());
        assertEquals("PT3M", conf.getDuration("d3", Duration.ZERO).toString());
        assertEquals("PT-3S", conf.getDuration("d4", Duration.ZERO).toString());

        // Hadoop format
        conf.set("hd1", "1d");
        conf.set("hd2", "3m");
        conf.set("hd3", "5s");
        assertEquals("PT24H", conf.getDuration("hd1", Duration.ZERO).toString());
        assertEquals("PT3M", conf.getDuration("hd2", Duration.ZERO).toString());
        assertEquals("PT5S", conf.getDuration("hd3", Duration.ZERO).toString());
    }

    @Test
    public void testCollection() {
        MutableConfig conf = new MutableConfig();

        conf.set("test.collection", "a\nb,\nc,\nd");
        assertEquals(3, conf.getTrimmedStringCollection("test.collection").size());

        conf.getTrimmedStringCollection("test.collection").stream().map(l -> l + " -> " + l.length())
                .forEach(System.out::println);

        conf.set("test.collection", "a,\nb,\nc,\nd");
        assertEquals(4, conf.getTrimmedStringCollection("test.collection").size());
    }

    @Test
    public void testStrings() {
        MutableConfig conf = new MutableConfig();

        String n1 = "n1";
        String v1 = "a,b,c,d";
        conf.set(n1, v1);

        assertEquals(v1, conf.get(n1));
        assertEquals(4, conf.getStrings(n1).length);
    }

    @Test
    public void testStrings2() {
        VolatileConfig conf = new VolatileConfig();

        String n1 = "n1";
        String v1 = "a,b,c,d";
        conf.set(n1, v1);

        assertEquals(v1, conf.get(n1));
        assertEquals(4, conf.getStrings(n1).length);
    }

    @Test
    public void testFallback() {
        MutableConfig mutableConfig = new MutableConfig();
        String n1 = "n1";
        String v1 = "a,b,c,d";
        mutableConfig.set(n1, v1);

        VolatileConfig conf = new VolatileConfig(mutableConfig);
        assertEquals(v1, conf.get(n1));
        System.out.println(StringUtils.join(conf.getStrings(n1), ", "));
        assertEquals(4, conf.getStrings(n1).length);
    }
}
