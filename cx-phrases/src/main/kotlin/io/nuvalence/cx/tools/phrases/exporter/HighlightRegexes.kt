package io.nuvalence.cx.tools.phrases.exporter

const val LOOKAROUND_PATTERN = "(?<=^|\\s)(?![\\p{L}\\p{N}\\p{P}]+(?=\$|\\s))"
const val SYMBOL_PATTERN = "[\\p{L}\\p{N}]*[\\p{P}\\p{S}][\\p{L}\\p{N}\\p{P}\\p{S}]*"
const val UNDERSCORES_PATTERN = "[\\p{L}\\p{N}]*_+[\\p{L}\\p{N}\\p{P}\\p{S}]*"
const val REGEX_PATTERN = "(?=[^\\s]*(\\\\|\\[|\\]))[^\\s]*"

enum class HighlightRegexes (val pattern: Regex) {
    REFERENCE_PATTERN(Regex("\\\$([A-Za-z]+[\\w-]*\\.)+[A-Za-z]+[\\w-]*")),
    HTML_TAG_PATTERN(Regex("<[^>]+>")),
    ANNOTATED_FRAGMENT_PATTERN(Regex("]\\s*\\([^)]*\\)|\\[")),
    FRAGMENT_WITH_SYMBOL_PATTERN(Regex("$LOOKAROUND_PATTERN$SYMBOL_PATTERN|$UNDERSCORES_PATTERN|$REGEX_PATTERN"))
}