package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.ql.TestBase
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach

class LLMFunctionsTest : TestBase() {
    val sql = """
  select
      llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
      dom_uri(dom) as url
  from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
        """

    @BeforeEach
    fun setup() {
        Assumptions.assumeTrue(ChatModelFactory.isModelConfigured(session.unmodifiedConfig))
    }

    /**
     * Test [LLMFunctions.extract]
     * */
    @org.junit.jupiter.api.Test
    fun `Test extract with field descriptions`() {
        query(sql)
    }

    /**
     * Test [LLMFunctions.extract]
     * */
    @org.junit.jupiter.api.Test
    fun `Test extract with field descriptions and convert to json`() {
        query(sql) {
            val json = ResultSetUtils.toJson(it, prettyPrinting = true)
            println(json)
        }
    }
}
