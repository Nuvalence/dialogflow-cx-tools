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
    val intents = translationAgent.flattenIntents()
    val intentHeaders = listOf("Intent Name") + translationAgent.allLanguages
    val intentHeaderOffset = 1
    val intentHighlightIndices = highlightForTrainingPhrases(intents, intentHeaderOffset)
    sheetWriter.deleteTab(PhraseType.Intents.title)
    sheetWriter.addTab(PhraseType.Intents.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Intents.title,
        intents,
        intentHeaders,
        listOf(200) + MutableList(translationAgent.allLanguages.size) { 500 },
        intentHeaderOffset,
        intentHighlightIndices,
        HighlightPreset.BLUE_BOLD
    )

    Thread.sleep(60000)  // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add entities to Entities tab
    val entities = translationAgent.flattenEntities()
    val entityHeaders = listOf("Entity Type", "Value") + translationAgent.allLanguages
    val entityHeaderOffset = 2
    val entityHighlightIndices = highlightForEntities(entities, translationAgent, entityHeaderOffset)
    sheetWriter.deleteTab(PhraseType.Entities.title)
    sheetWriter.addTab(PhraseType.Entities.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Entities.title,
        entities,
        entityHeaders,
        listOf(200, 200) + MutableList(translationAgent.allLanguages.size) { 500 },
        entityHeaderOffset,
        entityHighlightIndices,
        HighlightPreset.BLUE_BOLD
    )

    Thread.sleep(60000) // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add transition fulfillments to Transitions tab
    val flowTransitions = translationAgent.flattenFlows()
    val flowTransitionHeaders = listOf("Flow Name", "Page", "Transition Type", "Value", "Type", "Channel") + translationAgent.allLanguages
    val flowTransitionHeaderOffset = 6
    val flowTransitionHighlightIndices = highlightForTransitions(flowTransitions, flowTransitionHeaderOffset)
    sheetWriter.deleteTab(PhraseType.Flows.title)
    sheetWriter.addTab(PhraseType.Flows.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Flows.title,
        flowTransitions,
        flowTransitionHeaders,
        listOf(200, 200, 100, 300) + MutableList(translationAgent.allLanguages.size) { 500 },
        flowTransitionHeaderOffset,
        flowTransitionHighlightIndices,
        HighlightPreset.BLUE_BOLD
    )

    Thread.sleep(60000) // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add normal page fulfillments to Fulfillments tab
    val pages = translationAgent.flattenPages()
    val pageHeaders = listOf("Flow Name", "Page Name", "Type", "Channel") + translationAgent.allLanguages
    val pageHeaderOffset = 4
    val pageHighlightIndices = highlightForFulfillments(pages, pageHeaderOffset)
    sheetWriter.deleteTab(PhraseType.Pages.title)
    sheetWriter.addTab(PhraseType.Pages.title)
    sheetWriter.addFormattedDataToTab(
        PhraseType.Pages.title,
        pages,
        pageHeaders,
        listOf(200, 250, 150) + MutableList(translationAgent.allLanguages.size) { 500 },
        pageHeaderOffset,
        pageHighlightIndices,
        HighlightPreset.BLUE_BOLD
    )
}

fun highlightAnnotatedFragments(string: String) : List<Pair<Int, Int>>{
    val highlightIndices = mutableListOf<Pair<Int, Int>>()
    val pattern = Regex("]\\s*\\([^)]*\\)|\\[")
    pattern.findAll(string).forEach { matchResult ->
        highlightIndices.add(matchResult.range.first to matchResult.range.last)
    }

    return highlightIndices
}

fun highlightFragmentsWithSymbols(string: String) : List<Pair<Int, Int>>{
    val highlightIndices = mutableListOf<Pair<Int, Int>>()
    val lookarounds = "(?<=^|\\s)(?![\\p{L}\\p{N}\\p{P}]+(?=\$|\\s))"
    val symbolsPattern = "[\\p{L}\\p{N}]*[\\p{P}\\p{S}][\\p{L}\\p{N}\\p{P}\\p{S}]*"
    val underscoresPattern = "[\\p{L}\\p{N}]*_+[\\p{L}\\p{N}\\p{P}\\p{S}]*"
    val regexPattern = "(?=[^\\s]*(\\\\|\\[|\\]))[^\\s]*"
    val pattern = Regex("$lookarounds$symbolsPattern|$underscoresPattern|$regexPattern")
    pattern.findAll(string).forEach { matchResult ->
        highlightIndices.add(matchResult.range.first to matchResult.range.last)
    }

    return highlightIndices
}

private fun parseFunction(string: String, expression: MatchResult) : Pair<Int, Int>? {
    var inSingleQuotes = false
    var inDoubleQuotes = false
    var level = 1

    val start = expression.range.first
    val end = expression.range.last
    val rest = string.substring(expression.range.last + 2)

    for ((index, char) in rest.withIndex()) {
        if (level == 0) {
            return start to (end + index + 1)
        }

        if (char == '\'' && !inDoubleQuotes) {
            inSingleQuotes = !inSingleQuotes
        }
        if (char == '"' && !inSingleQuotes) {
            inDoubleQuotes = !inDoubleQuotes
        }

        if (inSingleQuotes || inDoubleQuotes) {
            continue
        }

        when (char) {
            '(' -> level++
            ')' -> level--
        }
    }

    return null
}

private fun getFunctionCallIndices(string: String, expressions: List<MatchResult>) : List<Pair<Int, Int>> {
    val fullFunctions = expressions.fold(mutableListOf<Pair<Int, Int>>()) { acc, expression ->
        val functionPair = parseFunction(string, expression)
        if (functionPair != null) {
            acc.add(functionPair)
        }
        acc
    }

    var lastFunctionEnd = 0
    val filteredFunctions = fullFunctions.filter { indexPair ->
        if (indexPair.first > lastFunctionEnd) {
            lastFunctionEnd = indexPair.second
            return@filter true
        }
        return@filter false
    }

    return filteredFunctions
}

fun highlightReferences(string: String) : List<Pair<Int, Int>>{
    val pattern = Regex("\\\$(\\w*\\.)+[\\w-]+")
    val rawExpressions = pattern.findAll(string).toList()
    val (functionCallExpressions, basicExpressions) = rawExpressions.partition { it.range.last < string.length-2 && string[it.range.last+1] == '(' }
    val functionCallIndices = getFunctionCallIndices(string, functionCallExpressions)

    val basicExpressionIndices = basicExpressions.filterNot { expression ->
        functionCallIndices.any { (start, end) ->
            expression.range.first in start..end
        }
    }.map { expression -> expression.range.first to expression.range.last }

    return basicExpressionIndices + functionCallIndices
}

fun highlightForEntities(entities: List<List<String>>, translationAgent: TranslationAgent, offset: Int) : List<List<List<Pair<Int, Int>>>> {
    return entities.map { row ->
        val entity = translationAgent.getEntity(PhrasePath(listOf(row[0], row[1])))
        val type = entity?.messagesByLanguage?.values?.toList()?.get(0)?.get(0)?.type
        val rowIndices = mutableListOf<List<Pair<Int, Int>>>()
        for (i in offset until row.size) {
            if (type == "KIND_REGEXP" || type == "KIND_LIST") {
                rowIndices.add(listOf(0 to row[i].length))
            } else {
                rowIndices.add(highlightFragmentsWithSymbols(row[i]))
            }
        }

        rowIndices
    }
}

fun highlightForTransitions(transitions: List<List<String>>, offset: Int) : List<List<List<Pair<Int, Int>>>> {
    return transitions.map { row ->
        val rowIndices = mutableListOf<List<Pair<Int, Int>>>()
        for (i in offset until row.size) {
            val pairs = (highlightFragmentsWithSymbols(row[i]) + highlightReferences(row[i]))

            val result = pairs.fold(mutableListOf<Pair<Int, Int>>()) { acc, pair ->
                val superstringPair = acc.find { it.first <= pair.first && it.second >= pair.second }
                val substringPair = acc.find { it.first <= pair.first && it.second >= pair.second }
                val overlapping = acc.find { (it.first <= pair.second) && (it.second >= pair.first) }

                if (superstringPair != null) {
                    acc
                } else if (substringPair != null) {
                    acc.remove(substringPair)
                    acc.add(pair)
                    acc
                } else if (overlapping != null) {
                    val newPair = overlapping.first.coerceAtMost(pair.first) to overlapping.second.coerceAtLeast(pair.second)
                    acc.remove(overlapping)
                    acc.add(newPair)
                    acc
                } else {
                    acc.add(pair)
                    acc
                }
            }

            rowIndices.add(result)
        }

        rowIndices
    }
}

fun highlightForFulfillments (fulfillments: List<List<String>>, offset: Int) : List<List<List<Pair<Int, Int>>>> {
    return fulfillments.map { row ->
        val rowIndices = mutableListOf<List<Pair<Int, Int>>>()
        for (i in offset until row.size) {
            rowIndices.add(highlightReferences(row[i]))
        }

        rowIndices
    }
}

fun highlightForTrainingPhrases (trainingPhrases: List<List<String>>, offset: Int) : List<List<List<Pair<Int, Int>>>> {
    return trainingPhrases.map { row ->
        val rowIndices = mutableListOf<List<Pair<Int, Int>>>()
        for (i in offset until row.size) {
            rowIndices.add(highlightAnnotatedFragments(row[i]))
        }

        rowIndices
    }
}
