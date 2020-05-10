# Chrome DevTools Java Client

## Description

Chrome DevTools Java Client is a DevTools client - in Java. (: It can be used for *instrumenting, inspecting, debuging and profiling Chromium, Chrome and other Blink-based browsers.* [1]

For more information on DevTools, see https://chromedevtools.github.io/devtools-protocol/.

[v1.3.6](https://github.com/kklisura/chrome-devtools-java-client/tree/v1.3.6) tested on Google Chrome Version 67.0.3396.87. Protocol files from [dev-tools-protocol#1aa7b31ca7](https://github.com/ChromeDevTools/devtools-protocol/tree/1aa7b31ca7bba982eceea8d4bd494b27850fb0df/json)

[v2.0.0](https://github.com/kklisura/chrome-devtools-java-client/tree/v2.0.0) tested on Google Chrome Version 76.0.3809.100. Protocol files from [dev-tools-protocol#e1fb93bd76](https://github.com/ChromeDevTools/devtools-protocol/tree/e1fb93bd76f99cdf401b949757c874c579e15434/json)

[1] https://chromedevtools.github.io/devtools-protocol/.

## Usage

Add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.github.kklisura.cdt</groupId>
  <artifactId>cdt-java-client</artifactId>
  <version>2.0.0</version>
</dependency>
```

You can use following code, taken from, `LogRequestsExample`:

```java
package com.github.kklisura.cdt.examples;

import com.github.kklisura.cdt.launch.ChromeLauncher;
import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;

/**
 * Log requests example with DevTools java client.
 *
 * <p>The following example will open chrome, create a tab with about:blank url, subscribe to
 * requestWillBeSent event and then navigate to github.com.
 *
 * @author Kenan Klisura
 */
public class LogRequestsExample {
  public static void main(String[] args) {
    // Create chrome launcher.
    final ChromeLauncher launcher = new ChromeLauncher();

    // Launch chrome either as headless (true) or regular (false).
    final ChromeService chromeService = launcher.launch(false);

    // Create empty tab ie about:blank.
    final ChromeTab tab = chromeService.createTab();

    // Get DevTools service to this tab
    final ChromeDevToolsService devToolsService = chromeService.createDevToolsService(tab);

    // Get individual commands
    final Page page = devToolsService.getPage();
    final Network network = devToolsService.getNetwork();

    // Log requests with onRequestWillBeSent event handler.
    network.onRequestWillBeSent(
        event ->
            System.out.printf(
                "request: %s %s%s",
                event.getRequest().getMethod(),
                event.getRequest().getUrl(),
                System.lineSeparator()));

    network.onLoadingFinished(
        event -> {
          // Close the tab and close the browser when loading finishes.
          chromeService.closeTab(tab);
          launcher.close();
        });

    // Enable network events.
    network.enable();

    // Navigate to github.com.
    page.navigate("http://github.com");

    devToolsService.waitUntilClosed();
  }
}
```

For more examples, see [examples](cdt-examples/src/main/java/com/github/kklisura/cdt/examples).

## Running unit tests

`make verify`

## Sonar analysis

`make sonar-analysis`

## Download latest protocol

Run following:
```
make download-latest-protocol
```

This will download `browser_protocol.json` and `js_protocol.json` (protocol definitions) from https://github.com/ChromeDevTools/devtools-protocol repo.

## Update protocol (generate Java files from protocol definitions)

Make sure you have correct or latest protocol definitions. See [Download latest protocol](#download-latest-protocol) on how to update protocol definitions to latest version.

Run following:
```
make update-protocol
```

This will build the tools for parsing and generating Java files, [cdt-protocol-builder](cdt-protocol-builder/) project. The input for this tool are protocol definitions files: `browser_protocol.json` and `js_protocol.json`. The generated Java files will be present in [cdt-java-client](cdt-java-client/) project. After building Java files, the [cdt-java-client](cdt-java-client/) will be compiled. If everything goes successfully, consider the protocol updated. :)

## License

Chrome DevTools Java Client is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE.txt) for the full license text.
