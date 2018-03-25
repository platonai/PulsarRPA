package fun.platonic.pulsar.boilerpipe.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vincent on 16-10-27.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class ManualRules {

  public static final Map<String, String> TITLE_RULES = new HashMap<>();

  static {
    TITLE_RULES.put("http://www.sxrb.com/(.+)", "h2");
  }
}
