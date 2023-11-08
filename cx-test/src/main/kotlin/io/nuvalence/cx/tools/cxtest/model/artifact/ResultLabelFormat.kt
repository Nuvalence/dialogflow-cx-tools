package io.nuvalence.cx.tools.cxtest.model.artifact

import com.google.api.services.sheets.v4.model.Color
import io.nuvalence.cx.tools.cxtest.model.test.ResultLabel

enum class ResultLabelFormat (val data: ResultLabel, val color: Color) {
    PASS(ResultLabel.PASS, Color().setRed(0.8f).setGreen(1.0f).setBlue(0.8f)),
    WARN(ResultLabel.WARN, Color().setRed(1.0f).setGreen(1.0f).setBlue(0.8f)),
    FAIL(ResultLabel.FAIL, Color().setRed(1.0f).setGreen(0.8f).setBlue(0.8f))
}
