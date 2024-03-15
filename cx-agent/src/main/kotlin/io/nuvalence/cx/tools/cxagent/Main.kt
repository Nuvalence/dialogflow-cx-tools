package io.nuvalence.cx.tools.cxagent

import io.nuvalence.cx.tools.cxagent.generator.generateAgent
import io.nuvalence.cx.tools.cxagent.compressor.zipRestore

fun main(args: Array<String>) {
    if (args.isEmpty())
        error("First parameter must specify the operation: generate to generate an agent, or zip-restore to zip and optionally restore an agent directory to a DFCX agent")
    when (args.first()) {
        "generate" -> generateAgent(args)
        "zip-restore" -> zipRestore(args)
        else -> error("First parameter must specify the operation: generate to generate an agent, or zip-restore to zip and optionally restore an agent directory to a DFCX agent")
    }
}