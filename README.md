# Agent Essentials for CCAI
The Nuvalence Agent Essentials for CCAI are accelerators that automate mundane, 
manual tasks related to creating and maintaining CCAI/Dialogflow agents:

1. Restoring the CCAI Agent from local files.
2. Supporting translations and proper prosody for URLs, numbers, etc.
3. Running CCAI Agent tests. 
4. Generating an initial cut of an Agent capable of answering questions. (Deprecated)

You can find detailed documentation in each module of the agent.

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

# CX-AGENT
## Agent Zip-Restore
Creates a zip file of the local CCAI JSON files and restores the specified CCAI Agent with that zip file.

For more information, see the [CX-AGENT README](./cx-agent/README.md)

## Agent Generator (deprecated)
Generates agent files from a source Google Sheet with intents and fulfillments. This leverage data collection patterns 
that are no longer suggested and is therefore *deprecated*.

For more information, see the [CX-AGENT README](./cx-agent/README.md)

# CX-INTENTGEN
This tool uses ChatGPT to create training phrases based on responses. It combines
`cx-suggestions` and `cx-variations` to automatically generate intents based on
a single question and answer.

For more information, see the [CX-INTENTGEN README](./cx-intentgen/README.md)

# CX-LARGE
This is a simple utility to generate an agent with a large number of flows. Primarily used for testing Flow limits.

For more information, see the [CX-LARGE README](./cx-large/README.md)

# CX-PHRASES
## Running the Export / Import


# Running Tests
Please see cx-test README.md for instructions.

# Other Tools
Please refer to the README.md files under `cx-large` and `cx-variations` for additional tools.

# Contributors
* https://github.com/aantenangeli
* https://github.com/gteng-nuvalence
* https://github.com/nuvalencenate