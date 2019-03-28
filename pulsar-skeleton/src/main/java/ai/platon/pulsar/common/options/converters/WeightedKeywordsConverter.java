package ai.platon.pulsar.common.options.converters;

import com.beust.jcommander.IStringConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class WeightedKeywordsConverter implements IStringConverter<Map<String, Double>> {
    @Override
    public Map<String, Double> convert(String value) {
        Map<String, Double> keywords = new HashMap<>();
        value = StringUtils.remove(value, ' ');
        String[] parts = value.split(",");
        for (String part : parts) {
            String k = part;
            String v = "1";

            int pos = part.indexOf('^');
            if (pos >= 1 && pos < part.length() - 1) {
                k = part.substring(0, pos);
                v = part.substring(pos + 1);
            }

            keywords.put(k, NumberUtils.toDouble(v, 1.0));
        }

        return keywords;
    }
}
