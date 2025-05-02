package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_DETAIL_URL
import ai.platon.pulsar.rest.api.TestUtils.PRODUCT_LIST_URL
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.rest.api.entities.PromptRequestL2
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.*

const val API_COMMAND_PROMPT1 = """
Visit https://www.amazon.com/dp/B0C1H26C46
Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
After page load: click #title, then scroll to the middle.
    """

const val API_COMMAND_PROMPT2 = """
Visit https://www.amazon.com/dp/B0C1H26C46

Page summary prompt: Provide a brief introduction of this product.
Extract fields: product name, price, and ratings.
Extract links: all links containing `/dp/` on the page.

When the page is ready, click the element with id "title" and scroll to the middle.

    """

const val API_COMMAND_PROMPT3 = """
Visit the page: https://www.amazon.com/dp/B0C1H26C46

### 📝 Tasks:

**1. Page Summary**  
Provide a brief introduction to the product.

**2. Field Extraction**  
Extract the following information from the page content:
- Product name
- Price
- Ratings

**3. Link Extraction**  
Collect all hyperlinks on the page that contain the substring `/dp/`.

**4. Page Interaction**  
Once the document is fully loaded:
- Click the element with `id="title"`
- Scroll to the middle of the page
    """

@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class PromptServiceTest {

    @Autowired
    private lateinit var conf: ImmutableConfig

    @Autowired
    private lateinit var service: PromptService

    @BeforeEach
    fun setup() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(conf))
    }

    @Test
    fun `test prompt convertion to request with cache`() {
        val url1 = "https://www.amazon.com/dp/B0C1H26C46"
        val url2 = "https://www.amazon.com/dp/B07PX3ZRJ6"

        val prompt1 = """
Visit $url1

Page summary prompt: Provide a brief introduction of this product.

        """.trimIndent()

        val result1 = service.convertAPIRequestCommandToJSON(prompt1, url1)

        val prompt2 = """
Visit $url2

Page summary prompt: Provide a brief introduction of this product.

        """.trimIndent()

        val result2 = service.convertAPIRequestCommandToJSON(prompt2, url2)

        assertEquals(result2, result1)
    }

    @Test
    fun `test prompt conversion without URL`() {
        val prompt = """
Visit amazon.com/dp/B0C1H26C46

Page summary prompt: Provide a brief introduction of this product.
        """.trimIndent()

        val request = service.convertPromptToRequest(prompt)
        assertNull(request)
    }

    /**
     * Execute a normal sql
     * */
    @Test
    fun `When chat about a page then the result is not empty`() {
        val request = PromptRequest(PRODUCT_LIST_URL, "Tell me something about the page")

        val response = service.chat(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test actions on page ready`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = PromptRequest(
            PRODUCT_DETAIL_URL, "Tell me something about the page", "", actions = actions
        )

        val response = service.chat(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test extract`() {
        val request = PromptRequest(PRODUCT_DETAIL_URL, "title, price, images")
        val response = service.extract(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test extract with actions`() {
        val actions = """
            move cursor to the element with id 'title' and click it
            scroll to middle
            scroll to top
            get the text of the element with id 'title'
        """.trimIndent().split("\n")
        val request = PromptRequest(
            PRODUCT_DETAIL_URL, "title, price, images", "", actions = actions
        )

        val response = service.extract(request)
        println(response)
        assertTrue { response.isNotEmpty() }
    }

    @Test
    fun `test command with pageSummaryPrompt`() {
        val request = PromptRequestL2(
            PRODUCT_DETAIL_URL,
            pageSummaryPrompt = "Give me the product name",
        )
        val response = service.command(request)
        Assumptions.assumeTrue(response.pageStatusCode == 200)
        println(response.pageSummary)

        assertTrue { response.isDone }
        assertNull(response.fields)
        assertNull(response.xsqlResultSet)
        assertTrue { !response.pageSummary.isNullOrBlank() }
    }

    @Test
    fun `test command with dataExtractionRules`() {
        val request = PromptRequestL2(
            PRODUCT_DETAIL_URL,
            dataExtractionRules = "product name, ratings, price",
        )
        val response = service.command(request)
        Assumptions.assumeTrue(response.pageStatusCode == 200)
        val fields = response.fields
        println(fields)

        assertTrue { response.isDone }
        assertNull(response.pageSummary)
        assertNull(response.xsqlResultSet)

        assertNotNull(fields)
        assertTrue { fields.isNotEmpty() }
    }

    @Test
    fun `test prompt convertion to request`() {
        val prompt = API_COMMAND_PROMPT1

        val request = service.convertPromptToRequest(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(request))
        assertNotNull(request)
        verifyPromptRequestL2(request)
    }

    @Test
    fun `test prompt convertion to request 2`() {
        val prompt = API_COMMAND_PROMPT2

        val request = service.convertPromptToRequest(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(request))
        assertNotNull(request)
        verifyPromptRequestL2(request)
    }

    @Test
    fun `test prompt convertion to request 3`() {
        val prompt = API_COMMAND_PROMPT3

        val request = service.convertPromptToRequest(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(request))
        assertNotNull(request)
        verifyPromptRequestL2(request)
    }

    @Test
    fun `test command 3`() {
        val prompt = API_COMMAND_PROMPT3
        val response = service.command(prompt)
        println(prettyPulsarObjectMapper().writeValueAsString(response))
        assertNotNull(response)
        Assumptions.assumeTrue(response.pageStatusCode == 200)
        assertEquals(200, response.statusCode)
        assertNotNull(response.pageSummary)
        assertNotNull(response.fields)
    }

    @Test
    fun `test convertResponseToMarkdown`() {
        val response = """
{
  "uuid" : null,
  "statusCode" : 200,
  "pageStatusCode" : 200,
  "pageContentBytes" : 2214039,
  "isDone" : true,
  "pageSummary" : "The **Huawei P60 Pro** is a premium, factory-unlocked smartphone featuring a **Dual SIM** setup, **8GB RAM**, and **256GB storage** (Global Model MNA-LX9). It comes in **Black** and is designed for global use, though it may lack warranty coverage in certain regions like the U.S.  \n\n### **Key Features:**  \n- **Display:** High-resolution screen (exact size not specified, but likely 6.6\"+) with vibrant colors.  \n- **Camera:** Advanced multi-lens rear camera system (details not listed, but Huawei flagships typically emphasize low-light and zoom capabilities).  \n- **Performance:** Powered by a flagship-grade processor (likely Kirin or Snapdragon, though specifics depend on the global variant).  \n- **Storage:** 256GB internal storage (non-expandable).  \n- **Dual SIM:** Supports two SIM cards for flexibility.  \n- **Unlocked:** Works with compatible GSM carriers worldwide (may not support CDMA networks like Verizon).  \n\n### **Additional Notes:**  \n- **Price:** ${'$'}595.00 (may vary with promotions).  \n- **Seller:** Ships from **Sell Phone Basement LLC** with a 30-day return policy.  \n- **Protection Plans:** Optional 2-year accidental damage coverage (${'$'}159.99) or monthly subscription (${'$'}7.49/month).  \n\n### **Potential Drawbacks:**  \n- **Limited U.S. carrier compatibility** (check bands for your provider).  \n- **No Google Services** (Huawei devices use HarmonyOS/HMS instead of Google Play).  \n\nIdeal for users seeking a high-end Huawei device with global network support, though research carrier compatibility before purchase. Let me know if you'd like further details!",
  "fields" : "Here’s the extracted information you requested from the provided Amazon product page:\n\n### **Product Name:**  \n**Huawei P60 Pro Dual SIM 8GB + 256GB Global Model (MNA-LX9) Factory Unlocked Smartphone - Black**  \n\n### **Price:**  \n**${'$'}595.00**  \n\n### **Ratings:**  \n- **Overall Rating:** Not explicitly stated in the provided text, but the **2-Year Protection Plan** by Asurion has a **3.7/5** (based on 393 ratings).  \n- **Customer Reviews Breakdown:**  \n  - 5-star: 49%  \n  - 4-star: 8%  \n  - 3-star: 14%  \n  - 2-star: 12%  \n  - 1-star: 17%  \n\n### **Additional Details:**  \n- **Storage:** 256GB  \n- **RAM:** 8GB  \n- **Model:** MNA-LX9 (Global Version, Factory Unlocked)  \n- **Color:** Black  \n- **Seller:** Sell Phone Basement LLC  \n- **Availability:** Only 4 left in stock  \n- **Delivery Estimate:** May 6 - 8 (if ordered soon)  \n\n### **Protection Plans (Optional):**  \n1. **2-Year Protection Plan** – **${'$'}159.99** (3.7/5 rating)  \n2. **Monthly Mobile Accident Protection Plan** – **${'$'}7.49/month** (3.0/5 rating)  \n\nWould you like any additional details, such as specifications or seller policies?",
  "links" : "Here are all the links containing `/dp/` found on the page:\n\n1. `https://www.amazon.com/dp/B0C1H26C46#nic-po-expander-heading`  \n2. `https://www.amazon.com/dp/B0C1H26C46#productFactsDesktopExpander`  \n3. `https://www.amazon.com/dp/B0C1H26C46`  \n4. `https://www.amazon.com/dp/B0C1H26C46#`  \n5. `https://www.amazon.com/dp/B089MCQKD5/ref=dp_atch_dss_w_lm_B0C1H26C46_`  \n6. `https://www.amazon.com/dp/B088YS1F7W/ref=dp_atch_dss_w_lm_B0C1H26C46_`  \n7. `https://www.amazon.com/dp/B089MCQKD5/ref=psd_bb_lm1_B0C1H26C46_B089MCQKD5`  \n8. `https://www.amazon.com/dp/B088YS1F7W/ref=psd_bb_lm2_B0C1H26C46_B088YS1F7W`  \n9. `https://www.amazon.com/dp/B0C1H26C46#productDetails`  \n\nLet me know if you'd like further analysis or filtering of these links!",
  "xsqlResultSet" : null,
  "createTime" : "2025-05-02T08:13:44.580574300Z",
  "finishTime" : "2025-05-02T08:14:42.128567700Z",
  "status" : "OK",
  "pageStatus" : "OK"
}
        """.trimIndent()

        val markdown = service.convertResponseToMarkdown(response)
        println(markdown)
    }

    private fun verifyPromptRequestL2(request: PromptRequestL2) {
        assertTrue { request.url == "https://www.amazon.com/dp/B0C1H26C46" }
        assertEquals("https://www.amazon.com/dp/B0C1H26C46", request.url)
        assertNotNull(request.pageSummaryPrompt)
        assertNotNull(request.dataExtractionRules)
        assertNotNull(request.linkExtractionRules)
        assertNotNull(request.onPageReadyActions)
    }
}
