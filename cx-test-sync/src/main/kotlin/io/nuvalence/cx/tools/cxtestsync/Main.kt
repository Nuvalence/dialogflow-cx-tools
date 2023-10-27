package io.nuvalence.cx.tools.cxtestsync

import io.nuvalence.cx.tools.cxtestcore.Processor
import io.nuvalence.cx.tools.cxtestcore.Properties
import kotlin.reflect.full.createInstance

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) {
                Properties.init("default.properties")
            } else {
                Properties.init(args[0])
            }

            val processorClassName = Properties.getProperty<String>("processorClassName")
            val processor = Class.forName(processorClassName).kotlin.createInstance() as Processor
            processor.process()
        }
    }
}
