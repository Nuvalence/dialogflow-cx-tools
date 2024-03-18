# Table of Contents
1. [Agent Essentials for CCAI](#agent-essentials-for-ccai)
2. [Prerequisites](#prerequisites)
3. [Building](#building)
4. [How to create the `credentials.json` file](#how-to-create-the-credentialsjson-file)
5. [Spotlighted Tools](#spotlighted-tools)
    - [CX-AGENT](#cx-agent)
    - [CX-PHRASES](#cx-phrases)
    - [CX-TEST](#cx-test)
6. [Other Tools](#other-tools)
    - [CX-TEST-SYNC](#cx-test-sync)
    - [CX-INTENTGEN](#cx-intentgen)
    - [CX-SUGGESTIONS](#cx-suggestions)
    - [CX-VARIATIONS](#cx-variations)
    - [CX-LARGE](#cx-large)

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

# Spotlighted Tools
## CX-AGENT
Can be used to create a zip file of the local CCAI Agent JSON files and restores the specified Agent with that zip file.

This also includes a `deprecated` Agent Generator to generate Agent JSON files from a source Google Sheet.

For more information, see the [CX-AGENT README](./cx-agent/README.md)

## CX-PHRASES
Includes `export` and `import` functionality used to update agent fulfillments, including translations, and generate
SSML for the fulfillments.

For more information, see the [CX-PHRASES README](./cx-phrases/README.md)

## CX-TEST
Runs test cases and outputs the results to a Google Sheet.

For more information, see the [CX-TEST README](./cx-test/README.md)

# Other Tools
## CX-TEST-SYNC
Updates CCAI Agent Test Cases from a Google Sheet import. Used to update test metadata such as the test name, tags, and
descriptions.

For more information, see the [CX-TEST-SYNC README](./cx-test-sync/README.md)

## CX-INTENTGEN
This tool uses ChatGPT to create training phrases based on responses. It combines
`cx-suggestions` and `cx-variations` to automatically generate intents based on
a single question and answer.

For more information, see the [CX-INTENTGEN README](./cx-intentgen/README.md)

## CX-SUGGESTIONS
This tool uses ChatGPT to create training phrases based on responses.

For more information, see the [CX-SUGGESTIONS README](./cx-suggestions/README.md)

## CX-VARIATIONS
This tool uses ChatGPT to create variations of training phrases, so you can
better train your agent.

For more information, see the [CX-VARIATIONS README](./cx-variations/README.md)

## CX-LARGE
This is a simple utility to generate an agent with a large number of flows. Primarily used for testing Flow limits.

For more information, see the [CX-LARGE README](./cx-large/README.md)