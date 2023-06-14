package io.nuvalence.cx.tools.cxagent

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import io.nuvalence.cx.tools.cxagent.model.CxAgentModel
import io.nuvalence.cx.tools.shared.zipDirectory
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * This class generates the different pieces of an agent based on the model. This class is bigger than I
 * would have hoped, but since it is pretty much following a sequence of steps, it's not that bad.
 *
 * @param path where to generate the agent. We do not delete the directory, since we don't know what may be there
 * @param model the model created from the spreadsheet
 */
class CxAgentGenerator(private val path: String, private val model: CxAgentModel) {
    private val freemarkerConfig = run {
        val cfg = Configuration(Configuration.VERSION_2_3_32)
        cfg.setClassLoaderForTemplateLoading((Configuration::class as Any).javaClass.classLoader, "/")
        cfg.defaultEncoding = "UTF-8"
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        cfg.logTemplateExceptions = false
        cfg.wrapUncheckedExceptions = true
        cfg.fallbackOnNullLoopVariable = false
        cfg.sqlDateAndTimeTimeZone = TimeZone.getDefault()
        cfg.autoFlush = true
        cfg
    }

    /**
     * Steps required to generate the agent. Order is important.
     */
    fun generate() {
        createDirectoryLayout()
        copyStaticFiles()
        generateAgent()
        generateIntents()
        generateTransitionRouteGroups()
        generateFaqFlows()
        generateSupportFlows()
        zipDirectory("$path/agent", "$path/agent.zip")
    }

    /**
     * Creates the target directory layout, so we don't have to worry about it later - all directories
     * will be already in place. This represents the "dynamic" part of the structure, i.e. the directories
     * that match the flows and the intents.
     */
    private fun createDirectoryLayout() {
        model.intents.forEach { intent ->
            File("$path/agent/intents/${intent.displayName}/trainingPhrases").mkdirs()
        }
        model.flowGroups.forEach { group ->
            File("$path/agent/flows/${group.displayName}/pages").mkdirs()
        }
    }

    /**
     * Copies all static files to the target. Those are the files that do not change, regardless of the
     * spreadsheet contents. We also delete hidden files, since we have a .gitkeep file under those
     * directories, as empty directories cannot be committed to git.
     */
    private fun copyStaticFiles() {
        val sourceUri = this.javaClass.classLoader.getResource("agent")?.toURI() ?: error("Could not find agent root directory")
        Paths.get(sourceUri).toFile().copyRecursively(File("$path/agent"))
        File("$path/agent").walk().forEach { file ->
            if (file.isHidden)
                file.delete()
        }
    }

    /**
     * Generates the main agent.json file
     */
    private fun generateAgent() {
        generate(model = model, templatePath = "templates/agent.ftl", targetFilePath = "$path/agent/agent.json")
    }

    /**
     * Generates all the intents
     */
    private fun generateIntents() {
        model.intents.forEach { intent ->
            // Generate the intents/<intent-name>.json file
            generate(
                model = intent,
                templatePath = "templates/intents/intent.ftl",
                targetFilePath = "$path/agent/intents/${intent.displayName}/${intent.displayName}.json"
            )
            // Generate the intents/<intent-name>/trainingPhrases/en.json file
            generate(
                model = intent,
                templatePath = "templates/intents/trainingPhrases/language.ftl",
                targetFilePath = "$path/agent/intents/${intent.displayName}/trainingPhrases/en.json"
            )
        }

    }

    /**
     * Generates the rg-faq transition route group
     */
    private fun generateTransitionRouteGroups() {
        generate(
            model = mapOf(
                "name" to UUID.randomUUID().toString(),
                "displayName" to "faq",
                "flowGroups" to model.flowGroups
            ),
            templatePath = "templates/flows/flow.routing/transitionRouteGroups/transitionRouteGroups.ftl",
            targetFilePath = "$path/agent/flows/flow.routing/transitionRouteGroups/rg-faq.json"
        )
    }

    /**
     * Generates the FAQ flows and their pages.
     */
    private fun generateFaqFlows() {
        model.flowGroups.forEach { flowGroup ->
            // generate <flowGroup.displayName> JSON file
            val groupModel = mapOf("model" to model, "flowGroup" to flowGroup)
            generate(
                model = groupModel,
                templatePath = "templates/flows/flowGroup.ftl",
                targetFilePath = "$path/agent/flows/${flowGroup.displayName}/${flowGroup.displayName}.json"
            )
            flowGroup.flows.forEach { flow ->
                val flowModel = mapOf("model" to model, "flow" to flow)
                // generate <flowGroup.displayName>/pages/<flow.displayName> JSON file
                generate(
                    model = flowModel,
                    templatePath = "templates/flows/pages/flow.ftl",
                    targetFilePath = "$path/agent/flows/${flowGroup.displayName}/pages/${flow.flowKey.displayName}.json"
                )
            }
        }
    }

    /**
     * Generates a gallimaufry of flows that support the FAQ flows.
     */
    private fun generateSupportFlows() {
        // These are trivial flows that follow the same pattern, so they can share the same template files
        listOf("confirmation", "end", "redirect-agent", "redirect-main-menu", "unable-to-help", "what-else").forEach { flow ->
            generate(
                model = mapOf("model" to model, "displayName" to flow),
                templatePath = "templates/flows/trivialFlow.ftl",
                targetFilePath = "$path/agent/flows/flow.$flow/flow.${flow}.json"
            )
            generate(
                model = mapOf(
                    "model" to model,
                    "displayName" to flow,
                    "fulfillment" to model.smalltalks.fulfillment(flow)
                ) + mapOf("parameters" to trivialPageParameters[flow]) + trivialPageForward[flow]!!,
                templatePath = "templates/flows/pages/trivialFlowPage.ftl",
                targetFilePath = "$path/agent/flows/flow.$flow/pages/$flow-message.json"
            )
        }
        // This creates the flow.repeat
        generate(
            model = mapOf("model" to model),
            templatePath = "templates/flows/flow.repeat.ftl",
            targetFilePath = "$path/agent/flows/flow.repeat/flow.repeat.json"
        )
        // This creates the flow.routing
        generate(
            model = mapOf("model" to model),
            templatePath = "templates/flows/flow.routing/flow.routing.ftl",
            targetFilePath = "$path/agent/flows/flow.routing/flow.routing.json"
        )
        // These are the remaining support flows, they also follow a similar pattern
        generateFlowAndPages("flow.help", listOf("help", "more-help"))
        generateFlowAndPages("flow.website", listOf("has-web-site", "no-web-site"))
        generateFlowAndPages("Default Start Flow", listOf("welcome-message"))
    }

    /**
     * Helper function to avoid copy and paste code
     */
    private fun generateFlowAndPages(flowName: String, pages: List<String>) {
        generate(
            model = mapOf("model" to model),
            templatePath = "templates/flows/$flowName.ftl",
            targetFilePath = "$path/agent/flows/$flowName/$flowName.json"
        )
        pages.forEach { page ->
            generate(
                model = mapOf("model" to model),
                templatePath = "templates/flows/pages/$page.ftl",
                targetFilePath = "$path/agent/flows/$flowName/pages/$page.json"
            )
        }
    }

    private val trivialPageParameters = mapOf(
        "confirmation" to mapOf("question-type" to "confirmation", "user-request-fwd" to "\$session.params.user-request", "web-site-fwd" to "\$session.params.user-request"),
        "what-else" to mapOf("question-type" to "faq-question")
    )
    private val trivialPageForward = mapOf(
        "confirmation" to (mapOf("targetFlow" to "flow.routing")),
        "end" to mapOf("targetPage" to "End Flow"),
        "redirect-agent" to mapOf("targetFlow" to "flow.what-else"),
        "redirect-main-menu" to mapOf("targetFlow" to "flow.what-else"),
        "unable-to-help" to mapOf("targetFlow" to "flow.what-else"),
        "what-else" to mapOf("targetFlow" to "flow.routing")
    )

    /**
     * Helper function to generate a file based on a template
     */
    private fun generate(model: Any, templatePath: String, targetFilePath: String) {
        val template = freemarkerConfig.getTemplate(templatePath)
        File(targetFilePath).printWriter().use { pw ->
            template.process(model, pw)
        }
    }
}