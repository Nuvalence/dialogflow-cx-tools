package io.nuvalence.cx.tools.phrases.exporter

// Conditions: must be a string AND must be included in exclusions list

enum class DFCXSystemFunctionParameterHighlightExclusion (private val value: String, val argsExcluded: List<Boolean>) {
    IF("\$sys.func.IF", listOf(false, true, true)),
    JOIN("\$sys.func.JOIN", listOf(true, true, true));

    companion object {
        infix fun from(value: String?): DFCXSystemFunctionParameterHighlightExclusion? =
            DFCXSystemFunctionParameterHighlightExclusion.values().firstOrNull { highlightExclusion -> highlightExclusion.value == value }
    }

    enum class ArgType {
        STRING,
        FUNCTION,
        OTHER
    }
}

typealias ArgType = DFCXSystemFunctionParameterHighlightExclusion.ArgType