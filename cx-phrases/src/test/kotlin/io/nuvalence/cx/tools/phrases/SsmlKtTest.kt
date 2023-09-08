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

        var outputAudioTextEs = audioMessage("es", "¡Hola Mundo! Llámenos al 555-555-5555 o ir a google.com/insurance para ahorrar un 15,0% o más en seguro de automóvil.").toString()
        assert(outputAudioTextEs.contains("outputAudioText"))
        assert(outputAudioTextEs.contains("\"ssml\":"))
        assert(outputAudioTextEs.contains("<speak>"))
        assert(outputAudioTextEs.contains("""<say-as interpret-as=\"telephone\">"""))
        assert(outputAudioTextEs.contains("""<break time=\"300ms\"/><prosody rate=\"90%\">"""))
        assert(outputAudioTextEs.contains("languageCode"))
        assert(outputAudioTextEs.contains("g o o g l e") && outputAudioTextEs.contains("i n s u r a n c e"))
        assert(!outputAudioTextEs.contains("google") && !outputAudioTextEs.contains("insurance"))

        outputAudioText = audioMessage("en", "Albany will be said normally.").toString()
        outputAudioTextEs = audioMessage("es", "Albany spelled A-l-b-a-n-i").toString()
        var outputAudioTextOther = audioMessage("na", "Albany spelled A-l-b-a-n-y").toString()
        assert(outputAudioText.contains("Albany"))
        assert(outputAudioTextEs.contains("Albani"))
        assert(outputAudioTextOther.contains("A l b a n y"))
    }

    @Test
    fun testAddSsmlTags() {
        assertEquals("""<speak>
OK. You want to withhold <break time="300ms"/><prosody rate="90%"><say-as interpret-as="percentage">2.5%</say-as></prosody><break time="300ms"/>of your benefits for state taxes.
</speak>""", addSsmlTags("OK. You want to withhold 2.5% of your benefits for state taxes."))

        assertEquals("""<speak>
Yes, please submit by 12:15pm or go to<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim"> o n </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> n y </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> g o v </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> / </say-as></s>when<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> t o </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s>file<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> a </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s>claim</prosody><break time="300ms"/>
</speak>""", addSsmlTags("Yes, please submit by 12:15pm or go to on.ny.gov/when-to-file-a-claim"))

        assertEquals("""<speak>
Please visit<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim"> d o l </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> n y </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> g o v </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> / </say-as></s>1099<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> G </say-as></s></prosody><break time="300ms"/>
</speak>""", addSsmlTags("Please visit dol.ny.gov/1099-G"))

        assertEquals("""<speak>
To request information or records, follow the instructions outlined at<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim"> o n </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> n y </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> g o v </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> / </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> d c f a q </say-as></s></prosody><break time="300ms"/>.
</speak>""", addSsmlTags("To request information or records, follow the instructions outlined at on.ny.gov/dcfaq ."))

        assertEquals("""<speak>
Please call <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">555-123-1234</say-as></prosody><break time="300ms"/>to schedule an appointment.
</speak>""", addSsmlTags("Please call 555-123-1234 to schedule an appointment."))

        assert(!addSsmlTags("Do not process 1234567890 as a telephone number").contains("<say-as interpret-as=\"telephone\">"))

        assertEquals("""<speak>
1099-G <break time="300ms"/> and 1099-G <break time="300ms"/> and 1099-G <break time="300ms"/>
</speak>""", addSsmlTags("1099-G and 1099G and 1099 G"))
    }

    @Test
    fun testProcessUrl() {
        assertEquals("session.params.web-site", processUrl("session.params.web-site"))
        assertEquals("session.params.web-site-fwd", processUrl("session.params.web-site-fwd"))
        assertEquals("""<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim"> d o l </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> n y </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> g o v </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> / </say-as></s>when<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> t o </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s>file<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> a </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s>claim</prosody><break time="300ms"/>""",
            processUrl(" dol.ny.gov/when-to-file-a-claim"))
        assertEquals("""<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim"> n y </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> d o l </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> g o v </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s>com<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> / </say-as></s>1099<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> - </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> G </say-as></s></prosody><break time="300ms"/>""",
            processUrl("ny.dol.gov.com/1099-G"))
        assertEquals("""<break time="300ms"/><prosody rate="90%">unemployment<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s>labor<break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> n y </say-as></s><break time="100ms"/><s><break time="100ms"/><say-as interpret-as="verbatim"> . </say-as></s><s><break time="100ms"/><say-as interpret-as="verbatim"> g o v </say-as></s></prosody><break time="300ms"/>""",
            processUrl("unemployment.labor.ny.gov"))

        var processedUrl = processUrl("nys.dol.ny.gov/nysdol-po-ny-gov")
        assert(processedUrl.contains("n y s"))
        assert(processedUrl.contains("d o l"))
        assert(processedUrl.contains("n y"))
        assert(processedUrl.contains("g o v"))
        assert(processedUrl.contains("n y s d o l"))
        assert(processedUrl.contains("p o"))
    }

    @Test
    fun testProcessNumber() {
        var ssmlNumber = """<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim">123450</say-as></s></prosody><break time="300ms"/>"""
        assertEquals(ssmlNumber, processNumber("123450"))
        var ssmlNumberNormal = """<break time="300ms"/><prosody rate="90%"><s><break time="100ms"/><say-as interpret-as="verbatim">1564</say-as></s></prosody><break time="300ms"/>"""
        assertEquals(ssmlNumberNormal, processNumber("1564"))
        assertEquals("800", processNumber("800"))
    }

    @Test
    fun testProcessPhone() {
        var ssmlPhone = """<break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">123-456-7890</say-as></prosody><break time="300ms"/>"""
        assertEquals(ssmlPhone, processPhone("123-456-7890"))
    }

    @Test
    fun testProcessPercentage() {
        var ssmlPercentage = """<break time="300ms"/><prosody rate="90%"><say-as interpret-as="percentage">2.5%</say-as></prosody><break time="300ms"/>"""
        assertEquals(ssmlPercentage, processPercentage("2.5%"))
    }

    @Test
    fun testSplitUrl() {
        assertEquals(9, splitUrl("dol.ny.gov/1099-G").size)
        assertEquals(13, splitUrl(" dol.ny.gov/create-confirmation-number/signin").size)
        assertEquals(5, splitUrl("on.ny.com").size)
    }
}