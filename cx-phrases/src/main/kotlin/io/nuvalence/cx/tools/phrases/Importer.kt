package io.nuvalence.cx.tools.phrases

import java.io.File
import java.net.URL
import kotlin.io.path.Path
import kotlin.io.path.createDirectory

/**
 * Reads data from a spreadsheet and create the agent with updated messages. "args" represent the
 * arguments passed to the main program. Proper access must be granted to the account running the
 * import (see Authorizer class for details).
 *
 * The expected parameters are as follows:
 *
 * import, the first parameter
 * spreadsheet ID, it appears in the spreadsheet URL, see SheetReader for details.
 * path to the source agent root directory, i.e. where the agent.json file is. This is the directory
 *      that was used to export the agent.
 * path to the target directory where the updated agent should be created. Note that the contents of
 *      this directory will be deleted as part of the import process.
 * URL to the credentials.json file is (see Authorizer class for details).
 */
fun import(args: Array<String>) {
    if (args.size < 5) {
        error("Required parameters after operation: <spreadsheet ID> <path to source agent root directory> <path to target agent root directory (will be deleted)> <URL to credentials.json>")
    }
    val spreadsheetId = args[1]
    val sourceAgentPath = args[2]
    val targetAgentPath = args[3]
    val url = URL(args[4])

    // Clear target directory and copy all files there, so we can update them
    File(targetAgentPath).deleteRecursively()
    Path(targetAgentPath).createDirectory()
    File(sourceAgentPath).copyRecursively(
        target = File(targetAgentPath),
        overwrite = true
    )

    val translationAgent = SheetPhrasesExtractor(url, spreadsheetId).processSheet()
    val agentLanguageMerger = AgentLanguageMerger(translationAgent, targetAgentPath)
    agentLanguageMerger.process()
}