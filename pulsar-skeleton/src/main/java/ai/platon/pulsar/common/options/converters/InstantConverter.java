package ai.platon.pulsar.common.options.converters;

import ai.platon.pulsar.common.SParser;
import com.beust.jcommander.IStringConverter;

import java.time.Instant;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class InstantConverter implements IStringConverter<Instant> {
    @Override
    public Instant convert(String value) {
        return new SParser(value).getInstant(Instant.EPOCH);
    }
}
