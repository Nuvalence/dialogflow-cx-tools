package io.nuvalence.cx.tools.shared.format

import com.google.api.services.sheets.v4.model.GridProperties
import com.google.api.services.sheets.v4.model.SheetProperties

/**
 * Enumerates presets for sheet properties for use with Google Sheets.
 * This can be used by projects that do not or cannot themselves contain the Google Sheets API library.
 */
enum class SheetPropertyPreset {
    FREEZE_N_ROWS {
        override fun getSheetProperties(sheetId: Int, span: Int): SheetProperties {
            return SheetProperties().setSheetId(sheetId).setGridProperties(GridProperties().setFrozenRowCount(span))
        }

        override fun getFields(): String {
            return "gridProperties.frozenRowCount"
        }
    },
    FREEZE_N_COLUMNS {
        override fun getSheetProperties(sheetId: Int, span: Int): SheetProperties {
            return SheetProperties().setSheetId(sheetId).setGridProperties(GridProperties().setFrozenColumnCount(span))
        }

        override fun getFields(): String {
            return "gridProperties.frozenColumnCount"
        }
    };

    abstract fun getSheetProperties(sheetId: Int, span: Int) : SheetProperties
    abstract fun getFields() : String
}