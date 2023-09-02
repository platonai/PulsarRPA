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
    2. [Fixed] create webdriver for existing tab
7. [Won't Fix] try to reduce communication between pulsar and the browser, a possible way is to disable the network api
8. selenium support, and multiple browsers support
9. support web driver's opener
   1. for chrome devtools, the opener can be found via ChromeTab.parentId
10. Webpage annotation system
11. more tests
