package io.nuvalence.cx.tools.cxagent.compressor

import com.google.cloud.dialogflow.cx.v3.AgentName
import com.google.cloud.dialogflow.cx.v3.AgentsClient
import com.google.cloud.dialogflow.cx.v3.RestoreAgentRequest
import com.google.protobuf.ByteString
import io.nuvalence.cx.tools.shared.zipDirectory
import java.nio.file.Files
import java.nio.file.Paths

fun zipRestore(args: Array<String>) {
    if (args.size < 3) {
        error("Required parameters: <path to local agent directory to compress> <path to resulting zip file>")
    } else if (args.size == 3 || args.size == 6) {
        val sourceAgentPath = args[1]
        val targetAgentPath = args[2]
        try {
            zipDirectory(sourceAgentPath, targetAgentPath)
        } catch (e: Exception) {
            print("An error occurred while attempting to create a zip file with name $targetAgentPath from the folder $sourceAgentPath: $e")
        }
        if (args.size == 6) {
            val projectId = args[3]
            val location = args[4]
            val agentId = args[5]
            restoreAgent(targetAgentPath,  projectId, location, agentId)
        }
    } else {
        error("Wrong number of parameters provided; please check the required parameters for your operation")
    }

}

fun restoreAgent(zipFile: String, projectId: String, location: String, agentId: String) {
    try {
        val agentsClient = AgentsClient.create()
        val agentName = AgentName.of(projectId, location, agentId)
        val agentBytes = Files.readAllBytes(Paths.get(zipFile))
        val agentByteString = ByteString.copyFrom(agentBytes)
        val restoreAgentRequest = RestoreAgentRequest.newBuilder()
            .setName(agentName.toString())
            .setAgentContent(agentByteString)
            .build()

        agentsClient.restoreAgentAsync(restoreAgentRequest).get()

    } catch (e: Exception) {
        print("An error happened while attempting to restore the zip file named $zipFile to the agent with id $agentId in project $projectId at location $location: $e")
    }
}