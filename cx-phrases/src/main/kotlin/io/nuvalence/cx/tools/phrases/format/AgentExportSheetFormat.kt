package io.nuvalence.cx.tools.phrases.format

import io.nuvalence.cx.tools.phrases.util.PhraseType

const val INTENT_NAME_COLUMN_WIDTH = 200
const val ENTITY_TYPE_COLUMN_WIDTH = 200
const val ENTITY_VALUE_COLUMN_WIDTH = 200
const val TRANSITION_FLOW_NAME_COLUMN_WIDTH = 200
const val TRANSITION_PAGE_COLUMN_WIDTH = 200
const val TRANSITION_TYPE_COLUMN_WIDTH = 100
const val TRANSITION_VALUE_COLUMN_WIDTH = 300
const val TRANSITION_ELEMENT_TYPE_COLUMN_WIDTH = 200
const val TRANSITION_CHANNEL_COLUMN_WIDTH = 200
const val PAGE_FLOW_NAME_COLUMN_WIDTH = 200
const val PAGE_NAME_COLUMN_WIDTH = 250
const val PAGE_ELEMENT_TYPE_COLUMN_WIDTH = 150
const val PAGE_CHANNEL_COLUMN_WIDTH = 200

private fun createHeaders (headerWidthPairs: List<Pair<String, Int>>) : List<AgentExportSheetFormat.AgentExportHeader> {
    return headerWidthPairs.map { (a, b) -> AgentExportSheetFormat.AgentExportHeader(a, b) }
}

val TRAINING_PHRASE_COLUMNS = createHeaders(listOf("Intent Name" to INTENT_NAME_COLUMN_WIDTH))
val ENTITY_COLUMNS = createHeaders(listOf("Entity Type" to ENTITY_TYPE_COLUMN_WIDTH, "Value" to ENTITY_VALUE_COLUMN_WIDTH))
val TRANSITION_COLUMNS = createHeaders(
    listOf("Flow Name" to TRANSITION_FLOW_NAME_COLUMN_WIDTH,
        "Page" to TRANSITION_PAGE_COLUMN_WIDTH,
        "Transition Type" to TRANSITION_TYPE_COLUMN_WIDTH,
        "Value" to TRANSITION_VALUE_COLUMN_WIDTH,
        "Type" to TRANSITION_ELEMENT_TYPE_COLUMN_WIDTH,
        "Channel" to TRANSITION_CHANNEL_COLUMN_WIDTH)
)
val FULFILLMENT_COLUMNS = createHeaders(
    listOf("Flow Name" to PAGE_FLOW_NAME_COLUMN_WIDTH,
        "Page Name" to PAGE_NAME_COLUMN_WIDTH,
        "Type" to PAGE_ELEMENT_TYPE_COLUMN_WIDTH,
        "Channel" to PAGE_CHANNEL_COLUMN_WIDTH)
)

enum class AgentExportSheetFormat (val phraseType: PhraseType, val phrasePathLength: Int, val languageColumnOffset: Int, val initialHeaders: List<AgentExportHeader>) {
    TRAINING_PHRASES(PhraseType.Intents, 1, 0, TRAINING_PHRASE_COLUMNS),
    ENTITIES(PhraseType.Entities, 2, 0, ENTITY_COLUMNS),
    TRANSITIONS(PhraseType.Flows, 4, 2, TRANSITION_COLUMNS),
    FULFILLMENTS(PhraseType.Pages, 4, 0, FULFILLMENT_COLUMNS);

    fun getHeaders () : List<String> {
        return this.initialHeaders.map { it.headerName }
    }

    fun getColumnWidths () : List<Int> {
        return this.initialHeaders.map { it.columnWidth }
    }

    fun getTotalOffset (): Int {
        return this.phrasePathLength + this.languageColumnOffset
    }

    data class AgentExportHeader (val headerName: String, val columnWidth: Int)
}