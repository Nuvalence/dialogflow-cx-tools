package io.nuvalence.cx.tools.shared

import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentSkipListMap

suspend fun parallelVariations(sources: List<String>, gptQuestion: String, client: OpenAI): List<String> {
    val result = ConcurrentSkipListMap<Int, String>()
    runBlocking(Dispatchers.Default) {
        sources.mapIndexed { index, source ->
            async {
                val questions = source.split("\n")
                val variations = questions.flatMap { question ->
                    variation(question, gptQuestion, client)
                }
                result[index] = "$source\n-----\n${variations.joinToString("\n")}"
            }
        }.awaitAll()
    }
    return result.values.toList()
}

suspend fun variations(sources: List<String>, gptQuestion: String, client: OpenAI) =
    sources.map { source ->
        val questions = source.split("\n")
        val variations = questions.flatMap { question ->
            variation(question, gptQuestion, client)
        }
        "$source\n-----\n${variations.joinToString("\n")}"
    }

suspend fun variation(source: String, gptQuestion: String, client: OpenAI) =
    source.split("\n").map { question ->
        client.completion(
            CompletionRequest(
                model = ModelId("text-davinci-003"),
                prompt = gptQuestion.replace("[question]", question),
                maxTokens = 200,
                echo = false
            )
        ).choices.first().text
    }
