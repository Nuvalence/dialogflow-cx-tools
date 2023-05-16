package io.nuvalence.cx.tools.cxagent.model

import java.util.*

/**
 * Captures an intent
 *
 * @param displayName the human-readable display name of this intent
 * @param name the intent name (misnomer, it's the UUID)
 * @param trainingPhrases list of training phrases associated with this intent
 * @param priority keeping it as the default for now
 */
data class Intent(
    val displayName: String,
    val name: String = UUID.randomUUID().toString(),
    val trainingPhrases: List<TrainingPhrase>,
    val priority: Int = 500000
)

/**
 * Represents a single training phrase
 *
 * @param id the UUID associated with this phrase
 * @param parts the parts that constitute this message (text + entity captures)
 * @param repeatCount keeping as default for now
 * @param languageCode keeping English only for now
 */
data class TrainingPhrase(
    val id: String = UUID.randomUUID().toString(),
    val parts: List<Part>,
    val repeatCount: Int = 1,
    val languageCode: String = "en"
)

/**
 * A phrase part.
 *
 * @param text the actual text
 * @param auto keeping as default for now
 */
data class Part(
    val text: String,
    val auto: Boolean = true
)

