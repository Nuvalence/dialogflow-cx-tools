package io.nuvalence.cx.tools.phrases.exporter

/**
 * Enumerates functions to be further processed for translation highlight exclusions, specifically of strings enclosed within
 * the enumerated functions that should still be translated
 *
 * @param value the function prefix
 * @param argsExcluded the list of expected arguments to process, specified by position
 */
enum class DFCXSystemFunctionParameterHighlightExclusion (private val value: String, val argsExcluded: List<Boolean>) {
    IF("\$sys.func.IF", listOf(false, true, true)),
    JOIN("\$sys.func.JOIN", listOf(true, true, true));

    companion object {
        /**
         * Returns highlight exclusion information given a known Dialogflow CX function prefix,
         * or null if the function prefix is not explicitly listed in the enum
         *
         * @param value the string representation of a Dialogflow CX function prefix (e.g. $sys.func.SOME_FUNCTION)
         * @return the corresponding highlight exclusion enum value, or null if not found
         */

        infix fun from(value: String?): DFCXSystemFunctionParameterHighlightExclusion? =
            DFCXSystemFunctionParameterHighlightExclusion.values().firstOrNull { highlightExclusion -> highlightExclusion.value == value }
    }

    /**
     * Enumerates types of arguments within a given function.
     * Only strings and functions warrant further processing -- all other types (e.g. numbers, booleans, non-function expressions) are ignored.
     */
    enum class ArgType {
        STRING,
        FUNCTION,
        OTHER
    }
}

typealias ArgType = DFCXSystemFunctionParameterHighlightExclusion.ArgType