package io.nuvalence.cx.tools.cxtest.util

private fun getRegionalDFCXEndpoint(agentPath: String): String {
    val base = "dialogflow.googleapis.com:443"
    val (_, _, _, location) = agentPath.split("/")
    return if (location == "global") {
        base
    } else {
        "${location}-${base}"
    }
}

enum class PROPERTIES(private val value: String) {
    ORCHESTRATION_MODE("orchestrationMode"),
    MATCHING_MODE("matchingMode"),
    MATCHING_RATIO("matchingRatio") {
        override fun get(): String {
            return super.get().takeIf { prop -> !prop.isNullOrEmpty() } ?: "80"
        }
    },
    CREDENTIALS_URL("credentialsUrl"),
    AGENT_PATH("agentPath"),
    SPREADSHEET_ID("spreadsheetId"),
    DFCX_ENDPOINT("dfcxEndpoint") {
        override fun get(): String {
            return super.get().takeIf { prop -> !prop.isNullOrEmpty() } ?: getRegionalDFCXEndpoint(AGENT_PATH.get()!!)
        }
    };

    open fun get(): String? {
        return System.getProperty(value)
    }
}
