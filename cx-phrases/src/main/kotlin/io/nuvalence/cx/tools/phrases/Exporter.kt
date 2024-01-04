package io.nuvalence.cx.tools.phrases

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

    // add entities to Entities tab
    sheetWriter.deleteTab(PhraseType.Entities.title)
    sheetWriter.addTab(PhraseType.Entities.title)
    sheetWriter.addDataToTab(
        PhraseType.Entities.title,
        translationAgent.flattenEntities(),
        listOf("Entity Type", "Value") + translationAgent.allLanguages,
        listOf(200, 200) + MutableList(translationAgent.allLanguages.size) { 500 }
    )

    // add transition fulfillments to Transitions tab
    sheetWriter.deleteTab(PhraseType.Flows.title)
    sheetWriter.addTab(PhraseType.Flows.title)
    sheetWriter.addDataToTab(
        PhraseType.Flows.title,
        translationAgent.flattenFlows(),
        listOf("Flow Name", "Page", "Type", "Value", "Channel") + translationAgent.allLanguages,
        listOf(200, 200, 100, 300) + MutableList(translationAgent.allLanguages.size) { 500 }
    )

    // add normal page fulfillments to Fulfillments tab
    sheetWriter.deleteTab(PhraseType.Pages.title)
    sheetWriter.addTab(PhraseType.Pages.title)
    sheetWriter.addDataToTab(
        PhraseType.Pages.title,
        translationAgent.flattenPages(),
        listOf("Flow Name", "Page Name", "Type", "Channel") + translationAgent.allLanguages,
        listOf(200, 250, 150) + MutableList(translationAgent.allLanguages.size) { 500 }
    )
}