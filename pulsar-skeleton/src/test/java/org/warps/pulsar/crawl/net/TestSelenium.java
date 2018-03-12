package org.warps.pulsar.crawl.net;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.warps.pulsar.common.config.MutableConfig;
import org.warps.pulsar.crawl.protocol.Response;
import org.warps.pulsar.net.SeleniumEngine;

import java.time.Duration;

import static org.warps.pulsar.common.config.CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT;
import static org.warps.pulsar.net.WebDriverQueues.*;

public class TestSelenium {

    MutableConfig conf = new MutableConfig();
    SeleniumEngine engine;
    String productIndexUrl = "https://www.mia.com/formulas.html";

    @Before
    public void setup() {
        engine = new SeleniumEngine(conf);
        conf.setDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(15));
    }

    @Test
    public void testCapabilities() {
        DEFAULT_CHROME_CAPABILITIES.addArguments("--blink-settings=imagesEnabled=false");
        DEFAULT_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        DEFAULT_CHROME_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        WebDriver driver = new ChromeDriver(DEFAULT_CHROME_CAPABILITIES);

        DEFAULT_CHROME_CAPABILITIES.addArguments("--blink-settings=imagesEnabled=false");
        DEFAULT_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        DEFAULT_CHROME_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        driver = new ChromeDriver(DEFAULT_CHROME_CAPABILITIES);
    }

    @Test
    public void testCompare() {
        Response response = engine.fetch("http://www.codeweblog.com/stag/hsql-vs-h2/");
        System.out.println(new String(response.getContent()).substring(0, 1000));
    }
}
