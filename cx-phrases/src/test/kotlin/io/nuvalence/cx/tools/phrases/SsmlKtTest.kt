package io.nuvalence.cx.tools.phrases

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SsmlKtTest {

    @Test
    fun testAudioMessage() {
        assertEquals(audioMessage("en", "Hello world! Please call us at 555-555-5555 to save 15.0% or more on car insurance."), """{"outputAudioText":{"ssml":"<speak>\nHello world! Please call us at  . <break time=\"300ms\"/><prosody rate=\"90%\"><say-as interpret-as=\"telephone\">555-555-5555</say-as></prosody> . or contain  . <break time=\"300ms\"/><prosody rate=\"90%\">2</prosody> . . . <break time=\"300ms\"/><prosody rate=\"90%\">5</prosody> . % of yourself.\n</speak>"},"languageCode":"en"}""")
    }

    @Test
    fun testAddSsmlTags() {
        assertEquals(addSsmlTags("OK. You want to withhold 2.5% of your benefits for state taxes."), """<speak>
OK. You want to withhold  . <break time="300ms"/><prosody rate="90%">2</prosody><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><break time="300ms"/><prosody rate="90%">5</prosody> . % of your benefits for state taxes.
</speak>""")

        assertEquals(addSsmlTags("Yes, please submit by 12:15pm or go to on.ny.gov/when-to-file-a-claim"), """<speak>
Yes, please submit by 12:15pm or go to . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> o n </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>when<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> t o </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>file<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> a </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>claim</prosody> . 
</speak>""")

        assertEquals(addSsmlTags("Please visit dol.ny.gov/1099-G"), """<speak>
Please visit . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> d o l </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>1099<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> G </say-as></prosody> . 
</speak>""")

        assertEquals(addSsmlTags("To request information or records, follow the instructions outlined at on.ny.gov/dcfaq ."), """<speak>
To request information or records, follow the instructions outlined at . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> o n </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>d c f a q</prosody> . .
</speak>""")

        assertEquals(addSsmlTags("To appeal a hearing, send a letter to the UI Appeal Board at PO Box 15126, Albany, NY 12212-5126, or by fax to 518-402-6208. Be sure to include your Administrative Law Judge Case number in your appeal request."),
        """<speak>
To appeal a hearing, send a letter to the UI Appeal Board at PO Box  . <break time="300ms"/><prosody rate="90%">1 5 1 2 6</prosody> . , Albany, NY . <break time="300ms"/><prosody rate="90%">1 2 2 1 2</prosody><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><break time="300ms"/><prosody rate="90%">5 1 2 6</prosody> . , or by fax to  . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">518-402-6208</say-as></prosody> . . Be sure to include your Administrative Law Judge Case number in your appeal request.
</speak>""")

        assertEquals(addSsmlTags("Please call 555-123-1234 to schedule an appointment."), """<speak>
Please call  . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">555-123-1234</say-as></prosody> . to schedule an appointment.
</speak>""")

        assert(!addSsmlTags("Do not process 1234567890 as a telephone number").contains("<say-as interpret-as=\"telephone\">"))
    }

    @Test
    fun testProcessUrl() {
        assertEquals(processUrl("session.params.web-site"), "session.params.web-site")
        assertEquals(processUrl("session.params.web-site-fwd"), "session.params.web-site-fwd")
        assertEquals(processUrl(" dol.ny.gov/when-to-file-a-claim"),
            """ . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> d o l </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>when<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> t o </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>file<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> a </say-as><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as>claim</prosody> . """)
        assertEquals(processUrl("ny.dol.gov.com/1099-G"), """ . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="verbatim"> n y </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as><say-as interpret-as="verbatim"> d o l </say-as><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>gov<break time="100ms"/><say-as interpret-as="verbatim"> . </say-as>com<break time="100ms"/><say-as interpret-as="verbatim"> / </say-as>1099<break time="100ms"/><say-as interpret-as="verbatim"> - </say-as><say-as interpret-as="verbatim"> G </say-as></prosody> . """)
    }

    @Test
    fun testProcessNumber() {
        var ssmlNumber = """ . <break time="300ms"/><prosody rate="90%">1 2 3 4 5 zero</prosody> . """
        assertEquals(ssmlNumber, processNumber("123450"))
        var ssmlNumberNormal = """ . <break time="300ms"/><prosody rate="90%">1 5 6 4</prosody> . """
        assertEquals(ssmlNumberNormal, processNumber("1564"))
        var ssmlNumber800 = """ . <break time="300ms"/><prosody rate="90%"> eight hundred </prosody> . """
        assertEquals(ssmlNumber800, processNumber("800"))
    }

    @Test
    fun testProcessPhone() {
        var ssmlPhone = """ . <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">123-456-7890</say-as></prosody> . """
        assertEquals(ssmlPhone, processPhone("123-456-7890"))
    }

    @Test
    fun testSplitUrl() {
        assertEquals(splitUrl("dol.ny.gov/1099-G").size, 9)
        assertEquals(splitUrl(" dol.ny.gov/create-confirmation-number/signin").size, 13)
        assertEquals(splitUrl("on.ny.com").size, 5)
    }
}