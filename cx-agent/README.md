# CX-AGENT

This directory has a tool for either generating or restoring a Dialogflow CX agent.

The generation tool can generate an agent based on a pattern defined in the root level `documentation` folder.

The restore functionality can zip a folder and restore that zip file to an existing DFCX agent in GCP.

## Setup

### Authentication with Dialogflow CX API

1. Login with the gcloud CLI to obtain credentials locally. Make sure the account you choose has access to your desired agent.
```
gcloud auth login
```

2. Set your project
```
gcloud config set project <project-name>
```

3. Set up application default credentials for use by local applications.
```
gcloud auth application-default login
```


## Running the Agent Generator
Note that unless you are using a sheet that is already created, you will need to create your own Google Sheet.

Main class: `io.nuvalence.cx.tools.cxagent.MainKt`

```
cd cx-agent
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* The word `generate`
* Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* Your Dialogflow project ID (the numeric project id)
* Directory where the agent and agent.zip should be created
* URL where to find the `credentials.json` file granting access to the Google Sheet above

## Running the Agent Zip/Restore Tool
Main class: `io.nuvalence.cx.tools.cxagent.MainKt`

```
cd cx-agent
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* The word `zip-restore`
* The full path to a local agent directory to zip/compress
* The full path to the resulting zip file
  * This should have `.zip` at the end
* (Optional) Project ID for GCP Project in which Dialogflow CX Agent is located to restore the zip file to
* (Optional) Location in which the Dialogflow CX Agent is located (e.g. `global`)
* (Optional) Agent ID for the Dialogflow CX Agent to restore the zip file to
  * This can be found in the URL for your agent like `/agents/<your-agent-id>`