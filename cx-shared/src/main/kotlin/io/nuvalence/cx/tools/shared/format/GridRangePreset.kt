package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.GridRange

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