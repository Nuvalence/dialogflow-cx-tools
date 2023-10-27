package gov.ny.dol.ui.ccai.dfcx.domain.util

import io.nuvalence.cx.tools.cxtestcore.PropertySet
import java.net.URL
import javax.naming.ConfigurationException
import kotlin.reflect.KClass

object CXTestSyncProperties : PropertySet {
    override val propertiesMap = mutableMapOf<String, Any?>()
    private lateinit var rawProperties: java.util.Properties

    override fun initialize(path: String) {
        val defaultProperties = java.util.Properties()
        CXTestSyncPropertiesDefinition.values().forEach { propertyDefinition ->
            if (propertyDefinition.default != null) {
                defaultProperties.setProperty(propertyDefinition.name, propertyDefinition.default)
            }
        }
        rawProperties = java.util.Properties(defaultProperties)
        val loader = Thread.currentThread().contextClassLoader
        val stream = loader.getResourceAsStream(path)
        rawProperties.load(stream)
        validateProps()
        setProps()
    }

    private fun validateProps () {
        val errors = CXTestSyncPropertiesDefinition.values().fold(mutableListOf<String>()) { acc, entry ->
            if (entry.required && (rawProperties.getProperty(entry.propertyName) == "" || rawProperties.getProperty(entry.propertyName) == null)) {
                acc.add("${entry.propertyName} is a required property. Example value: ${entry.example}")
            }
            acc
        }

        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                System.err.println(error)
            }
            throw ConfigurationException("Missing properties detected. Please supply the necessary properties before proceeding with execution.")
        }
    }

    private fun setProps() {
        CXTestSyncPropertiesDefinition.values().forEach { propDef ->
            val baseProperty = rawProperties.getProperty(propDef.propertyName)

            propertiesMap[propDef.propertyName] = when (propDef.type) {
                String::class -> baseProperty
                Integer::class -> baseProperty.toInt()
                Long::class -> baseProperty.toLong()
                Short::class -> baseProperty.toShort()
                Byte::class -> baseProperty.toByte()
                Double::class -> baseProperty.toDouble()
                Float::class -> baseProperty.toFloat()
                Boolean::class -> baseProperty.toBoolean()
                Char::class -> baseProperty[0]
                else -> propDef.type.constructors.find { it.parameters.size == 1 && it.parameters[0].type.classifier == String::class }
                    ?.call(baseProperty)
                    ?: baseProperty
            }
        }
    }
}

enum class CXTestSyncPropertiesDefinition(val propertyName: String, val type: KClass<*>, val required: Boolean = false, val example: String, val default: String? = "") {
    CREDENTIALS_URL("credentialsUrl", URL::class,true, "file:///path/to/creds/file.json"),
    AGENT_PATH("agentPath", String::class, true, "projects/<projectName>/locations/<location>/agents/<agentId>"),
    SPREADSHEET_ID("spreadsheetId", String::class, true, "the final segment of the spreadsheet URL, e.g. \"asdf\" if your spreadsheet URL is https://docs.google.com/spreadsheets/d/asdf"),
    DFCX_ENDPOINT("dfcxEndpoint", String::class, false, "(<region>-)dialogflow.googleapis.com:443"),
    PROCESSOR_CLASS_NAME("processorClassName", String::class, false, "fully qualified domain name of processor class, e.g. \"gov.ny.dol.ui.ccai.dfcx.domain.sync.processor.SpreadsheetProcessor\"", "gov.ny.dol.ui.ccai.dfcx.domain.sync.processor.SpreadsheetProcessor");
}