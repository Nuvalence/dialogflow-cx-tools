package io.nuvalence.cx.tools.cxagent

import java.net.URL

fun main(args: Array<String>) {
    if (args.size < 4) {
        error("Required parameters: <spreadsheet ID> <path where to generate the agent> <URL to credentials.json>")
    }
    val spreadsheetId = args.first()
    val projectId = args[1]
    val agentPath = args[2]
    val url = URL(args[3])

    val agentModel = SheetParser(projectNumber = projectId, credentialsUrl = url, spreadsheetId = spreadsheetId).create()
    CxAgentGenerator(agentPath, agentModel).generate()
}