## 1.11.x

1. upgrade kotlin to 1.7.20
2. upgrade spring to 5.3.23
3. upgrade spring-boot to 2.6.12
4. WebPage refactoring: make WebPage be an interface

## 1.10.x

1. advanced rest api
2. executable jar
3. new load option: -fieldSelectors used for scrape methods
4. improve MockWebDriver, use FileBackendStore for mocked pages, and rise a mock server
5. more event handlers
    1. onWillCheckHTML/onRegisterHTMLChecker
    2. onWillRetry
    3. onWillSniffPageCategory/onRegisterPageCategorySniffer
    4. onRegisterUserAgent?
    5. onInitUrlBlockingRules
    6. onInitRetryDelayPolicy
6. RPA supports
    1. frames
    2. **finished** create webdriver for existing tab
7. try to reduce communication between pulsar and the browser, a possible way is to disable the network api
8. selenium support, and multiple browsers support
9. support web driver's opener
   1. for chrome devtools, the opener can be found via ChromeTab.parentId
