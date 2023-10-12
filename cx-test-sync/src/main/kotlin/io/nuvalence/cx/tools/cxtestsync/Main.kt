package io.nuvalence.cx.tools.cxtestsync

import io.nuvalence.cx.tools.cxtestsync.processor.SpreadsheetProcessor
import io.nuvalence.cx.tools.cxtestsync.util.Properties
import javax.naming.ConfigurationException

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) {
                throw ConfigurationException("Please specify a properties file in the arguments before proceeding.")
            }

            Properties.init(args[0])

            // Process
            // SpreadsheetProcessor
        }
    }

}
