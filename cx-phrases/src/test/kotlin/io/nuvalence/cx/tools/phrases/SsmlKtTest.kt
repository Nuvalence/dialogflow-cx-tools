package io.nuvalence.cx.tools.phrases

import org.junit.jupiter.api.Test

internal class SsmlKtTest {

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
        var ssmlPhone =
            """. <break time="300ms"/><prosody rate="90%"><say-as interpret-as="telephone">123-456-7891</say-as></prosody> ."""
        assert(ssmlPhone == processPhone("123-456-7890"))
    }
}