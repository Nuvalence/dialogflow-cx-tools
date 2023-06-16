package io.nuvalence.cx.tools.suggestions

import com.aallam.openai.client.OpenAI
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.suggestions
import java.net.URL

fun main(args: Array<String>) {
    if (args.size < 4)
        error("Must supply <spreadsheet id> <URL to credentials.json file> <ChatGPT API key> <ChatGPT question>")

    val spreadsheetId = args[0]
    val credentialsUrl = URL(args[1])
    val apiKey = args[2]
    val gptQuestion = args[3]
    OpenAI(token = apiKey).use { client ->
        val questionsTab = SheetReader(credentialsUrl, spreadsheetId, "Questions").read()
        val newQuestions = suggestions(questionsTab.drop(1).map { it[3] }, gptQuestion, client)
        val newQuestionsTab = newQuestions.zip(questionsTab.drop(1)) { newCell, row ->
            row.toMutableList().apply {
                this[2] = newCell
            }.toList()
        }
        val sheetWriter = SheetWriter(credentialsUrl, spreadsheetId)
        sheetWriter.deleteTab("Questions - Suggestions")
        sheetWriter.addTab("Questions - Suggestions")
        sheetWriter.addDataToTab(
            "Questions - Suggestions",
            newQuestionsTab,
            listOf("Topic", "Intent", "Training Phrases", "Response", "Website", "Follow-up"),
            listOf(150, 150, 400, 400, 150, 150)
        )
    }
}
