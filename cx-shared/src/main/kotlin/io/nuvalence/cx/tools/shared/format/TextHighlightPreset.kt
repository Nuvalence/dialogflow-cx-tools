package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.TextFormat

enum class TextHighlightPreset {
    BLUE_BOLD {
        override fun getHighlightFormat() : TextFormat {
            return TextFormat().setBold(true).setForegroundColor(Color().setBlue(0.8f))
        }
    };

    abstract fun getHighlightFormat() : TextFormat
}
