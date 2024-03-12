package io.nuvalence.cx.tools.cxagent.generator.model

import java.util.*

/**
 * Represents a group flows that belong to the same category
 *
 * @param name the group name (misnomer, it's the group UUID)
 * @param displayName the actual, human-readable name
 * @param flows the flows that belong to this group
 */
data class FlowGroup(
    val name: String = UUID.randomUUID().toString(),
    val displayName: String,
    val flows: List<Flow>
) {
    /**
     * Escapes the display name, so it can be used in a DF expression
     */
    fun escapedDisplayName() = displayName.replace(".", "\\\\.")
}


/**
 * Key representing a flow
 *
 * @param prefix optional prefix to be used for the key (e.g. "faq")
 * @param topicName the name of the topic to which this flow belongs
 * @param flowName the name of the flow within the topic
 * @param displayName the "calculated" display name for this flow
 */
data class FlowKey(
    val prefix: String?,
    val topicName: String,
    val flowName: String,
    val displayName: String = flowKeyDisplayName(prefix, topicName, flowName)
) {
    companion object {
        /**
         * Converts the key to a usable display name
         */
        fun flowKeyDisplayName(prefix: String?, topicName: String, flowName: String) = "${if (prefix == null) "" else "$prefix."}${topicName}.${flowName}"    }
}


/**
 * Represents a flow
 *
 * @param flowKey the key associated with the flow
 * @param name  the flow name (misnomer, it's the flow UUID)
 * @param fulfillment the fulfillment message associated with the flow
 * @param webSite optional URL associated with the flow
 * @param followUp optional reference to another flow that contains the answer to a follow up question
 */
data class Flow(
    val flowKey: FlowKey,
    val name: String = UUID.randomUUID().toString(),
    val fulfillment: String,
    val webSite: String?,
    val followUp: FlowKey?
)
