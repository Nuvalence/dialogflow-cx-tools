package io.nuvalence.cx.tools.cxtest

import com.google.cloud.dialogflow.cx.v3beta1.ConversationTurn
import com.google.cloud.dialogflow.cx.v3beta1.QueryInput
import com.google.cloud.dialogflow.cx.v3beta1.TestCaseName
import com.google.cloud.dialogflow.cx.v3beta1.TextInput
import com.google.cloud.dialogflow.cx.v3beta1.UpdateTestCaseRequest
import com.google.protobuf.FieldMask
import io.nuvalence.cx.tools.cxtest.extension.TestBuilderTestExtension
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Tag("test")
@ExtendWith(TestBuilderTestExtension::class)
class TestBuilderSpec {
    fun parseJson(data: List<ConversationTurn>?) {
        for (i in data?.indices!!) {
            val turn = data[i];

            // User Input
            if (turn.hasUserInput()) {
                val userInput = turn.userInput
                if (userInput.hasInput()) {
                    val input = userInput.input
                    if (input.hasText()) {
                        val text = input.text
                        println("CALLER SAYS: ${text.text}")
                    }
                }
            }

            // Virtual Agent Output
            if (turn.hasVirtualAgentOutput()) {
                val virtualAgentOutput = turn.virtualAgentOutput
                if (virtualAgentOutput.textResponsesCount > 0) {
                    val textResponses = virtualAgentOutput.textResponsesList
                    for (j in 0 until textResponses.size) {
                        val response = textResponses[j]
                        if (response.textCount > 0) {
                            println("AGENT SAYS: ${response.textList}")
                        }
                    }
                }
                if (virtualAgentOutput.differencesCount > 0) {
                    val differences = virtualAgentOutput.differencesList

                    println(differences.toString())
                }
            }
        }
    }

    @Test
    fun `should update ssn`() {

        val testClient = TestBuilderTestExtension.testClient!!

        val testCase = testClient.getTestCase(
            TestCaseName.of(
                "dol-uisim-ccai-dev-app",
                "global",
                "997f91c8-6a5b-4852-9621-f933faa7fa13",
                "6cc5dc50-b60e-4826-b33e-4dc3c58a13be"
            ).toString()
        )

        println("Display name: ${testCase?.displayName}")
        println("Tags: ${testCase?.tagsList}")
        println("Notes: ${testCase?.notes}")

        val updatedTestCaseBuilder = testCase.toBuilder()!!
        testCase.testCaseConversationTurnsList?.forEachIndexed { index, turn ->
            if (index > 0 && testCase.testCaseConversationTurnsList!![index - 1].virtualAgentOutput.sessionParameters.fieldsMap["data-collection-type"]?.stringValue == "ssn" &&
                turn.userInput.input.text.text.matches(Regex(".*\\d{9}.*"))) {
                val updatedTurn = turn.toBuilder()
                    .setUserInput(turn.userInput?.toBuilder()!!.setInput(
                        QueryInput.newBuilder().setText(
                            TextInput.newBuilder().setText("987654321").build())
                    ).build()
                    )
                    /*
                    .setVirtualAgentOutput(originalStep.virtualAgentOutput?.toBuilder()!!.setSessionParameters(
                        originalStep.virtualAgentOutput.sessionParameters.toBuilder()!!.fieldsMap["ssn"] = "987654321"
                    ))
                    */
                    .build()

                updatedTestCaseBuilder.setTestCaseConversationTurns(index, updatedTurn)
            }
        }

        updatedTestCaseBuilder.clearTags()
        updatedTestCaseBuilder.addTags("#newTestTag")

        val updateRequest = UpdateTestCaseRequest.newBuilder()
            .setTestCase(updatedTestCaseBuilder.build())
            .setUpdateMask(FieldMask.newBuilder().addAllPaths(listOf("test_case_conversation_turns", "tags")).build())
            .build()

        testClient.updateTestCase(updateRequest)
    }
}

