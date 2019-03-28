package ai.platon.pulsar.crawl.parse.html;

import java.util.HashMap;

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class FieldCollection extends HashMap<CharSequence, CharSequence> {
    private String name;
    private int required = 0;
    private int loss = 0;

    public FieldCollection() {

    }

    public FieldCollection(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        put("name", name);
    }

    public int getRequired() {
        return required;
    }

    public void setRequired(int required) {
        this.required = required;
    }

    public void increaseRequired(int count) {
        this.required += count;
    }

    public int getLoss() {
        return loss;
    }

    public void setLoss(int loss) {
        this.loss = loss;
    }

    public void loss(int loss) {
        this.loss += loss;
    }

    public FieldCollection assertContains(String key, String value) {
        assert (value.equals(get(key).toString()));
        // If (!equals) throw Exception
        return this;
    }

    public FieldCollection assertContainsKey(String... keys) {
        for (String key : keys) {
            assert (containsKey(key));
            // If (!equals) throw Exception
        }
        return this;
    }

    public FieldCollection assertContainsValue(String... values) {
        for (String value : values) {
            assert (containsValue(value));
            // If (!equals) throw Exception
        }
        return this;
    }

    public FieldCollection assertContains(String message, String key, String value) {
        if (!value.equals(get(key).toString())) {
            System.out.println(key + ", " + "value");
            System.out.println(message);
        }
        return this;
    }
}
