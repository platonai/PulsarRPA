package ai.platon.pulsar.common;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

/**
 * Created by vincent on 17-1-14.
 */
public class TestJson {

    String[] urls = {
            "http://sz.sxrb.com/sxxww/dspd/szpd/bwch/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/fcjjjc/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/hydt/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/jykj_0/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/qcjt/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/wsjk/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/wyss/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/zjaq/"
    };

    @Test
    public void testCollection() {
        Gson gson = new GsonBuilder().create();
        System.out.println(gson.toJson(urls));
        System.out.println(gson.toJson(Sets.newHashSet(urls)));
    }

    @Test
    public void testTab() {
        String seed = "http://www.sxrb.com/sxxww/\t-i pt1s -p";
        Gson gson = new GsonBuilder().create();
        System.out.println(gson.toJson(seed));
    }
}
