package io.nuvalence.cx.tools.intentgen

import com.aallam.openai.client.OpenAI
import io.nuvalence.cx.tools.shared.SheetReader
import io.nuvalence.cx.tools.shared.SheetWriter
import io.nuvalence.cx.tools.shared.suggestions
import io.nuvalence.cx.tools.shared.variations
import java.net.URL

fun main(args: Array<String>) {
    if (args.size < 4)
        error("Must supply <spreadsheet id> <URL to credentials.json file> <ChatGPT API key> <ChatGPT variation question> <ChatGPT intent question>")

    val spreadsheetId = args[0]
    val credentialsUrl = URL(args[1])
    val apiKey = args[2]
    val variationQuestion = args[3]
    val intentQuestion = args[4]

    OpenAI(token = apiKey).use { client ->
        val questionsTab = SheetReader(credentialsUrl, spreadsheetId, "Questions").read()
        val originalQuestions = questionsTab.drop(1).map { it[2] }
        val answers = questionsTab.drop(1).map { it[3] }

        // Step 1 - generate variations for the (single) question.
        val questionVariations = variations(originalQuestions, variationQuestion, client)

        // Step 2 - using step 1 and the answer, generate the variations
        val intents = suggestions(
            answers,
            questionVariations,
            intentQuestion,
            client
        ).map { intent ->
            intent
                .split("\n")
                .filter { it.contains("?") }
                .joinToString("\n")
        }

        // Step 3 - merge the result
        val trainingPhrases = intents.zip(questionsTab.drop(1)) { newCell, row ->
            row.toMutableList().apply {
                this[2] = newCell
            }.toList()
        }

        // Step 4 - writ ethe new tab
        val sheetWriter = SheetWriter(credentialsUrl, spreadsheetId)
        sheetWriter.deleteTab("Questions - Generated")
        sheetWriter.addTab("Questions - Generated")
        sheetWriter.addDataToTab(
            "Questions - Generated",
            trainingPhrases,
            listOf("Topic", "Intent", "Training Phrases", "Response", "Website", "Follow-up"),
            listOf(150, 150, 400, 400, 150, 150)
        )
    }
}
