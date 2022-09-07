## 1.10.x

1. advanced rest api
1. executable jar
1. new load option: -fieldSelectors used for scrape methods
1. improve MockWebDriver, use FileBackendStore for mocked pages, and rise a mock server
1. more event handlers
    1. onWillCheckHTML/onRegisterHTMLChecker
    2. onWillRetry
    3. onWillSniffPageCategory/onRegisterPageCategorySniffer
    4. onRegisterUserAgent
    5. onRegisterUrlBlockingRules
    6. onInitRetryDelayPolicy
1. RPA supports
    1. frames
    2. create webdriver for existing tab
1. try to reduce communication between pulsar and the browser, a possible way is to disable the network api
1. selenium support, and multiple browsers support
