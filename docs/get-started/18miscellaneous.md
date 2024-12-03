Miscellaneous
=

[Prev](16console.md) | [Home](1home.md)

Browser settings
==

`BrowserSettings` defines a straightforward interface for managing browser behavior. It offers a suite of static methods to tailor browser configurations.
For instance, to launch several ephemeral browsers in headless mode—common in web scraping operations—employ the following snippet:

```kotlin
BrowserSettings
  .headless()
  .privacy(4)
  .maxOpenTabs(12)
  .enableUrlBlocking()
```

This code accomplishes the following:

1. Initiates the browser in headless mode.
2. Configures four independent privacy contexts.
3. Limits each browser context to a maximum of 12 open tabs.
4. Activates URL blocking.

To utilize your system's default browser with a graphical user interface (GUI) and interact with web pages, use the following configuration:

```kotlin
BrowserSettings.withSystemDefaultBrowser().withGUI().withSPA()
```

The above configuration performs the following actions:

1. Employs the system's default browser.
2. Operates the browser in GUI mode.
3. Configures the system to handle single-page applications (SPA).

Privacy agents
==

A privacy agent is an identifier that ensures distinct identities for website visits. When different privacy agents are used, page visits should not be attributed to the same individual, regardless of whether they originate from the same host.

Concepts
==

To unlock the full potential of PulsarRPA and tackle even the most complex data scraping tasks, a solid understanding of its core concepts is essential. By grasping these fundamental principles, you'll be equipped to wield PulsarRPA as a powerful tool for extracting valuable information from the web.

[PulsarRPA Concepts](/docs/concepts.md).

------

[Prev](16console.md) | [Home](1home.md)
