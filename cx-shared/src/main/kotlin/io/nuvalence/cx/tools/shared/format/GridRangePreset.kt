package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.GridRange

/**
 * Enumerates presets for cell ranges for use with Google Sheets, generally for formatting tasks.
 * This can be used by projects that do not or cannot themselves contain the Google Sheets API library.
 */
enum class GridRangePreset {
    FIRST_N_ROWS {
        override fun getGridRange(sheetId: Int, span: Int): GridRange {
            return GridRange().setSheetId(sheetId)
                .setStartRowIndex(0).setEndRowIndex(span)
        }
    },
    FIRST_N_COLUMNS {
        override fun getGridRange(sheetId: Int, span: Int): GridRange {
            return GridRange().setSheetId(sheetId)
                .setStartColumnIndex(0).setEndColumnIndex(span)
        }
    };

    abstract fun getGridRange(sheetId: Int, span: Int) : GridRange
}