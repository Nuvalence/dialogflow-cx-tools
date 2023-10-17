package io.nuvalence.cx.tools.cxtestsync

import io.nuvalence.cx.tools.cxtestsync.processor.SpreadsheetProcessor
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import javax.naming.ConfigurationException

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) {
                Properties.init("default.properties")
            } else {
                Properties.init(args[0])
            }

            SpreadsheetProcessor().process()
        }
    }

}
