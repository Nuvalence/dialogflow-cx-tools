package io.nuvalence.cx.tools.cxagent.model

import java.util.UUID

/**
 * Holds a more convenient representation of the Agent input spreadsheet.
 *
 * @param projectNumber The Google project number associated with the agent
 * @param flowGroups Flows grouped by category
 * @param intents Intents captured from the spreadsheet
 * @param smalltalks Smalltalk intents and messages captured from the spreadsheet
 */
data class CxAgentModel(
    val projectNumber: String,
    val flowGroups: List<FlowGroup>,
    val intents: List<Intent>,
    val smalltalks: Smalltalks
) {
    private val flowMap = flowGroups.flatMap { flowGroup ->
        flowGroup.flows.map {  flow ->
            flow.flowKey to flow
        }
    }.toMap()

    /**
     * Gets the flow associated with a key
     * @param flowKey the key
     */
    fun getFlow(flowKey: FlowKey) = flowMap[flowKey]

    /**
     * Generates a random UUID
     */
    fun getUuid() = UUID.randomUUID().toString()
}

/**
 * Converts (potentially "dirty") prefix/topic/intent into a DF page display name
 */
fun toDisplayName(prefix: String, topic: String, intent: String) = "${prefix}.${cleanUpString(topic)}.${cleanUpString(intent)}"

private val alphanumericOnly = Regex("[^A-Za-z0-9 ]")

/**
 * Remove all non-alphanumeric characters from a string
 */
fun cleanUpString(string: String) = alphanumericOnly.replace(string, "-").lowercase()

/**
 * Replaces newlines in a message with a \n and double quotes with single quotes, so it can
 * be safely used in JSON
 */
fun cleanupMessage(string: String) = string.replace("\n", "\\n").replace('"', '\'')
