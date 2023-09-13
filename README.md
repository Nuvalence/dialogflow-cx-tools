# Agent Essentials for CCAI
The Nuvalence Agent Essentials for CCAI are accelerators that automate mundane, 
manual tasks related to creating and maintaining CCAI/Dialogflow agents:

1. Generating an initial cut of an Agent capable of answering questions.
2. Supporting translations and proper prosody for URLs, numbers, etc.

You can find detailed documentation under the `documentation` folder.

# Prerequisites
* JDK 17

# Building

Please run the build command without executing tests, as cx-tests will try to run a test suite.

`./gradlew build -x test`

# How to create the `credentials.json` file
To programmatically access Google Sheets, a credentials.json file is required to authenticate and authorize the 
application. This file contains information and credentials that allow the application to access the Google Sheets API
on behalf of the user or the service account.

For the Generator, you need read-only access; for the translator, since we create a Google Sheet with the
agent's phrases, you need read/write access.

> Please note that this file is a gateway that enables access to Google Sheets, so be sure to **not** commit
it to source control!

Instructions [here.](https://developers.google.com/workspace/guides/configure-oauth-consent)

# Running the Agent Generator
Note that unless you are using a sheet that is already created, you will need to create your own Google Sheet.

Main class: `io.nuvalence.cx.tools.cxagent.MainKt`

```
cd cx-agent
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* Your Dialogflow project ID (the numeric project id)
* Directory where the agent and agent.zip should be created
* URL where to find the `credentials.json` file granting access to the Google Sheet above

# Running the Export / Import

Main class: `io.nuvalence.cx.tools.phrases.MainKt`

```
cd cx-phrases
../gradlew run --args="<arguments as described below>"
```

## Export
**Arguments**:
* The word `export`
* Google Sheet ID (see above)
* Path to where the exploded agent is (i.e. download agent.zip and unzip it)
* URL where to find the `credentials.json` file granting access to the Google Sheet above

## Import
**Arguments**:
* The word `import`
* Google Sheet ID (see above)
* Path to where the exploded agent is
* Path to where the resulting agent should be created (**must be an empty directory**,
we make no assumption whether we can go ahead and delete things...) 
* URL where to find the `credentials.json` file granting access to the Google Sheet above

# Running Tests
Please see cx-test README.md for instructions.

# Other Tools
Please refer to the README.md files under `cx-large` and `cx-variations` for additional tools.

# Contributors
* https://github.com/aantenangeli
* https://github.com/gteng-nuvalence
* https://github.com/nuvalencenate
