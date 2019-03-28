package ai.platon.pulsar.crawl.net;

import ai.platon.pulsar.common.config.MutableConfig;
import ai.platon.pulsar.net.SeleniumEngine;
import ai.platon.pulsar.net.WebDriverQueues;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.CapabilityType;

import java.time.Duration;

import static ai.platon.pulsar.common.config.CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT;

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
        WebDriverQueues.DEFAULT_CHROME_CAPABILITIES.addArguments("--blink-settings=imagesEnabled=false");
        WebDriverQueues.DEFAULT_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        WebDriverQueues.DEFAULT_CHROME_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        WebDriver driver = new ChromeDriver(WebDriverQueues.DEFAULT_CHROME_CAPABILITIES);

        WebDriverQueues.DEFAULT_CHROME_CAPABILITIES.addArguments("--blink-settings=imagesEnabled=false");
        WebDriverQueues.DEFAULT_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        WebDriverQueues.DEFAULT_CHROME_CAPABILITIES.setCapability(CapabilityType.PROXY, (Object) null);
        driver = new ChromeDriver(WebDriverQueues.DEFAULT_CHROME_CAPABILITIES);
    }
}
