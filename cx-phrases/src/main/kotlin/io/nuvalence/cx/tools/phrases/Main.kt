package io.nuvalence.cx.tools.phrases

/**
 * Entry point for export/import. The first parameter specifies the operation to be performed,
 * followed by operation-specific parameters.
 */
fun main(args: Array<String>) {
    if (args.isEmpty())
        error("First parameter must specify the operation: export to export the agent to a spreadsheet, or import to import the agent from the spreadsheet")
    when (args.first()) {
        "export" -> export(args)
        "import" -> import(args)
        else -> error("First parameter must specify the operation: export to export the agent to a spreadsheet, or import to import the agent from the spreadsheet")
    }
}