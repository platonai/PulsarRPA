package fun.platonic.pulsar.parse.html.experimental;

import org.jsoup.nodes.Element;

import java.util.ArrayList;

/**
 * Created by vincent on 17-8-2.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class Selector {

    private String name;
    private Element root;
    private String css;
    private String xpath;
    private String regex;
    private ArrayList<String> fields;

    public Selector(String name, Element root) {
        this.name = name;
        this.root = root;
    }

    public Selector css(String css) {
        this.css = css;
        return this;
    }

    public Selector xpath(String xpath) {
        this.xpath = xpath;
        return this;
    }

    public Selector regex(String regex) {
        this.regex = regex;
        return this;
    }

    public ArrayList<String> extract() {
        root.select(css).forEach(e -> fields.add(e.text()));
        // root.xpath(xpath).forEach(e -> fields.add(e.text()));
        return fields;
    }
}
