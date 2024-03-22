# CX-TEST
This module facilitates the running of test cases built within the Dialogflow CX console and outputs the results to a 
Google Sheet, aiming to bridge the gap in immediate feedback regarding fulfillment language discrepancies. As Dialogflow 
CX currently lacks a streamlined way to identify unexpected language variations in fulfillments, this tool serves a 
crucial role in enhancing testing efficiency by generating outputs of test cases into a Google Sheet. The resulting 
Google Sheet can be used to aid in the quick identification of unexpected language differences and can be leveraged as 
testing scripts for manual testers. 

Additionally, leveraging the test outcomes documented in the Google Sheet, users can further refine test metadata, 
including names, tags, and notes, and seamlessly upload these updates back into the Dialogflow agent. For more 
information about updating test metadata, see our [cx-test-sync readme](../cx-test-sync/README.md).

# Table of Contents
1. [Introduction](#cx-test)
2. [Test Suites](#test-suites)
   - [End-to-End Tests](#end-to-end-tests)
   - [Smoke Tests](#smoke-tests)
   - [Dialogflow CX Test Cases](#dialogflow-cx-test-cases)
3. [Setup](#setup)
   - [Authentication](#authentication)
   - [Properties File](#properties-file)
4. [JAR Packaging](#jar-packaging)
5. [Test Execution](#test-execution)
6. [Properties](#properties)

## Test Suites
There are different types of tests that can be run using this module. The properties file dictates which types of tests 
to run as well as any configuration for the test run. The different types of tests are as follows:

### End-to-End Tests
[ADD E2E DETAILS HERE]

### Smoke Tests
[ADD SMOKE DETAILS HERE]

### Dialogflow CX Tests
These are tests built in the Dialogflow CX Test Cases console. This module will leverage the DFCX APIs to invoke the tests built in
DFCX. These tests can be filtered using tags. The result will output to a Google Sheet that can be leveraged as scripts 
when manually testing the agent.

## Setup

### Authentication

#### Google Sheets

To obtain the necessary credentials for Google Sheets, see our
[Google Sheets documentation](../documentation/google-sheets.md).

### Dialogflow CX API

#### Local Authentication

To log in with the gcloud CLI, see our
[Local Authentication documentation](../documentation/local-authentication.md).

### Properties File

1. Create a file suffixed with `.properties`. Place it where desired and take note of the path.
   - For example, a file `default.properties` may be created in `cx-test/src/main/resources/`.
   - A properties template file can be found [here](./src/main/resources/template.properties).
2. See [here](#properties) for a list of properties to include in the file.

## JAR Packaging

Run the following command from project root:

```
./gradlew cx-test:shadowJar
```

This will generate a JAR file in `cx-test/build/libs/` with the following name:

```
cx-test-<version>-all.jar
```

You are then free to move this JAR file to a desired location.

## Test Execution

### Via Gradle

Run the following command from project root:

```
./gradlew cx-test:run --args <relative path to properties file from project root>
```

For example, if the properties file is located at `cx-test/src/main/resources/default.properties`, the command would be:

```
./gradlew cx-test:run --args src/main/resources/default.properties
```

### Via JAR

Alternatively, if you are running the JAR file directly, run the following command from the directory containing the JAR file:

```
java -jar <JAR file name> <relative path from current working directory>
```

For example, if:
  * The JAR file is located at `cx-test/build/libs/cx-test-1.0.0-all.jar`
  * The properties file is located at `cx-test/src/main/resources/default.properties`, and
  * Your current working directory is the project root

The command would be:

```
java -jar cx-test-1.0.0-all.jar src/main/resources/default.properties
```

## Properties

The following are properties that can be specified in a properties file for use by the test suite. (See: [Properties File](#properties-file)).
A properties template file can be found [here](./src/main/resources/template.properties).
Note that whether a property is "required" or not determines whether it needs to be specified in the properties file. A property may be specified but not populated, in which case it is treated as an empty string.

| Property               | Description                                                                                                                                                                                                                                                                                                                                                                   | Usage                                       | Required | Default                                                 |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|----------|---------------------------------------------------------|
| Credentials URL        | URL referencing the desired credentials file for access to the Google Sheets API. This can be a local file, indicated by the file URL protocol (file:///). To obtain the necessary credentials, see [here](#google-sheets).                                                                                                                                                   | `credentialsUrl="<credentials URL>"`        | Yes      | N/A                                                     |
| Agent Path             | A portion of the URL specifying the path to the desired agent under test. The agent path format is as follows:`projects/<project name>/locations/<location>/agents/<agent UUID>`.                                                                                                                                                                                             | `agentPath="<agent path>"`                  | Yes      | N/A                                                     |
| Spreadsheet ID         | The ID for your desired spreadsheet on Google Sheets. This can be derived from the URL as follows: `https://docs.google.com/spreadsheets/d/<spreadsheetId>`.                                                                                                                                                                                                                  | `spreadsheetId="<spreadsheet ID>"`          | No       | N/A (Required if includeTags includes `e2e` or `smoke`) |
| Dialogflow CX Endpoint | The service endpoint to use for Dialogflow CX sessions. This must correspond with the region specified in the agent path.  See [here](https://cloud.google.com/dialogflow/cx/docs/reference/rest/v3beta1-overview#service-endpoint) for a list of available endpoints. These will typically be suffixed with port 443.                                                        | `dfcxEndpoint="<endpoint URL>"`             | No       | `dialogflow.googleapis.com:443`                         |
| Matching Ratio         | Determines the percentage matching threshold to use for considering agent responses as matching expectations.                                                                                                                                                                                                                                                                 | `matchingRatio=[<number>]`                  | No       | `80`                                                    |
| Matching Mode          | Determines how agent response strings are matched. <ul><li>Normal: Matches the full agent response against the full expected response wholesale</li><li>Adaptive: Matches the agent response against the expected response line by line</li></ul>                                                                                                                             | `matchingMode=[normal, adaptive]`           | No       | `normal`                                                |
| Orchestration Mode     | Determines how to run tests generated from the spreadsheet. <ul><li>Simple: Takes only the first message from all user inputs in a given scenario</li><li>Comprehensive: Tests every possible message for all user inputs at least once</li></ul>                                                                                                                             | `orchestrationMode=[simple, comprehensive]` | No       | `simple`                                                |
| No-Date Match          | Determines whether disparate agent fulfillments should be matched with (dynamic) dates removed. New date formats and language codes may be added in code as needed. <ul><li>True: Matches the agent response against the expected response with dates removed from both</li><li>False: Matches the full agent response against the full expected response wholesale</li></ul> | `noDateMatch=[true, false]`                 | No       | `false`                                                 |
| Include Tags           | Determines which tests should be run, based on their tags. <ul><li>e2e: End-to-end tests</li><li>smoke: Smoke tests</li><li>dfcx: DFCX Test Builder tests</li></ul>                                                                                                                                                                                                           | `includeTags="e2e\|smoke\|dfcx"`            | No       | `"dfcx"`                                                |
| Exclude Tags           | Determines which tests should not be run, based on their tags. <ul><li>e2e: End-to-end tests</li><li>smoke: Smoke tests</li><li>dfcx: DFCX Test Builder tests</li></ul>                                                                                                                                                                                                       | `excludeTags="e2e\|smoke\|dfcx"`            | No       | `"e2e\|smoke"`                                          |
| DFCX Tag Filter        | Determines which tests should be run, based on the associated tags on the test. Used exclusively for the DFCX Test Builder Spec. <ul><li>`"ALL"`: runs all tests associated with the agent</li><li>`"#Filter1,#Filter2,..."`: runs all tests that include all tags contained within the comma-separated expression</li></ul>                                                  | `-dfcxTagFilter="#Filter1,#Filter2"`        | No       | `"ALL"`                                                 |
| Test Package           | Classpath for files containing test cases. This must be a fully qualified package name.                                                                                                                                                                                                                                                                                       | `testPackage="package.name"`                | No       | `io.nuvalence.cx.tools.cxtest`                          |
