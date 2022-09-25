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
    2. create webdriver for existing tab
7. try to reduce communication between pulsar and the browser, a possible way is to disable the network api
8. selenium support, and multiple browsers support
9. close irrelevant tabs

