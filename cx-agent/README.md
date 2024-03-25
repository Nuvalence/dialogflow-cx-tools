# CX-AGENT
This module contains tools for
- Restoring a Dialogflow CX agent from local files
- Generating a Dialogflow CX agent (Deprecated)

# Table of Contents
1. [Setup](#setup)
2. [Agent Restorer](#agent-restorer)
3. [Agent Generator (Deprecated)](#agent-generator-deprecated)


The generation tool can generate an agent based on a pattern defined in the root level `documentation` folder.

The restore functionality can zip a folder and restore that zip file to an existing DFCX agent in GCP.

## Setup
### Authentication with Dialogflow CX API
To log in with the gcloud CLI, see our
[Local Authentication documentation](../documentation/local-authentication.md).

# Agent Restorer
This function will zip together local Agent JSON files and, if credentials and an existing Agent ID is provided, will
run a Restore command to update the specified agent using the zipped file.

## Running the Agent Zip/Restore Tool
Main class: `io.nuvalence.cx.tools.cxagent.MainKt`

From the root directory:
```
./gradlew cx-agent:run --args="<arguments as described below>"
```

### Arguments
| Argument # | Argument Description                                                                                      | Argument Required | Argument Example                       |
|------------|-----------------------------------------------------------------------------------------------------------|-------------------|----------------------------------------|
| 1          | The specified function to run the zip and agent restore process. This should be `zip-restore`             | true              | `zip-restore`                          |
| 2          | The full path to a local agent directory to zip/compress                                                  | true              | `/Users/usr/source/agent`              |
| 3          | The full path to the resulting zip file. This should have `.zip` at the end.                              | true              | `/Users/usr/source/agent/agent.zip`    |
| 4          | (Optional) Project ID for GCP Project in which Dialogflow CX Agent is located to restore the zip file to. | false             | `dol-uisim-ccai-dev-app`               |
| 5          | (Optional) Region in which the Dialogflow CX Agent is located                                             | false             | `global`                               |
| 6          | (Optional) Agent ID for the Dialogflow CX Agent to restore the zip file to.                               | false             | `ee332e8e-de2c-4d65-884f-405682eeaf3c` |

### Example
```
./gradlew cx-agent:run --args="zip-restore \
/Users/usr/source/agent \
/Users/usr/source/agent/agent.zip \
dol-uisim-ccai-dev-app \
global \
ee332e8e-de2c-4d65-884f-405682eeaf3c"
```

# Agent Generator (Deprecated)
Note the Agent Generator uses patterns that are no longer recommended. If starting a project from scratch, it is not
recommended to use the Agent Generator as scaffolding.

## Running the Agent Generator
Note that unless you are using a sheet that is already created, you will need to create your own Google Sheet.

Main class: `io.nuvalence.cx.tools.cxagent.MainKt`

```
./gradlew cx-agent:run --args="<arguments as described below>"
```

### Arguments

| Argument # | Argument Description                                                                                                                                                                            | Argument Required | Argument Example                                                                                             |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|--------------------------------------------------------------------------------------------------------------|
| 1          | The specified function to run the zip and agent restore process. This should be `generate`                                                                                                      | true              | `generate`                                                                                                   |
| 2          | Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: <br> docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559  | true              | `1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU` |
| 3          | Your Dialogflow Project number                                                                                                                                                                  | true              | `919068341332`                                                                                               |
| 4          | Directory where the agent and agent.zip should be created                                                                                                                                       | false             | `/Users/usr/source/agent`                                                                                    |
| 5          | URL where to find the `credentials.json` file granting access to the Google Sheet above                                                                                                         | false             | `/Users/usr/source/agent/credentials.json`                                                                   |

### Example
```
./gradlew cx-agent:run --args="generate \
1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU \
919068341332 \
/Users/usr/source/agent \
global \
/Users/usr/source/agent/credentials.json"
```