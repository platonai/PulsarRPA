package fun.platonic.pulsar.common.options.converters;

import com.beust.jcommander.IStringConverter;
import fun.platonic.pulsar.common.StringUtil;

import java.util.List;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class SlashedLinesReader implements IStringConverter<List<String>> {
    @Override
    public List<String> convert(String value) {
        return StringUtil.getUnslashedLines(value);
    }
}
