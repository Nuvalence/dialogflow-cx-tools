package io.nuvalence.cx.tools.phrases

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.util.*

// Collection of functions that process snippets of JSON elements


/**
 * Given a language code and a list of training phrases, create a JSON array
 * containing those training phrases in the structure Dialogflow expects.
 */
fun intentLanguage(languageCode: String, phrases: List<String>?): JsonArray {
    val trainingPhrasesArray = JsonArray()
    phrases?.forEach { phrase ->
        val trainingPhrase = JsonObject()
        val parts = processIntentText(phrase)
        trainingPhrase.addProperty("id", UUID.randomUUID().toString())
        trainingPhrase.add("parts", parts)
        trainingPhrase.addProperty("repeatCount", 1)
        trainingPhrase.addProperty("languageCode", languageCode)
        trainingPhrasesArray.add(trainingPhrase)
    }

    return trainingPhrasesArray
}

/**
 * Since we capture parameters as [text](parameterId), this function extracts the
 * references to those parameters and converts them back to the format expected
 * by Dialogflow - a list of texts that may contain a reference to a parameter id.
 */
fun processIntentText(toProcess: String): JsonArray {
    val pattern = Regex("\\[(.*?)\\]\\((.*?)\\)") // Matches the [xxx](yyy) pattern
    var lastEndIndex = 0
    val parts = JsonArray()

    pattern.findAll(toProcess).forEach { matchResult ->
        // Process the text before the first match
        val text = toProcess.substring(lastEndIndex, matchResult.range.first)
        if (text.isNotEmpty()) {
            parts.add(createIntentPart(text))
        }
        // Process the matches
        val parameterText = matchResult.groupValues[1]
        val parameterId = matchResult.groupValues[2]
        parts.add(createIntentPart(parameterText, parameterId))

        lastEndIndex = matchResult.range.last + 1
    }
    // Now process the text after the last match
    val text = toProcess.substring(lastEndIndex).trim()
    if (text.isNotEmpty())
        parts.add(createIntentPart(text))
    return parts
}

/**
 * Create a JSON object with a text attribute and, if a parameter is
 * supplied, a parameterId attribute.
 */
fun createIntentPart(text: String, parameter: String? = null): JsonObject {
    val part = JsonObject()
    part.addProperty("text", text)
    if (parameter != null)
        part.addProperty("parameterId", parameter)
    else
        part.addProperty("auto", true)
    return part
}

/**
 * Create an array with messages for the given language/phrases. For events, the list of
 * messages for each language should become an array of messages; for flows, they should
 * be combined as a newline separated string and returned an array with a single element.
 *
 * @param singleString whether to return the phrases as a single string or array of strings
 * @param phrases map associating a language to a list of phrases
 */
fun languagePhrasesToJson_NEW(singleString: Boolean, phrases: Map<String, List<Message>>): JsonArray {
    val messagesJson = JsonArray()
    phrases.keys.forEach { languageCode ->
        val messagesList = phrases[languageCode] ?: error("Something weird happened with key = $languageCode")
        messagesList.filter { message: Message -> message.type == "audio" }.forEach { message: Message ->
            val outerText = JsonObject()
            val innerText = JsonArray()
            if (singleString)
                innerText.add(message.phrases?.joinToString("\n"))
            else
                message.phrases?.forEach { text -> innerText.add(text) }
            outerText.add("text", innerText)
            val textBlob = JsonObject()
            textBlob.add("text", outerText)
            textBlob.addProperty("languageCode", languageCode)
            if (!message.channel.isNullOrEmpty()) {
                textBlob.addProperty("channel", message.channel)
            }
            messagesJson.add(textBlob)
            if (message.channel != "DF_MESSENGER") {
                if (singleString)
                    messagesJson.add(message.phrases?.joinToString("\n")?.let { audioMessage(languageCode, it) })
                else
                    message.phrases
                        ?.filter { it.isNotEmpty() }
                        ?.forEach { text ->
                            messagesJson.add(audioMessage(languageCode, text))
                        }
            }
        }
    }
    return messagesJson
}
fun languagePhrasesToJson(singleString: Boolean, phrases: Map<String, List<String>>): JsonArray {
    val messages = JsonArray()
    phrases.keys.forEach { languageCode ->
        val texts = phrases[languageCode] ?: error("Something weird happened with key = $languageCode")
        val outerText = JsonObject()
        val innerText = JsonArray()
        if (singleString)
            innerText.add(texts.joinToString("\n"))
        else
            texts.forEach { text -> innerText.add(text) }
        outerText.add("text", innerText)
        val textBlob = JsonObject()
        textBlob.add("text", outerText)
        textBlob.addProperty("languageCode", languageCode)
        messages.add(textBlob)
        if (singleString)
            messages.add(audioMessage(languageCode, texts.joinToString("\n")))
        else
            texts
                .filter { it.isNotEmpty() }
                .forEach { text ->
                    messages.add(audioMessage(languageCode, text))
                }
    }
    return messages
}

/**
 * Similar functionality for languagePhrasesToJson, but instead of building the text messages
 * json object, we build a custom payload with chips.
 *
 * @param phrases map associating a language to a list of chips values
 */
fun chipsTextToJson(textFields: Map<String, List<String>>): JsonArray {
    val messages = JsonArray()
    textFields.keys.forEach { languageCode ->
        val texts = textFields[languageCode] ?: error("Something weird happened with key = $languageCode")
        val payload = JsonObject()
        val richContent = JsonArray()
        val richContentInnerArray = JsonArray()
        val richContentItem = JsonObject()
        val options = JsonArray()
        texts.forEach { chipText ->
            val chip = JsonObject()
            chip.addProperty("text", chipText)
            options.add(chip)
        }
        richContentItem.add("options", options)
        richContentItem.addProperty("type", "chips")
        richContentInnerArray.add(richContentItem)
        richContent.add(richContentInnerArray)
        payload.add("richContent", richContent)
        val message = JsonObject()
        message.add("payload", payload)
        message.addProperty("languageCode", languageCode)
        message.addProperty("channel", "DF_MESSENGER")
        messages.add(message)
    }
    return messages
}

/**
 * Looks for assignments to the "web-site" or "web-site-fwd" session parameters, and generate
 * the corresponding -ssml variables, adding them to the parameter assignment list. Then re-add
 * parameters so they appear in the correct order.
 */
fun processParameters(jsonObject: JsonObject?) {
    var parameters = jsonObject?.get("setParameterActions")
    parameters?.asJsonArray?.let { jsonArray ->
        val toAppend = mutableMapOf<String, String?>()
        val toRemove = mutableListOf<Int>()
        jsonArray.forEachIndexed { index, element ->
            val jsonObject = element.asJsonObject
            when (jsonObject["parameter"].asString) {
                "web-site" -> toAppend["web-site-ssml"] = convertWebSite(jsonObject)
                "web-site-fwd" -> toAppend["web-site-fwd-ssml"] = convertWebSite(jsonObject)
                "web-site-ssml" -> toRemove.add(index)
                "web-site-fwd-ssml" -> toRemove.add(index)
            }
        }
        toRemove.asReversed().forEach { jsonArray.remove(it) }
        toAppend.forEach { (parameter, value) ->
            val newParameter = JsonObject()
            newParameter.addProperty("parameter", parameter)
            newParameter.addProperty("value", value)
            jsonArray.add(newParameter)
        }
    }

    jsonObject?.remove("setParameterActions")
    if (parameters != null && parameters !is JsonNull) {
        jsonObject?.add("setParameterActions", parameters)
    }
}


/**
 * Converts the website to the corresponding prosody.
 */
fun convertWebSite(jsonObject: JsonObject): String? {
    val value = jsonObject["value"]
    return if (value != null && value.isJsonPrimitive)
        if (value.asJsonPrimitive.isString) processUrl(value.asJsonPrimitive.asString) else null
    else null
}

/**
 * Helper function to convert a list to a JSON array.
 */
fun toJsonArray(elements: List<String>): JsonArray {
    val jsonArray = JsonArray()
    elements.forEach { jsonArray.add(it) }
    return jsonArray
}
