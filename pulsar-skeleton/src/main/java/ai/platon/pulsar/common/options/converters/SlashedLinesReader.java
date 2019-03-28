package ai.platon.pulsar.common.options.converters;

import ai.platon.pulsar.common.StringUtil;
import com.beust.jcommander.IStringConverter;

import java.util.List;

/**
 * Created by vincent on 17-4-7.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class SlashedLinesReader implements IStringConverter<List<String>> {
    @Override
    public List<String> convert(String value) {
        return StringUtil.getUnslashedLines(value);
    }
}
