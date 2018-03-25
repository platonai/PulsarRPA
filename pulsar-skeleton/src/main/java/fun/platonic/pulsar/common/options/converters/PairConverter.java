package fun.platonic.pulsar.common.options.converters;

import com.beust.jcommander.IStringConverter;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class PairConverter implements IStringConverter<Pair<Integer, Integer>> {
    @Override
    public Pair<Integer, Integer> convert(String value) {
        String[] parts = value.split(",");
        return Pair.of(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
    }
}
