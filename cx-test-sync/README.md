# CX-TEST-SYNC

This is a test synchronization tool for use in tandem with cx-test for the UI CCAI project.

## Setup

### Authentication

#### Google Sheets

To obtain the necessary credentials for Google Sheets:

1. Login to Google with an account that has permissions to access your desired spreadsheet(s).
2. Go to the [Google Cloud Console](https://console.cloud.google.com/).
3. Navigate to the APIs & Services Dashboard by clicking on the menu icon in the top-left corner and selecting "Cloud Overview" -> "Dashboard" -> "Explore and enable APIs"
4. Navigate to the Google Sheets API details page by clicking on "Google Sheets API"
5. Click on "Credentials" on the navigation bar to reveal the credentials menu.
6. If you do not already have an OAuth client ID available, click on the "+ Create Credentials" button and select "OAuth Client ID" from the dropdown menu.
    - Fill in the required information for the OAuth client, such as application type and client name.
    - Click the "Create" button. This will generate a new service account and download a JSON file containing the credentials.
7. If you already have an OAuth client ID available, you can click the download button under the "Actions" column, corresponding to your desired client ID.

### Dialogflow CX API

#### Local Authentication

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

#### CI Authentication

TBD

### Properties File

1. Create a file suffixed with `.properties`. Place it where desired and take note of the path.
- For example, a file `default.properties` may be created in `cx-test-sync/src/main/resources/`.
  - A properties template can be found in `cx-test-sync/src/main/resources/template.properties`.
2. See [here](#properties) for a list of properties to include in the file.

## JAR Packaging

Run the following command from project root:

```
./gradlew cx-test-sync:shadowJar
```

This will generate a JAR file in `cx-test-sync/build/libs/` with the following name:

```
cx-test-sync-<version>-all.jar
```

You are then free to move this JAR file to a desired location.

## Test Execution

### Via Gradle

Run the following command from project root:

```
./gradlew cx-test-sync:run --args <relative path to properties file from project root>
```

For example, if the properties file is located at `cx-test-sync/src/main/resources/default.properties`, the command would be:

```
./gradlew cx-test-sync:run --args src/main/resources/default.properties
```

### Via JAR

Alternatively, if you are running the JAR file directly, run the following command from the directory containing the JAR file:

```
java -jar <JAR file name> <relative path from current working directory>
```

For example, if:
* The JAR file is located at `cx-test-sync/build/libs/cx-test-sync-1.0.0-all.jar`
* The properties file is located at `cx-test-sync/src/main/resources/default.properties`, and
* Your current working directory is the project root

The command would be:

```
java -jar cx-test-sync-1.0.0-all.jar src/main/resources/default.properties
```

## Export Agent

Test execution will generate the zip file of agent before applying diffs. The zip file default will be located in project root
as agent.zip

## Properties

The following are properties that can be specified in a properties file for use by the test suite. (See: [Properties File](#properties-file)

### Credentials URL

URL referencing the desired credentials file for access to the Google Sheets API. This can be a local file, indicated by the file URL protocol (`file:///`).

To obtain the necessary credentials, see [here](#google-sheets).

- Usage: `credentialsUrl="<credentials URL>"`
- Required: Yes

### Agent Path

A portion of the URL specifying the path to the desired agent under test. The agent path format is as follows:

```
projects/<project name>/locations/<location>/agents/<agent UUID>
```

- Usage: `agentPath="<agent path>"`, e.g. `agentPath="projects/project/locations/global/agents/00000000-0000-0000-0000-000000000000"`
- Required: Yes

### Spreadsheet ID

The ID for your desired spreadsheet on Google Sheets. This can be derived from the URL as follows:

```
https://docs.google.com/spreadsheets/d/<spreadsheetId>
```

- Usage: `spreadsheetId="<spreadsheet ID>"`
- Required: No UNLESS Include Tags includes `e2e` or `smoke`

### Dialogflow CX Endpoint

The service endpoint to use for Dialogflow CX sessions.

This must correspond with the region specified in the agent path.  See [here](https://cloud.google.com/dialogflow/cx/docs/reference/rest/v3beta1-overview#service-endpoint) for a list of available endpoints. These will typically be suffixed with port 443.

Defaults to us-east-1 if not specified.

- Usage: `dfcxEndpoint="<endpoint URL>"`, e.g. `dfcxEndpoint="us-east1-dialogflow.googleapis.com:443"`
- Required: No
- Default if not supplied: `dialogflow.googleapis.com:443`

### Export Agent Path

The path to export agent before applying diffs

Defaults to root folder of the module

- Usage: `exportAgentPath="<provide absolute path>"`, e.g. `exportAgentPath="/Users/<USERNAME>/dol/dialogflow-cx-tools/cx-test-sync/src/main/resources/export/"`
- Required: No
- Default if not supplied: `/Users/<USERNAME>/dol/dialogflow-cx-tools/cx-test-sync/`
