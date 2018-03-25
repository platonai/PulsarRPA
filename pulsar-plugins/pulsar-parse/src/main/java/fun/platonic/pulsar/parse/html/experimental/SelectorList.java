package fun.platonic.pulsar.parse.html.experimental;

import fun.platonic.pulsar.common.options.EntityOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vincent on 17-8-2.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class SelectorList {

    private List<Selector> selectors = new ArrayList<>();
    private Map<String, String> fields;

    public SelectorList() {

    }

    public static SelectorList parse(String args) {
        EntityOptions options = new EntityOptions(args);
        options.parse();
        SelectorList selectorList = new SelectorList();
        options.getCssRules().forEach(selectorList::css);
        return selectorList;
    }

    public SelectorList css(String css) {
        return this;
    }

    public SelectorList css(String name, String css) {
        return this;
    }

    public SelectorList xpath(String xpath) {
        return this;
    }

    public SelectorList xpath(String name, String xpath) {
        return this;
    }

    public SelectorList regex(String regex) {
        return this;
    }

    public SelectorList regex(String name, String regex) {
        return this;
    }

    public Map<String, String> extract() {
        return fields;
    }
}
