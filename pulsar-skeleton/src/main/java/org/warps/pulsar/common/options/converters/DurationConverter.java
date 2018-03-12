package org.warps.pulsar.common.options.converters;

import com.beust.jcommander.IStringConverter;
import org.warps.pulsar.common.SParser;

import java.time.Duration;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class DurationConverter implements IStringConverter<Duration> {
    @Override
    public Duration convert(String value) {
        return new SParser(value).getDuration();
    }
}
