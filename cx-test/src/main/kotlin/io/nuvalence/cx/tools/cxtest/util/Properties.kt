package io.nuvalence.cx.tools.cxtest.util

import java.io.File
import java.io.FileInputStream
import java.net.URL
import javax.naming.ConfigurationException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMembers

class PropertyDelegate<T> : ReadWriteProperty<Any?, T> {
    private var _value: T? = null
    private var isSet = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException("${property.name} is not initialized.")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (isSet) {
            throw IllegalStateException("${property.name} has already been set.")
        }
        _value = value
        isSet = true
    }
}

class Properties {
    companion object {
        var CREDENTIALS_URL: URL by PropertyDelegate()
        var AGENT_PATH: String by PropertyDelegate()
        var SPREADSHEET_ID: String by PropertyDelegate()
        var DFCX_ENDPOINT: String by PropertyDelegate()
        var ORCHESTRATION_MODE: String by PropertyDelegate()
        var MATCHING_MODE: String by PropertyDelegate()
        var MATCHING_RATIO: Int by PropertyDelegate()
        var DFCX_TAG_FILTER: String by PropertyDelegate()

        private val defaultProperties = java.util.Properties()
        private lateinit var properties: java.util.Properties

        fun init(path: String) {
            PropertiesDefinition.values().forEach { propertyDefinition ->
                if (propertyDefinition.default?.isNotEmpty() == true) {
                    defaultProperties.setProperty(propertyDefinition.value, propertyDefinition.default)
                }
            }
            properties = java.util.Properties(defaultProperties)

            val file = File(path)
            val stream = FileInputStream(file)
            properties.load(stream)
            validateProps()
            setProps()
        }

        private fun validateProps () {
            val errors = PropertiesDefinition.values().fold(mutableListOf<String>()) { acc, entry ->
                if (entry.required && (properties.getProperty(entry.value) == "" || properties.getProperty(entry.value) == null)) {
                    acc.add("${entry.value} is a required property. Example value: ${entry.example}")
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
            val companionInstance = Properties::class.companionObjectInstance!!

            PropertiesDefinition.values().forEach { propDef ->
                val baseProperty = properties.getProperty(propDef.value)
                val fieldName = propDef.name
                val field = Companion::class.declaredMembers.find { it.name == fieldName } as? KMutableProperty1<*, *>
                    ?: throw IllegalArgumentException("No mutable property named $fieldName found")

                println("$fieldName : $baseProperty")

                field.setter.call(companionInstance,
                    when (propDef.type) {
                        String::class -> baseProperty
                        Int::class -> baseProperty.toInt()
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
                )
            }
        }
    }
}

enum class PropertiesDefinition(val value: String, val type: KClass<*>, val required: Boolean = false, val example: String, val default: String? = "") {
    CREDENTIALS_URL("credentialsUrl", URL::class,true, "file:///path/to/creds/file.json"),
    AGENT_PATH("agentPath", String::class, true, "projects/<projectName>/locations/<location>/agents/<agentId>"),
    SPREADSHEET_ID("spreadsheetId", String::class, true, "the final segment of the spreadsheet URL, e.g. \"asdf\" if your spreadsheet URL is https://docs.google.com/spreadsheets/d/asdf"),
    DFCX_ENDPOINT("dfcxEndpoint", String::class, false, "(<region>-)dialogflow.googleapis.com:443", "dialogflow.googleapis.com:443"),
    ORCHESTRATION_MODE("orchestrationMode", String::class, false, "[simple, comprehensive]", "simple"),
    MATCHING_MODE("matchingMode", String::class, false, "[normal, adaptive]", "normal"),
    MATCHING_RATIO("matchingRatio", Int::class, false, "Integer from 0-100", "80"),
    DFCX_TAG_FILTER("dfcxTagFilter", String::class, false, "Comma-delimited list of tags, e.g. \"#Tag1,#Tag2,#Tag3\", or \"ALL\"", "ALL");
}