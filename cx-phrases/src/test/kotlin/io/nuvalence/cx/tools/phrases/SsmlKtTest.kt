package io.nuvalence.cx.tools.phrases

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SsmlKtTest {

    @Test
    fun testAudioMessage() {
        var outputAudioText = audioMessage("en", "Hello world! Please call us at 555-555-5555 to save 15.0% or more on car insurance.").toString()
        assert(outputAudioText.contains("outputAudioText"))
        assert(outputAudioText.contains("\"ssml\":"))
        assert(outputAudioText.contains("<speak>"))
        assert(outputAudioText.contains("""<say-as interpret-as=\"telephone\">"""))
        assert(outputAudioText.contains("""<break time=\"300ms\"/><prosody rate=\"90%\">"""))
        assert(outputAudioText.contains("languageCode"))
    }

    @Test
    fun testAddSsmlTags() {
        assertEquals("""<speak>
OK. You want to withhold <break time="300ms"/><prosody rate="90%">2</prosody><break time="300ms"/>.<break time="300ms"/><prosody rate="90%">5</prosody><break time="300ms"/>% of your benefits for state taxes.
</speak>""", addSsmlTags("OK. You want to withhold 2.5% of your benefits for state taxes."))

        assertEquals("""<speak>
Yes, please submit by 12:15pm or go to<break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> o n </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>when<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> t o </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>file<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> a </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>claim</prosody><break time="300ms"/>
</speak>""", addSsmlTags("Yes, please submit by 12:15pm or go to on.ny.gov/when-to-file-a-claim"))

        assertEquals("""<speak>
Please visit<break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> d o l </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>1099<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> G </say-as></prosody><break time="300ms"/>
</speak>""", addSsmlTags("Please visit dol.ny.gov/1099-G"))

        assertEquals("""<speak>
To request information or records, follow the instructions outlined at<break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> o n </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>dcfaq</prosody><break time="300ms"/>.
</speak>""", addSsmlTags("To request information or records, follow the instructions outlined at on.ny.gov/dcfaq ."))

        assertEquals("""<speak>
Please call <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">555-123-1234</say-as></prosody><break time="300ms"/>to schedule an appointment.
</speak>""", addSsmlTags("Please call 555-123-1234 to schedule an appointment."))

        assert(!addSsmlTags("Do not process 1234567890 as a telephone number").contains("<say-as interpret-as=\"telephone\">"))
    }

    @Test
    fun testProcessUrl() {
        assertEquals("session.params.web-site", processUrl("session.params.web-site"))
        assertEquals("session.params.web-site-fwd", processUrl("session.params.web-site-fwd"))
        assertEquals("""<break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> d o l </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>when<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> t o </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>file<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> a </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>claim</prosody><break time="300ms"/>""",
            processUrl(" dol.ny.gov/when-to-file-a-claim"))
        assertEquals("""<break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> d o l </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>com<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>1099<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> G </say-as></prosody><break time="300ms"/>""",
            processUrl("ny.dol.gov.com/1099-G"))
    }

    @Test
    fun testProcessNumber() {
        var ssmlNumber = """<break time="300ms"/><prosody rate="90%">1 2 3 4 5 zero</prosody><break time="300ms"/>"""
        assertEquals(ssmlNumber, processNumber("123450"))
        var ssmlNumberNormal = """<break time="300ms"/><prosody rate="90%">1 5 6 4</prosody><break time="300ms"/>"""
        assertEquals(ssmlNumberNormal, processNumber("1564"))
        var ssmlNumber800 = """<break time="300ms"/><prosody rate="90%"> eight hundred </prosody><break time="300ms"/>"""
        assertEquals(ssmlNumber800, processNumber("800"))
    }

    @Test
    fun testProcessPhone() {
        var ssmlPhone = """<break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">123-456-7890</say-as></prosody><break time="300ms"/>"""
        assertEquals(ssmlPhone, processPhone("123-456-7890"))
    }

    @Test
    fun testSplitUrl() {
        assertEquals(9, splitUrl("dol.ny.gov/1099-G").size)
        assertEquals(13, splitUrl(" dol.ny.gov/create-confirmation-number/signin").size)
        assertEquals(5, splitUrl("on.ny.com").size)
    }
}