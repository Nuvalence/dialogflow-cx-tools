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
fun languagePhrasesToJson(singleString: Boolean, phrases: Map<String, List<Message>>, fulfillmentJson: JsonObject?): JsonArray {
    val messagesJson = JsonArray()
    phrases.keys.forEach { languageCode ->
        val messagesList = phrases[languageCode] ?: error("Something weird happened with key = $languageCode")
        messagesList.filter { message -> message.type == "message" }.forEach { message ->
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
            if (!message.channel.isNullOrEmpty() && !message.channel.equals("audio")) {
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

        val payloadBlob = JsonObject()
        val payload = JsonObject()
        val richContent = JsonArray()
        val innerRichContent = JsonArray()
        var channel: String? = null
        messagesList.filter { message -> message.type != "message" }.forEach { message ->
            val content = JsonObject()
            val messageType = message.type?.split("\n")?.firstOrNull()
            if (channel == null) {
                channel = message.channel.toString()
            }

            var messagePayloads = JsonArray()
            fulfillmentJson?.get("messages")?.asJsonArray?.forEach { messageJson ->
                if (messageJson.asJsonObject["payload"] != null && messageJson.asJsonObject["channel"].asString == "DF_MESSENGER" && messageJson.asJsonObject["languageCode"].asString == "en") {
                    messagePayloads = messageJson.asJsonObject["payload"].asJsonObject["richContent"].asJsonArray.get(0).asJsonArray
                }
            }

             when(messageType) {
                 "html" -> {
                     content.addProperty("html", message.phrases?.joinToString("\n"))
                     content.addProperty("type", "html")
                 }
                 "button" -> {
                     message.phrases?.forEach { buttonText ->
                         var buttonAttributes = JsonObject()

                         messagePayloads.forEach { messagePayload ->
                             if (messagePayload.asJsonObject["type"].asString == "button" && messagePayload.asJsonObject["text"].asString == buttonText) {
                                 buttonAttributes = messagePayload.asJsonObject
                             }
                         }
                         buttonAttributes.remove("text")
                         buttonAttributes.remove("type")
                         buttonAttributes.addProperty("text", buttonText)
                         buttonAttributes.addProperty("type", "button")
                         innerRichContent.add(buttonAttributes)
                     }
                 }
                 "chips" -> {
                     val options = JsonArray()

                     var chipOptions = JsonArray()
                     messagePayloads.forEach { messagePayload ->
                         if (messagePayload.asJsonObject["type"].asString == "chips") {
                             chipOptions = messagePayload.asJsonObject["options"].asJsonArray
                         }
                     }

                     message.phrases?.forEach { chipText ->
                         var chipAttributes = JsonObject()

                         chipOptions.forEach { chipOption ->
                             if (chipOption.asJsonObject["text"].asString == chipText) {
                                 chipAttributes = chipOption.asJsonObject
                             }
                         }
                         chipAttributes.remove("text")
                         chipAttributes.addProperty("text", chipText)

                         options.add(chipAttributes)
                     }
                     content.add("options", options)
                     content.addProperty("type", "chips")
                 }
            }
            if (!content.isEmpty) {
                innerRichContent.add(content)
            }
        }
        if (!innerRichContent.isEmpty) {
            richContent.add(innerRichContent)
            payload.add("richContent", richContent)
            payloadBlob.add("payload", payload)
            payloadBlob.addProperty("languageCode", languageCode)
            if (!channel.isNullOrEmpty() && !channel.equals("audio")) {
                payloadBlob.addProperty("channel", channel)
            }
            messagesJson.add(payloadBlob)
        }
    }
    return messagesJson
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
