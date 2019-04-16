package ai.platon.pulsar.crawl.common;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.options.PulsarOptions;
import ai.platon.pulsar.common.options.converters.WeightedKeywordsConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class TestJCommander {

    private ImmutableConfig conf = new ImmutableConfig();

    @Test
    public void quoted() {
        class Cmd {
            @Parameter(names = {"-instance", "-ins"}, required = true, description = "Instance ID")
            private List<String> instances = new LinkedList<>();
        }

        Cmd cmd = new Cmd();
        JCommander.newBuilder().addObject(cmd)
                .args(new String[] {"-ins", "\"string one\"","-ins",  "\"string two\""})
                .build();

        assertEquals(cmd.instances.size(), 2);

        String args = "-ins \"string one\" -ins \"string two\"";
        String[] argv = PulsarOptions.split(args);

        Cmd cmd2 = new Cmd();
        JCommander.newBuilder().addObject(cmd2).args(argv).build();
        assertEquals(cmd2.instances.size(), 2);

        // System.out.println(String.join(" | ", argv));
    }

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
