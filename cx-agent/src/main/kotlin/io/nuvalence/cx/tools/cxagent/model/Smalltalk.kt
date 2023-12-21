package io.nuvalence.cx.tools.cxagent.model

import io.nuvalence.cx.tools.cxagent.model.SmalltalkInstance.*
import java.util.*

/**
 * Captures a smalltalk from the corresponding spreadsheet tab
 *
 * @param smalltalkName the (real) name of the smalltalk (e.g. "redirect-agent")
 * @param displayName the DF display name
 * @param name the smalltalk name (misnomer, it's the UUID)
 * @param fulfillment fulfillment associated with this smalltalk
 */
data class Smalltalk(
    val smalltalkName: String,
    val displayName: String = smalltalkDisplayName(smalltalkName),
    val name: String = UUID.randomUUID().toString(),
    val fulfillment: String
) {
    companion object {
        /**
         * Standard smalltalk display name
         */
        fun smalltalkDisplayName(name: String) = "generic.smalltalk.$name"
    }
}

/**
 * Collection of all smalltalks. Note that there is a well known set of smalltalks -
 * they all must be present in the spreadsheet, and they have fixed names, since they
 * are directly referenced by the generator.
 *
 * @param smalltalks the collection of all smalltalks
 */
data class Smalltalks(
    val smalltalks: List<Smalltalk>
) {
    private val smalltalkMap = smalltalks.associate { smalltalk ->
        fromKey(smalltalk.smalltalkName) to smalltalk
    }

    val defaultEventHandlers = mapOf(
        "sys.no-input-default" to formatEventFulfillment(NO_INPUT_DEFAULT),
        "sys.no-match-default" to formatEventFulfillment(NO_MATCH_DEFAULT)
    )

    val allEventHandlers = mapOf(
        "sys.no-input-1" to formatEventFulfillment(NO_INPUT_1),
        "sys.no-input-2" to formatEventFulfillment(NO_INPUT_2),
        "sys.no-input-3" to formatEventFulfillment(NO_INPUT_3),
        "sys.no-match-1" to formatEventFulfillment(NO_MATCH_1),
        "sys.no-match-2" to formatEventFulfillment(NO_MATCH_2),
        "sys.no-match-3" to formatEventFulfillment(NO_MATCH_3)
    ) + defaultEventHandlers


    /**
     * Returns the smalltalk corresponding to the instance
     * @param instance which smalltalk to retrieve
     */
    operator fun get(instance: SmalltalkInstance) = smalltalkMap[instance] ?: error("No smalltalk entry for $instance")

    fun get(instance: String) = smalltalkMap[fromKey(instance)] ?: error("No smalltalk entry for $instance")

    /**
     * Returns the fulfillment associated with this smalltalk. Note that not all smalltalks have fulfillment, some
     * only have intents associated with thiem.
     * @param key String key associated with the smalltalk
     */
    fun fulfillment(key: String) = smalltalkMap[fromKey(key)]?.fulfillment ?: error("Smalltalk $key does not exist")

    /**
     * Formats an event fulfillment, breaking it into an array of comma-separated strings. This is only used
     * for event smalltalks (no-match and no-input)
     * @param event the event smalltalk for which we want the fulfillment
     */
    private fun formatEventFulfillment(event: SmalltalkInstance) =
        get(event).fulfillment.split("\\n").joinToString("\" , \"", "\"", "\"")

    /**
     * Given a string key, retrieves the smalltalk
     * @param key the string key (must match the SmalltalkInstance key)
     */
    private fun fromKey(key: String) = SmalltalkInstance.values().find { it.key == key }
        ?: error("Cannot find SmalltalkInstance: $key")
}

/**
 * The closed list of smalltalks the generator uses
 */
enum class SmalltalkInstance(val key: String) {
    WELCOME_MESSAGE("Welcome Message"),
    NO_INPUT_DEFAULT("no-input-default"),
    NO_INPUT_1("no-input-1"),
    NO_INPUT_2("no-input-2"),
    NO_INPUT_3("no-input-3"),
    NO_MATCH_DEFAULT("no-match-default"),
    NO_MATCH_1("no-match-1"),
    NO_MATCH_2("no-match-2"),
    NO_MATCH_3("no-match-3"),
    CONFIRMATION("confirmation"),
    UNABLE_TO_HELP("unable-to-help"),
    WHAT_ELSE("what-else"),
    HOW_CAN_I_HELP("how-can-I-help"),
    HELP("help"),
    MORE_HELP("more-help"),
    WEBSITE("website"),
    WEBSITE_MISSING("website-missing"),
    REPEAT("repeat"),
    REDIRECT_AGENT("redirect-agent"),
    REDIRECT_MAIN_MENU("redirect-main-menu"),
    END("end"),
    NO("no"),
    YES("yes");
}
