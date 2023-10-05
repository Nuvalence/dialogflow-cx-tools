package io.nuvalence.cx.tools.cxtest.extension

import com.google.cloud.dialogflow.cx.v3beta1.TestCasesClient
import com.google.cloud.dialogflow.cx.v3beta1.TestCasesSettings
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TestBuilderTestExtension () : BeforeAllCallback, AfterAllCallback {
    companion object {
        var testClient: TestCasesClient? = null
    }

    override fun beforeAll(context: ExtensionContext?) {
        println("Agent: ${PROPERTIES.AGENT_PATH.get()}")
        println("Matching mode: ${PROPERTIES.MATCHING_MODE.get()}")

        testClient = TestCasesClient.create(
            TestCasesSettings.newBuilder()
                .setEndpoint(PROPERTIES.DFCX_ENDPOINT.get())
                .build())
    }

    override fun afterAll(context: ExtensionContext?) {
        testClient?.close()
    }
}
