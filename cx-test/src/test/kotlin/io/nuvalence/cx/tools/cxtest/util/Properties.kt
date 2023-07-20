package io.nuvalence.cx.tools.cxtest.util

enum class PROPERTIES(private val value: String) {
    ORCHESTRATION_MODE("orchestrationMode"),
    MATCHING_MODE("matchingMode"),
    MATCHING_RATIO("matchingRatio") {
        override fun get(): String {
            return super.get().takeIf { prop -> prop.isNotEmpty() } ?: "80"
        }
    },
    CREDENTIALS_URL("credentialsUrl"),
    AGENT_PATH("agentPath"),
    SPREADSHEET_ID("spreadsheetId"),
    DFCX_ENDPOINT("dfcxEndpoint") {
        override fun get(): String {
            return super.get().takeIf { prop -> prop.isNotEmpty() } ?: "us-east1-dialogflow.googleapis.com:443"
        }
    };

    open fun get(): String {
        return System.getProperty(value)
    }
}
