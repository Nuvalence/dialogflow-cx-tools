package io.nuvalence.cx.tools.shared

import com.aallam.openai.api.completion.CompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.*

/**
 * Since ChatGPT is slow, calls it in parallel to compute training phrases variations.
 * Pool size and periodMs should be adjusted according to your account.
 *
 * @param sources list of questions we want to expand (each string may have multiple questions separated by a newline)
 * @param gptQuestion the question you want to ask ChatGPT to generate the variations
 * @param client the ChatGPT client
 */
fun parallelVariations(sources: List<String>, gptQuestion: String, client: OpenAI) =
    Throttler(poolSize = 4, periodMs = 200L).use { throttler ->
        throttler.throttle(sources) { source ->
            val questions = source.split("\n")
            val variations = questions.filter { it.isNotBlank() }.flatMap { question ->
                variation(question, gptQuestion, client)
            }
            "$source\n\n${variations.joinToString("\n")}"
        }
    }

/**
 * Calls ChatGPT to compute training phrases variations - sequentially.
 *
 * @param sources list of questions we want to expand (each string may have multiple questions separated by a newline)
 * @param gptQuestion the question you want to ask ChatGPT to generate the variations
 * @param client the ChatGPT client
 */
fun variations(sources: List<String>, gptQuestion: String, client: OpenAI) =
    sources.map { source ->
        val questions = source.split("\n")
        println("##### $questions")
        val variations = questions.filter { it.isNotBlank() }.flatMap { question ->
            variation(question, gptQuestion, client)
        }
        val result = "$source\n\n${variations.joinToString("\n")}"
        println("##### $result")
        result
    }

/**
 * Compute variations for a single set of questions.
 *
 * @param source the string with newline separated questions
 * @param gptQuestion the question you want to ask ChatGPT to generate the variations
 * @param client the ChatGPT client
 */
fun variation(source: String, gptQuestion: String, client: OpenAI) =
    source.split("\n").map { question ->
        try {
            runBlocking {
                client.completion(
                    CompletionRequest(
                        model = ModelId("text-davinci-003"),
                        prompt = gptQuestion.replace("[question]", question),
                        maxTokens = 500,
                        echo = false
                    )
                ).choices.first().text
            }
        } catch (ex: Exception) {
            "ChatGPT returned an error: ${ex.message}"
        }
    }

/**
 * Calls ChatGPT passing the responses as parameter, and asking it to generate the questions.
 * Calls in parallel, but it throttles the calls. Pool size and periodMs should be adjusted
 * according to your account.
 *
 * @param responses List of answers we want questions for
 * @param gptQuestion What to ask ChatGPT to generate questions
 * @param client the ChatGPT client
 */
fun parallelSuggestions(responses: List<String>, questions: List<String>, gptQuestion: String, client: OpenAI): List<String> =
    Throttler(poolSize = 4, periodMs = 1000L).use { throttler ->
        throttler.throttle(responses.zip(questions)) { (response, question) ->
            suggestion(response, question, gptQuestion, client)
        }
    }

/**
* Calls ChatGPT passing the responses as parameter, and asking it to generate the questions.
* Calls sequentially, so this takes some time...
*
* @param responses List of answers we want questions for
* @param gptQuestion What to ask ChatGPT to generate questions
* @param client the ChatGPT client
*/
fun suggestions(responses: List<String>, questions: List<String>, gptQuestion: String, client: OpenAI) =
    responses.zip(questions).map { (response, question) ->
        println("##### Response: $response\n##### Question: $question\n")
        val result = suggestion(response, question, gptQuestion, client)
        println("##### Generated questions:\n${result}")
        result
    }

/**
 * Calls ChatGPT  to generate questions for a single response.
 *
 * @param response Answer we want questions for
 * @param gptQuestion What to ask ChatGPT to generate questions
 * @param client the ChatGPT client
 */
fun suggestion(response: String, question: String, gptQuestion: String, client: OpenAI) =
    runBlocking {
        try {
            client.completion(
                CompletionRequest(
                    model = ModelId("text-davinci-003"),
                    prompt = "${gptQuestion.replace("[question]", question)}: \n$response",
                    maxTokens = 1000,
                    echo = false
                )
            ).choices.first().text
        } catch (ex: Exception) {
            "ChatGPT returned an error: ${ex.message}"
        }
    }