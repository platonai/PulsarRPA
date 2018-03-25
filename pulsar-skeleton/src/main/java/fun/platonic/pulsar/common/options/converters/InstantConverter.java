package fun.platonic.pulsar.common.options.converters;

import com.beust.jcommander.IStringConverter;
import fun.platonic.pulsar.common.SParser;

import java.time.Instant;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class InstantConverter implements IStringConverter<Instant> {
    @Override
    public Instant convert(String value) {
        return new SParser(value).getInstant(Instant.EPOCH);
    }
}
