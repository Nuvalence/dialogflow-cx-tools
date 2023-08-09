package io.nuvalence.cx.tools.phrases

import org.junit.jupiter.api.Test

internal class SsmlKtTest {

    @Test
    fun audioMessage() {
    }

    @Test
    fun testAddSsmlTags() {
        //println(addSsmlTags("OK. You want to withhold 2.5% of your benefits for state taxes.\n\n"))
        //println((addSsmlTags("Yes, please submit by 12:15pm or go to on.ny.gov/when-to-file-a-claim\n\n")))
        //println(addSsmlTags("Please visit dol.ny.gov/1099-G"))
    }

    @Test
    fun processString() {
    }

    @Test
    fun processUrl() {
    }

    @Test
    fun testProcessNumber() {
        var ssmlNumber = """. <break time="300ms"/><prosody rate="90%">1 2 3 4 5 zero</prosody> ."""
        assert(ssmlNumber == processNumber("123450"))
        var ssmlNumberNormal = """. <break time="300ms"/><prosody rate="90%">1564</prosody> ."""
        assert(ssmlNumberNormal == processNumber("1564"))
        var ssmlNumber800 = """. <break time="300ms"/><prosody rate="90%"> eight hundred </prosody> ."""
        assert(ssmlNumber800 == processNumber("800"))

    }

    @Test
    fun testProcessPhone() {
        var ssmlPhone = """. <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">123-456-7891</say-as></prosody> ."""
        assert(ssmlPhone == processPhone("123-456-7890"))
    }

    @Test
    fun splitUrl() {
    }
}