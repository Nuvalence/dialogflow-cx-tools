package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.TextFormat

enum class CellFormatPreset {
    HEADER {
        override fun getCellFormat() : CellFormat {
            return CellFormat()
                .setBackgroundColor(Color().setRed(0.8f).setGreen(0.8f).setBlue(0.8f))
                .setTextFormat(TextFormat().setBold(true))
        }
    };

    abstract fun getCellFormat() : CellFormat
}