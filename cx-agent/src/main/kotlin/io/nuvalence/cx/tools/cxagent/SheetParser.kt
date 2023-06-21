package io.nuvalence.cx.tools.cxagent

import io.nuvalence.cx.tools.cxagent.model.*
import io.nuvalence.cx.tools.cxagent.model.Smalltalk.Companion.smalltalkDisplayName
import io.nuvalence.cx.tools.shared.SheetReader
import java.net.URL

/**
 * Parses a Google Sheet with a well-defined format and generates the agent model
 *
 * @param projectNumber Google project number
 * @param credentialsUrl URL to where the credentials.json file that grants access to the spreadsheet is located
 * @param spreadsheetId the long ID that uniquely identifies a Google Sheet (after the /d/ up to the next /)
 */
class SheetParser(
    private val projectNumber: String,
    private val credentialsUrl: URL,
    private val spreadsheetId: String
) {
    companion object {
        // Columns where to find information
        const val SMALLTALK_INTENT = 0
        const val SMALLTALK_TRAINING = 1
        const val SMALLTALK_FULFILLMENT = 2
        const val QUESTION_TOPIC = 0
        const val QUESTION_INTENT = 1
        const val QUESTION_TRAINING = 2
        const val QUESTION_FULFILLMENT = 3
        const val QUESTION_WEBSITE = 4
        const val QUESTION_FOLLOWUP = 5
        const val FAQ_PREFIX = "faq"
        val leadingDigitsRegex = Regex("^\\d[.)]\\s")
    }

    /**
     * Main function that creates the agent model
     */
    fun create(): CxAgentModel {
        val smalltalkTab = SheetReader(credentialsUrl, spreadsheetId, "Smalltalk").read()
        val smalltalks = extractSmalltalks(smalltalkTab)
        val smalltalkIntents = extractSmalltalkIntents(smalltalkTab)
        val questionsTab = SheetReader(credentialsUrl, spreadsheetId, "Questions").read()
        val intents = extractQuestionIntents(questionsTab)
        val flowGroups = extractQuestionFlows(questionsTab)
        return CxAgentModel(
            projectNumber = projectNumber,
            flowGroups = flowGroups,
            intents = smalltalkIntents + intents,
            smalltalks = Smalltalks(smalltalks)
        )
    }

    /**
     * Pulls smalltalks with fulfillments from the Smalltalk tab
     */
    private fun extractSmalltalks(rows: List<List<String>>) =
        rows.drop(1)
            .filter { row -> row.size > 2  }
            .map { row ->
            val intent = row[SMALLTALK_INTENT]
            val fulfillment = row[SMALLTALK_FULFILLMENT]
            Smalltalk(
                smalltalkName = intent,
                fulfillment= cleanupMessage(fulfillment)
            )
        }

    /**
     * Pulls intents from the Smalltalk tab
     */
    private fun extractSmalltalkIntents(rows: List<List<String>>) =
        rows.drop(1)
            .filter { row -> row[SMALLTALK_TRAINING].isNotEmpty() }
            .map { row ->
                val intent = row[SMALLTALK_INTENT]
                val phrases = phrases(row[SMALLTALK_TRAINING])
                Intent(
                    displayName = smalltalkDisplayName(intent),
                    trainingPhrases = phrases.map { phrase -> TrainingPhrase(parts = listOf(Part(phrase))) }
                )
        }

    /**
     * Extracts intents from the Questions tab
     */
    private fun extractQuestionIntents(rows: List<List<String>>) =
        rows.drop(1)
            .map { row ->
                val topic = row[QUESTION_TOPIC]
                val intent = row[QUESTION_INTENT]
                val phrases = phrases(row[QUESTION_TRAINING])
                Intent(
                    displayName = toDisplayName(FAQ_PREFIX, topic, intent),
                    trainingPhrases = phrases.map { phrase -> TrainingPhrase(parts = listOf(Part(phrase))) }
                )
            }

    /**
     * Maps the Questions tab to flows, including website and follow up when they exist
     */
    private fun extractQuestionFlows(rows: List<List<String>>) =
        rows.drop(1).map { row ->
            val topic = cleanUpString(row[QUESTION_TOPIC])
            val intent = cleanUpString(row[QUESTION_INTENT])
            val fulfillment = row[QUESTION_FULFILLMENT]
            val website = if (row.size > QUESTION_WEBSITE && row[QUESTION_WEBSITE].isNotEmpty()) row[QUESTION_WEBSITE] else null
            val followUp = extractFollowUp(row)
            Flow(
                flowKey = FlowKey(FAQ_PREFIX, topic, intent),
                fulfillment = cleanupMessage(fulfillment),
                webSite = website,
                followUp = followUp
            )
        }.groupBy { flow -> flow.flowKey.topicName  }.map { (topic, flows) ->
            FlowGroup(
                displayName = "$FAQ_PREFIX.$topic",
                flows = flows
            )
        }

    /**
     * Extracts intents - remove numbers followed by a "." from the beginning, since sometimes
     * people paste numbered lists of questoins.
     */
    private fun phrases(cell: String) =
        cell.split('\n')
            .filter { phrase -> phrase.isNotEmpty() }
            . map { phrase -> leadingDigitsRegex.replace(phrase, "") }

    /**
     * Extracts a followup reference if one exists
     */
    private fun extractFollowUp(row: List<String>) =
        if (row.size > QUESTION_FOLLOWUP && row[QUESTION_FOLLOWUP].isNotEmpty()) {
            val parts = row[QUESTION_FOLLOWUP].split('\n')
            if (parts.size != 2) error("Follow ups must include topic and intent separated by a newline: $parts")
            val prefix = if (parts[0] == "smalltalk") null else FAQ_PREFIX
            FlowKey(prefix = prefix, topicName = parts[0], flowName = parts[1])
        } else null
}
