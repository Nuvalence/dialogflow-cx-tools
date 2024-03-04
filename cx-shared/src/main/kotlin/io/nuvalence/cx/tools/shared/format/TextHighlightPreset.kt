package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.TextFormat

/**
 * Enumerates presets for text highlighting formats for use with Google Sheets.
 * Use this instead of CellFormatPreset in situations where text formatting should not apply to the whole cell (e.g. where TextFormatRuns are necessary)
 * This can be used by projects that do not or cannot themselves contain the Google Sheets API library.
 */
enum class TextHighlightPreset {
    BLUE_BOLD {
        override fun getHighlightFormat() : TextFormat {
            return TextFormat().setBold(true).setForegroundColor(Color().setBlue(0.8f))
        }
    };

    abstract fun getHighlightFormat() : TextFormat
}
