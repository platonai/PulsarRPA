package fun.platonic.pulsar.crawl.common;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.options.converters.WeightedKeywordsConverter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestConverters {

    private ImmutableConfig conf = new ImmutableConfig();

    @Test
    public void testWeightedKeywordsConverter() {
        WeightedKeywordsConverter converter = new WeightedKeywordsConverter();
        Map<String, Double> answer = new HashMap<>();
        answer.put("a", 1.1);
        answer.put("b", 2.0);
        answer.put("c", 0.2);
        answer.put("d", 1.0);
        answer.put("e^", 1.0);
        answer.put("^1", 1.0);
        answer.put("^", 1.0);

        assertEquals("Not match", answer, converter.convert("a^1.1,     b^2.0,c^0.2,d,e^,^1,^,"));
    }
}
