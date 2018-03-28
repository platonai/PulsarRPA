package fun.platonic.pulsar.common.options.converters;

import com.beust.jcommander.IStringConverter;
import fun.platonic.pulsar.common.SParser;

import java.time.Duration;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class DurationConverter implements IStringConverter<Duration> {
    @Override
    public Duration convert(String value) {
        return new SParser(value).getDuration();
    }
}
