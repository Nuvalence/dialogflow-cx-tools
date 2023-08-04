package io.nuvalence.cx.tools.cxtest.extension

import io.nuvalence.cx.tools.cxtest.artifact.SpreadsheetArtifact
import io.nuvalence.cx.tools.cxtest.assertion.ContextAwareAssertionError
import io.nuvalence.cx.tools.cxtest.orchestrator.OrchestratedTestMap
import io.nuvalence.cx.tools.cxtest.sheetformat.SmokeFormatReader
import io.nuvalence.cx.tools.cxtest.util.PROPERTIES
import io.nuvalence.cx.tools.shared.SheetReader
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.api.extension.ExtensionContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.stream.Stream

class SmokeTestExtension () : ArgumentsProvider, BeforeAllCallback, AfterAllCallback {
    private val artifact = SpreadsheetArtifact()

    override fun beforeAll(context: ExtensionContext?) {
        println("Matching mode: ${PROPERTIES.MATCHING_MODE.get()}")

        val artifactSpreadsheetId = artifact.createArtifact("Smoke Spreadsheet ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                Date()
            )}")
        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("artifactSpreadsheetId", artifactSpreadsheetId)
        println("Created spreadsheet $artifactSpreadsheetId")
    }

    override fun afterAll(context: ExtensionContext?) {
        val errorList = context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("errors") as MutableList<ContextAwareAssertionError>
        val artifactSpreadsheetId = context.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.get("artifactSpreadsheetId") as String

        val outputColumn = SmokeFormatReader.cols[SmokeFormatReader.COMMENTS]!!

        val requestData = errorList.associate { e ->
            "${'A' + outputColumn}${e.sourceLocator}" to e.message!!
        }

        artifact.writeArtifact(artifactSpreadsheetId, requestData)
    }

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments?> {
        val testCases = SmokeFormatReader().listSheets("SMOKE_").map { sheet ->
            OrchestratedTestMap(SmokeFormatReader().read(sheet)).generatePairs()
        }.flatten()

        context?.root?.getStore(ExtensionContext.Namespace.GLOBAL)?.put("testCases", testCases)

        val testCaseArgs = testCases.map { (testScenario, executionPath) ->
             Arguments.of(testScenario, executionPath)
        }

        return Stream.of(*testCaseArgs.toTypedArray())
    }
}
