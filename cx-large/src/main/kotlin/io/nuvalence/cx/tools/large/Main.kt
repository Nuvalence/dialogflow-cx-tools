package io.nuvalence.cx.tools.large

import io.nuvalence.cx.tools.shared.SheetWriter
import java.net.URL

fun main(args: Array<String>) {
    if (args.size < 2)
        error("Must supply <spreadsheet id> <URL to credentials.json file>")

    val spreadsheetId = args[0]
    val url = URL(args[1])
    val questionList = generateQuestions()
    val sheetWriter = SheetWriter(url, spreadsheetId)
    sheetWriter.deleteTab("Questions")
    sheetWriter.addTab("Questions")
    sheetWriter.addDataToTab(
        "Questions",
        questionList,
        listOf("Topic", "Intent", "Training Phrases", "Response", "Website", "Follow-up"),
        listOf(150, 150, 400, 400, 150, 150)
    )
}

fun generateQuestions() =
    categories.flatMap { category ->
        questions.mapIndexed { index, question ->
            val intent = "$category-$index"
            val withSubject = question.replace("[subject]", category)
            listOf(category, intent, withSubject, "The not overly creative answer to $withSubject is $category")
        }
    }

val questions = """
    What are common symptoms of [subject]?
    How does [subject] affect daily life?
    What are the main causes of [subject]?
    What are the benefits of [subject]?
    What are the risks involved in [subject]?
    Can you explain the process of [subject]?
    What are the available treatment options for [subject]?
    What are the long-term effects of [subject]?
    Are there any preventive measures for [subject]?
    What are the different types of [subject]?
    How does [subject] impact society?
    Are there any current research studies on [subject]?
    What are the most effective strategies for managing [subject]?
    Can you share success stories related to [subject]?
    How does [subject] relate to other fields?
    What are the potential complications of [subject]?
    What are the common misconceptions about [subject]?
    Can you recommend reliable resources for learning about [subject]?
    What are the ethical considerations in [subject]?
    Are there any promising advancements in [subject] research?
""".trimIndent().split("\n")

val categories = """
    Sports
    Technology
    Movies
    Music
    History
    Food
    Science
    Literature
    Travel
    Fashion
    Art
    Politics
    Geography
    Health and Wellness
    Education
    Finance
    Mythology
    Philosophy
    Animals
    Psychology
    Environment
    Business
    Astronomy
    Language
    Religion
    Cars
    Television
    Photography
    Video Games
    Relationships
    Human Body
    Weather
    Architecture
    Hobbies
    Celebrities
    Fitness
    Law
    Mathematics
    Cooking
    Gardening
    Writing
    Dance
    Home Improvement
    Supernatural
    Fashion Designers
    Social Media
    Current Events
    Journalism
    Sports Cars
    Nutrition
    Ethics
    Sociology
    Energy
    World Records
    Inventions
    Space Exploration
    Robotics
    Famous Landmarks
    Comedy
    Theater
    Parenting
    Crafting
    Automobile Racing
    Aviation
    Famous Battles
    Archaeology
    Military History
    Cryptocurrency
    Sustainable Living
    DIY Projects
    Film Directors
    Music Genres
    Quantum Physics
    Board Games
    Cultural Traditions
    Famous Speeches
    Virtual Reality
    Yoga and Meditation
    Celebrity Gossip
    Home Decor
""".trimIndent().split("\n")