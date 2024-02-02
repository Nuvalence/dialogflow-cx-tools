package io.nuvalence.cx.tools.shared

import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.TextFormat

enum class HighlightPreset {
    BLUE_BOLD {
        override fun getHighlightFormat() : TextFormat {
            return TextFormat().setBold(true).setForegroundColor(Color().setBlue(0.8f))
        }
    };

    abstract fun getHighlightFormat() : TextFormat
}
