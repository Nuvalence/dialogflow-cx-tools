package io.nuvalence.cx.tools.phrases.exporter

import io.nuvalence.cx.tools.phrases.format.AgentExportSheetFormat
import io.nuvalence.cx.tools.phrases.util.PhrasePath
import io.nuvalence.cx.tools.phrases.util.TranslationAgent
import io.nuvalence.cx.tools.shared.format.TextHighlightPreset
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.format.CellFormatPreset
import io.nuvalence.cx.tools.shared.format.GridRangePreset
import io.nuvalence.cx.tools.shared.format.SheetPropertyPreset
import java.net.URL

const val PHRASE_COLUMN_WIDTH = 500

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
/*
    // add intent training phrases to Training Phrases tab
    val intents = translationAgent.flattenIntents()
    val intentSheetFormat = AgentExportSheetFormat.TRAINING_PHRASES
    val intentSheetName = intentSheetFormat.phraseType.title
    val intentHeaders = intentSheetFormat.getHeaders() + translationAgent.allLanguages
    val intentColumnWidths = intentSheetFormat.getColumnWidths() + MutableList(translationAgent.allLanguages.size) { PHRASE_COLUMN_WIDTH }
    val intentHeaderOffset = intentSheetFormat.getTotalOffset()
    val intentHighlightIndices = highlightForTrainingPhrases(intents, intentHeaderOffset)
    val intentMissingTranslations = highlightMissingTranslations(intents, intentHeaderOffset, 1, intentSheetFormat.phrasePathLength)
    sheetWriter.deleteTab(intentSheetName)
    sheetWriter.addTab(intentSheetName)
    sheetWriter.addFormattedDataToTab(
        intentSheetName,
        intents,
        intentHeaders,
        intentColumnWidths,
        intentHeaderOffset,
        intentHighlightIndices,
        TextHighlightPreset.BLUE_BOLD
    )
    sheetWriter.applyCellFormatUpdates(intentSheetName, CellFormatPreset.HEADER, GridRangePreset.FIRST_N_ROWS, 1)
    if (intentMissingTranslations.isNotEmpty()) {
        sheetWriter.applyCellFormatUpdates(intentSheetName, CellFormatPreset.FG_RED, intentMissingTranslations)
    }
    sheetWriter.applySheetPropertyUpdates(intentSheetName, SheetPropertyPreset.FREEZE_N_ROWS, 1)
    sheetWriter.applySheetPropertyUpdates(intentSheetName, SheetPropertyPreset.FREEZE_N_COLUMNS, intentSheetFormat.phrasePathLength)

    Thread.sleep(60000)  // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add entities to Entities tab
    val entities = translationAgent.flattenEntities()
    val entitySheetFormat = AgentExportSheetFormat.ENTITIES
    val entitySheetName = entitySheetFormat.phraseType.title
    val entityHeaders = entitySheetFormat.getHeaders() + translationAgent.allLanguages
    val entityColumnWidths = entitySheetFormat.getColumnWidths() + MutableList(translationAgent.allLanguages.size) { PHRASE_COLUMN_WIDTH }
    val entityHeaderOffset = entitySheetFormat.getTotalOffset()
    val entityHighlightIndices = highlightForEntities(entities, translationAgent, entityHeaderOffset)
    val entityMissingTranslations = highlightMissingTranslations(entities, entityHeaderOffset, 1, entitySheetFormat.phrasePathLength)
    sheetWriter.deleteTab(entitySheetName)
    sheetWriter.addTab(entitySheetName)
    sheetWriter.addFormattedDataToTab(
        entitySheetName,
        entities,
        entityHeaders,
        entityColumnWidths,
        entityHeaderOffset,
        entityHighlightIndices,
        TextHighlightPreset.BLUE_BOLD
    )
    sheetWriter.applyCellFormatUpdates(entitySheetName, CellFormatPreset.HEADER, GridRangePreset.FIRST_N_ROWS, 1)
    if (entityMissingTranslations.isNotEmpty()) {
        sheetWriter.applyCellFormatUpdates(entitySheetName, CellFormatPreset.FG_RED, entityMissingTranslations)
    }
    sheetWriter.applySheetPropertyUpdates(entitySheetName, SheetPropertyPreset.FREEZE_N_ROWS, 1)
    sheetWriter.applySheetPropertyUpdates(entitySheetName, SheetPropertyPreset.FREEZE_N_COLUMNS, entitySheetFormat.phrasePathLength)

    Thread.sleep(60000) // Sleep added here due to Google Sheets quota limits of 300 operations per minute

    // add transition fulfillments to Transitions tab
    val flowTransitions = translationAgent.flattenFlows()
    val flowTransitionSheetFormat = AgentExportSheetFormat.TRANSITIONS
    val flowTransitionSheetName = flowTransitionSheetFormat.phraseType.title
    val flowTransitionHeaders = flowTransitionSheetFormat.getHeaders() + translationAgent.allLanguages
    val flowTransitionColumnWidths = flowTransitionSheetFormat.getColumnWidths() + MutableList(translationAgent.allLanguages.size) { PHRASE_COLUMN_WIDTH }
    val flowTransitionHeaderOffset = flowTransitionSheetFormat.getTotalOffset()
    val flowTransitionHighlightIndices = highlightForTransitions(flowTransitions, flowTransitionHeaderOffset)
    val flowTransitionMissingTranslations = highlightMissingTranslations(flowTransitions, flowTransitionHeaderOffset, 1, flowTransitionSheetFormat.phrasePathLength)
    sheetWriter.deleteTab(flowTransitionSheetName)
    sheetWriter.addTab(flowTransitionSheetName)
    sheetWriter.addFormattedDataToTab(
        flowTransitionSheetName,
        flowTransitions,
        flowTransitionHeaders,
        flowTransitionColumnWidths,
        flowTransitionHeaderOffset,
        flowTransitionHighlightIndices,
        TextHighlightPreset.BLUE_BOLD
    )
    sheetWriter.applyCellFormatUpdates(flowTransitionSheetName, CellFormatPreset.HEADER, GridRangePreset.FIRST_N_ROWS, 1)
    if (flowTransitionMissingTranslations.isNotEmpty()) {
        sheetWriter.applyCellFormatUpdates(
            flowTransitionSheetName,
            CellFormatPreset.FG_RED,
            flowTransitionMissingTranslations
        )
    }
    sheetWriter.applySheetPropertyUpdates(flowTransitionSheetName, SheetPropertyPreset.FREEZE_N_ROWS, 1)
    sheetWriter.applySheetPropertyUpdates(flowTransitionSheetName, SheetPropertyPreset.FREEZE_N_COLUMNS, flowTransitionSheetFormat.phrasePathLength)

    Thread.sleep(60000) // Sleep added here due to Google Sheets quota limits of 300 operations per minute
*/
    // add normal page fulfillments to Fulfillments tab
    val pages = translationAgent.flattenPages()
    val pageSheetFormat = AgentExportSheetFormat.FULFILLMENTS
    val pageSheetName = pageSheetFormat.phraseType.title
    val pageHeaders = pageSheetFormat.getHeaders() + translationAgent.allLanguages
    val pageColumnWidths = pageSheetFormat.getColumnWidths() + MutableList(translationAgent.allLanguages.size) { PHRASE_COLUMN_WIDTH }
    val pageHeaderOffset = pageSheetFormat.getTotalOffset()
    val pageHighlightIndices = highlightForFulfillments(pages, pageHeaderOffset)
    val pageMissingTranslations = highlightMissingTranslations(pages, pageHeaderOffset, 1, pageSheetFormat.phrasePathLength)

    sheetWriter.deleteTab(pageSheetName)
    sheetWriter.addTab(pageSheetName)
    sheetWriter.addFormattedDataToTab(
        pageSheetName,
        pages,
        pageHeaders,
        pageColumnWidths,
        pageHeaderOffset,
        pageHighlightIndices,
        TextHighlightPreset.BLUE_BOLD
    )
    sheetWriter.applyCellFormatUpdates(pageSheetName, CellFormatPreset.HEADER, GridRangePreset.FIRST_N_ROWS, 1)
    if (pageMissingTranslations.isNotEmpty()) {
        sheetWriter.applyCellFormatUpdates(pageSheetName, CellFormatPreset.FG_RED, pageMissingTranslations)
    }
    sheetWriter.applySheetPropertyUpdates(pageSheetName, SheetPropertyPreset.FREEZE_N_ROWS, 1)
    sheetWriter.applySheetPropertyUpdates(pageSheetName, SheetPropertyPreset.FREEZE_N_COLUMNS, pageSheetFormat.phrasePathLength)

}

fun highlightMissingTranslations(table: List<List<String>>, columnOffset: Int, headerHeight: Int, pathWidth: Int) : List<String> {
    val coordinates = mutableListOf<String>()
    table.forEachIndexed { rowIndex, row ->
        val hasMissingTranslation = row.drop(columnOffset).any { cell -> cell.isEmpty() }

        if (hasMissingTranslation) {
            for (colIndex in 0 until pathWidth) {
                coordinates += "${'A' + colIndex}${rowIndex + headerHeight + 1}"
            }
        }
    }

    return coordinates
}

/**
 * Highlights annotated fragments in a given string.
 * These are represented by square brackets, parentheses, and the contents of the parentheses in a given string.
 * Used primarily for training phrases.
 *
 * @param string the string to be processed
 * @return the list of index ranges where the given string should be highlighted
 */
fun highlightAnnotatedFragments(string: String) : List<Pair<Int, Int>>{
    val highlightIndices = mutableListOf<Pair<Int, Int>>()
    val pattern = Regex("]\\s*\\([^)]*\\)|\\[")
    pattern.findAll(string).forEach { matchResult ->
        highlightIndices.add(matchResult.range.first to matchResult.range.last)
    }

    return highlightIndices
}

/**
 * Highlights fragments that contain symbols in a given string.
 * Used primarily for entities with value/synonym maps.
 *
 * @param string the string to be processed
 * @return the list of index ranges where the given string should be highlighted
 */
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
    val substringSkip = 1 // substring end is exclusive
    val restSkip = 1 // start at immediately following parenthesis

    val start = expression.range.first
    val end = expression.range.last + substringSkip
    val rest = string.substring(end + restSkip)

    var level = 1
    var inSingleQuotes = false
    var inDoubleQuotes = false

    for ((index, char) in rest.withIndex()) {
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
            ')' -> {
                level--
                if (level == 0) {
                    return start to (end + index + restSkip)
                }
            }
        }
    }

    return null
}

/**
 * Highlights function calls within a given string.
 * Used primarily for transition and page fulfillments.
 * Logic is part of highlightReferences.
 *
 * @param string the string to be processed
 * @param expressions a list of regex match results for expressions to additionally check for functions
 * @return the list of index ranges, including the contents of the function calls, where the given string should be highlighted
 */
private fun getFunctionCallsIndices(string: String, expressions: List<MatchResult>) : List<Pair<Int, Int>> {
    val fullFunctions = expressions.fold(mutableListOf<Pair<Int, Int>>()) { acc, expression ->
        val functionPair = parseFunction(string, expression)
        if (functionPair != null) {
            acc.add(functionPair)
        }
        acc
    }

    var lastFunctionEnd = 0
    val topLevelFunctions = fullFunctions.filter { indexPair ->
        val isNewFunction = indexPair.first > lastFunctionEnd
        if (isNewFunction) {
            lastFunctionEnd = indexPair.second
        }
        isNewFunction
    }

    return topLevelFunctions
}

fun getExclusionProcessedStringIndices(string: String, stringIndices: List<Pair<Int, Int>>) : List<Pair<Int, Int>> {
    // for each fragment
    val exclusions = stringIndices.map {
        val pattern = Regex("\\\$(\\w*\\.)+[\\w-]+")
        val rawExpressions = pattern.findAll(string, it.first).toList().filter { expression -> expression.range.first < it.second }
        val (functionCallExpressions, basicExpressions) = rawExpressions.partition { expression -> expression.range.last < string.length-2 && string[expression.range.last+1] == '(' }
        val functionCallsIndices = getFunctionCallsIndices(string, functionCallExpressions)
        val functionHighlightExclusionIndices = getHighlightExclusionIndices(string, functionCallsIndices)

        val basicExpressionIndices = basicExpressions.filterNot { expression ->
            functionCallsIndices.any { (start, end) ->
                expression.range.first in start..end
            }
        }.map { expression -> expression.range.first to expression.range.last }

        val exclusionIndices = mutableListOf<Pair<Int, Int>>()
        val exclusionNegations = basicExpressionIndices + functionHighlightExclusionIndices

        if (exclusionNegations.isNotEmpty()) {
            exclusionIndices.add(it.first to exclusionNegations[0].first - 1)
            exclusionNegations.dropLast(1).forEachIndexed { index, exclusionNegation ->
                val newFirst = exclusionNegation.second + 1
                val newSecond = exclusionNegations[index+1].first
                if (newFirst != newSecond) exclusionIndices.add(newFirst to newSecond)
            }
            if (exclusionNegations.last().second + 1 != it.second) exclusionIndices.add(exclusionNegations.last().second + 1 to it.second)
        } else {
            if (string.startsWith("Please hold while I transfer you to an agent to speak about your")) {
                println("No exclusion negations detected for ${string.substring(it.first, it.second+1).replace(" ", "<space>")}")
            }
            exclusionIndices.add(it)
        }

        exclusionIndices
    }.flatten().sortedBy { it.first }

    if (string.startsWith("Please hold while I transfer you to an agent to speak about your")) {
        println(string)
        println("Initial string list:")
        println(stringIndices)
        println(stringIndices.joinToString("\n") { string.substring(it.first, it.second+1) })
        println("String exclusions list: $exclusions")
    }

    return exclusions
}

fun getHighlightExclusionIndices(string: String, functionCallsIndices: List<Pair<Int, Int>>) : List<Pair<Int, Int>> {
    // for each top level function call
    // > identify functions
    val functionNames = functionCallsIndices.map { string.substring(string.indexOf('$', it.first), string.indexOf('(', it.first)) }
    // > recursively process each function
    val exclusions = functionNames.zip(functionCallsIndices).map { (functionName, functionCallIndices) ->
        // separate parameters by commas not enclosed in quotes or parentheses
        val start = functionCallIndices.first + functionName.length + 1
        val end = functionCallIndices.second
        var level = 0
        var inSingleQuotes = false
        var inDoubleQuotes = false
        var runningParameter = ""
        var runningParameterStart = start

        val functionParametersIndices = mutableListOf<Pair<Int, Int>>()
        for (i in start.. end) {
            val char = string[i]
            if (char == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes
            }
            if (char == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes
            }
            if (char == '(') {
                level += 1
            }
            if (char == ')') {
                level -= 1
            }

            if (runningParameter.isEmpty() && char == ' ') {
                runningParameterStart++
                continue
            }

            if (inSingleQuotes || inDoubleQuotes || level > 0) {
                runningParameter += char
                continue
            }

            if (char == ',' || i == end) {
                functionParametersIndices += runningParameterStart to i-1
                runningParameterStart = i+1
                runningParameter = ""
            } else {
                runningParameter += char
            }
        }
        /*
        if (string.startsWith("Please hold while I transfer you to an agent to speak about your")) {
            println("Functions:")
            println(functionCallsIndices.joinToString("\n") { string.substring(it.first, it.second+1)})
            println("Parameters found:")
            println(functionParametersIndices.joinToString("\n") { string.substring(it.first, it.second+1) })
        }

         */


        // classify each parameter type
        val parameterTypes = functionParametersIndices.map { indices ->
            val functionParameter = string.substring(indices.first, indices.second+1).trim()
            when (functionParameter.startsWith("\"")) {
                true -> ArgType.STRING
                false -> {
                    when (functionParameter.startsWith("$") && functionParameter.contains("(")) {
                        true -> ArgType.FUNCTION
                        false -> ArgType.OTHER
                    }
                }
            }
        }

        val functionFunctionParameters = functionParametersIndices.filterIndexed { index, _ ->
            parameterTypes[index] == ArgType.FUNCTION
        }
        val nestedFunctionExclusions = getHighlightExclusionIndices(string, functionFunctionParameters)

        val argsExcluded = DFCXSystemFunctionParameterHighlightExclusion.from(functionName)?.argsExcluded
        val stringExclusions = getExclusionProcessedStringIndices(string, functionParametersIndices.filterIndexed { index, _ ->
            parameterTypes[index] == ArgType.STRING && argsExcluded != null && argsExcluded[index]
        })

        nestedFunctionExclusions + stringExclusions
    }.flatten().sortedBy { it.first }

    /*
    if (string.startsWith("Please hold while I transfer you to an agent to speak about your")) {
        println("Function exclusions list: $exclusions")
    }

     */

    return exclusions
}

/**
 * Highlights references (parameters or function calls) within a given string.
 * References start with $ and have its fragments delimited by ".". Function calls are additionally followed by parentheses.
 * Used primarily for transition and page fulfillments.
 *
 * @param string the string to be processed
 * @return the list of index ranges where the given string should be highlighted
 */
fun highlightReferences(string: String) : List<Pair<Int, Int>>{
    val pattern = Regex("\\\$(\\w*\\.)+[\\w-]+")
    val rawExpressions = pattern.findAll(string).toList()
    val (functionCallExpressions, basicExpressions) = rawExpressions.partition { it.range.last < string.length-2 && string[it.range.last+1] == '(' }
    val functionCallsIndices = getFunctionCallsIndices(string, functionCallExpressions)
    val processedFunctionCallsIndices = if (functionCallsIndices.isNotEmpty()) {
        val highlightExclusions = getHighlightExclusionIndices(string, functionCallsIndices)

        if (string.startsWith("Please hold while I transfer you to an agent to speak about your")) {
            println("Final exclusions list: $highlightExclusions")
            println("[${highlightExclusions.joinToString("|"){ string.substring(it.first, it.second+1) }}]")
        }

        val newFunctionCallsIndices = mutableListOf<Pair<Int,Int>>()
        functionCallsIndices.forEach {
            val enclosedExclusions = highlightExclusions.filter { exclusion -> it.first <= exclusion.first && it.second >= exclusion.second }
            if (enclosedExclusions.isNotEmpty()) {
                newFunctionCallsIndices.add(it.first to enclosedExclusions[0].first)
                enclosedExclusions.dropLast(1).forEachIndexed { index, exclusion ->
                    val newFirst = exclusion.second
                    val newSecond = enclosedExclusions[index+1].first
                    if (newFirst != newSecond) newFunctionCallsIndices.add(newFirst to newSecond)
                }
                if (enclosedExclusions.last().second != it.second) newFunctionCallsIndices.add(enclosedExclusions.last().second to it.second)
            }
        }

        when(newFunctionCallsIndices.isEmpty()) {
            true -> functionCallsIndices
            false -> newFunctionCallsIndices
        }
    } else functionCallsIndices

    if (string.startsWith("Please hold while I transfer you to an agent to speak about your")) {
        println("Final runs: $processedFunctionCallsIndices")
    }

    // anything not inside a top-level function call
    val basicExpressionIndices = basicExpressions.filterNot { expression ->
        functionCallsIndices.any { (start, end) ->
            expression.range.first in start..end
        }
    }.map { expression -> expression.range.first to expression.range.last }

    return basicExpressionIndices + processedFunctionCallsIndices
}

fun highlightForEntities(entities: List<List<String>>, translationAgent: TranslationAgent, offset: Int) : List<List<List<Pair<Int, Int>>>> {
    return entities.map { row ->
        val entity = translationAgent.getEntity(PhrasePath(listOf(row[0], row[1])))
        val type = entity?.messagesByLanguage?.values?.toList()?.get(0)?.get(0)?.type
        val rowIndices = mutableListOf<List<Pair<Int, Int>>>()
        for (i in offset until row.size) {
            // if the entity is configured as regex or a composite of other entities, highlight the whole string
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
            rowIndices.add(highlightReferences(row[i]))
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
