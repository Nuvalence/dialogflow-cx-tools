package io.nuvalence.cx.tools.phrases

import io.nuvalence.cx.tools.shared.HighlightPreset
import io.nuvalence.cx.tools.shared.SheetWriter
import java.net.URL

/**
 * Exports an agent to a spreadsheet. "args" represent the parameters passed to the main
 * program. Note that the exporter will delete the tabs where it publishes the data, but
 * it will retain any other tabs in the spreadsheet. Proper access must be granted to the
 * account running the export (see Authorizer class for details).
 *
 * The expected arguments are as follows:
 *
 * export, the first parameter
 * spreadsheet ID, it appears in the spreadsheet URL, see SheetReader for details.
 * path to the source agent root directory, i.e. where the agent.json file is.
 * URL to the credentials.json file is (see Authorizer class for details).
 *
 */
fun export(args: Array<String>) {
    if (args.size < 4) {
        error("Required parameters after operation: <spreadsheet ID (will delete the intents/flows/pages tabs)> <path to source agent root directory> <URL to credentials.json>")
    }
    val spreadsheetId = args[1]
    val agentPath = args[2]
    val url = URL(args[3])

    val translationAgent = AgentPhrasesExtractor(agentPath).process()
    val sheetWriter = SheetWriter(url, spreadsheetId)

    // add intent training phrases to Training Phrases tab
    sheetWriter.deleteTab(PhraseType.Intents.title)
    sheetWriter.addTab(PhraseType.Intents.title)
    sheetWriter.addDataToTab(
        PhraseType.Intents.title,
        translationAgent.flattenIntents(),
        listOf("Intent Name") + translationAgent.allLanguages,
        listOf(200) + MutableList(translationAgent.allLanguages.size) { 500 }
    )

    Thread.sleep(60000)  // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add entities to Entities tab
    val entities = translationAgent.flattenEntities()
    val entityHeaders = listOf("Entity Type", "Value") + translationAgent.allLanguages
    sheetWriter.deleteTab(PhraseType.Entities.title)
    sheetWriter.addTab(PhraseType.Entities.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Entities.title,
        entities,
        entityHeaders,
        listOf(200, 200) + MutableList(translationAgent.allLanguages.size) { 500 },
        (entityHeaders.size downTo 2).toList(),
        ::highlightEntity,
        HighlightPreset.BLUE_BOLD.getHighlightFormat()
    )

    Thread.sleep(60000) // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add transition fulfillments to Transitions tab
    val flowTransitions = translationAgent.flattenFlows()
    val flowTransitionHeaders = listOf("Flow Name", "Page", "Transition Type", "Value", "Type", "Channel") + translationAgent.allLanguages
    sheetWriter.deleteTab(PhraseType.Flows.title)
    sheetWriter.addTab(PhraseType.Flows.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Flows.title,
        flowTransitions,
        flowTransitionHeaders,
        listOf(200, 200, 100, 300) + MutableList(translationAgent.allLanguages.size) { 500 },
        (flowTransitionHeaders.size downTo 6).toList(),
        ::highlightTransition,
        HighlightPreset.BLUE_BOLD.getHighlightFormat()
    )

    Thread.sleep(60000) // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add normal page fulfillments to Fulfillments tab
    val pages = translationAgent.flattenPages()
    val pageHeaders = listOf("Flow Name", "Page Name", "Type", "Channel") + translationAgent.allLanguages
    sheetWriter.deleteTab(PhraseType.Pages.title)
    sheetWriter.addTab(PhraseType.Pages.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Pages.title,
        pages,
        pageHeaders,
        listOf(200, 250, 150) + MutableList(translationAgent.allLanguages.size) { 500 },
        (pageHeaders.size downTo 4).toList(),
        ::highlightFulfillment,
        HighlightPreset.BLUE_BOLD.getHighlightFormat()
    )
}

fun highlightEntity(string: String) : List<Pair<Int, Int>>{
    val highlightIndices = mutableListOf<Pair<Int, Int>>()
    val pattern = Regex("]\\s*\\([^)]*\\)|\\[")
    pattern.findAll(string).forEach { matchResult ->
        highlightIndices.add(matchResult.range.first to matchResult.range.last)
    }

    return highlightIndices
}

fun highlightTransition(string: String) : List<Pair<Int, Int>>{
    val highlightIndices = mutableListOf<Pair<Int, Int>>()
    val pattern = Regex("(?<=^|\\s)(?![\\p{L}\\p{N}]+(?=\$|\\s))[\\p{L}\\p{N}]*[\\p{P}\\p{S}][\\p{L}\\p{N}\\p{P}\\p{S}]*")
    pattern.findAll(string).forEach { matchResult ->
        highlightIndices.add(matchResult.range.first to matchResult.range.last)
    }

    return highlightIndices
}

private fun getFunctionCallIndices(string: String, expressions: List<MatchResult>) : List<Pair<Int, Int>> {
    fun isFunctionExpression (expression: MatchResult) : Boolean {
        return expression.range.last < string.length-2 && string[expression.range.last+1] == '('
    }

    var lastFunctionEnd = 0;
    return expressions.filter(::isFunctionExpression).fold(mutableListOf<Pair<Int, Int>>()) { acc, expression ->
        var inSingleQuotes = false
        var inDoubleQuotes = false
        var level = 1

        val start = expression.range.last+2
        val rest = string.substring(expression.range.last+2)

        fun toggleQuotes (char: Char) : Boolean {
            return !((char == '\'' && !inDoubleQuotes) || (char == '"' && !inSingleQuotes))
        }

        loop@ for ((index, char) in rest.withIndex()) {
            if (level == 0) {
                acc.add(start to (start + index))
                break@loop
            }

            inSingleQuotes = toggleQuotes(char)
            inDoubleQuotes = toggleQuotes(char)

            val isInQuotes = inSingleQuotes || inDoubleQuotes

            if (isInQuotes) {
                continue@loop
            } else when (char) {
                '(' -> level++
                ')' -> level--
            }
        }

        return acc
    }.filter { indexPair ->
        if (indexPair.second > lastFunctionEnd) {
            lastFunctionEnd = indexPair.second
            return@filter true
        }
        return@filter false
    }
}

fun highlightFulfillment(string: String) : List<Pair<Int, Int>>{
    val pattern = Regex("\\\$(\\p{L}*\\.)*\\p{L}+")
    val expressions = pattern.findAll(string).toList()
    val functionCallIndices = getFunctionCallIndices(string, expressions)

    return expressions.map { expression -> expression.range.first to expression.range.last } + functionCallIndices
}
