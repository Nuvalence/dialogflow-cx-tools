package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.TextFormat

/**
 * Enumerates presets for cell formats for use with Google Sheets.
 * This can be used by projects that do not or cannot themselves contain the Google Sheets API library.
 */
enum class CellFormatPreset {
    HEADER {
        override fun getCellFormat() : CellFormat {
            return CellFormat()
                .setBackgroundColor(Color().setRed(0.8f).setGreen(0.8f).setBlue(0.8f))
                .setTextFormat(TextFormat().setBold(true))
        }
    },
    BG_RED {
        override fun getCellFormat() : CellFormat {
            return CellFormat().setBackgroundColor(Color().setRed(1.0f).setGreen(0.8f).setBlue(0.8f))
        }
    };

    abstract fun getCellFormat() : CellFormat
}