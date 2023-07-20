# CX-TEST

This is a test suite setup for Dialogflow CX testing for the UI CCAI project.

## Test Execution

Run the following command:

```
./gradlew build --info \
    -PcredentialsUrl="<credentials URL>" \
    -PagentPath="<agent path, e.g. projects/projectName/locations/global/agents/agentUUID>" \
    -PspreadsheetId="<Google Docs spreadsheet ID>"
```

### Required Arguments

#### Credentials URL

URL referencing the desired credentials file for access to the Google Sheets API.

To obtain the necessary credentials:

1. Go to the Google Cloud Console.

2. Navigate to the APIs & Services Dashboard by clicking on the menu icon in the top-left corner and selecting "Cloud Overview" -> "Dashboard" -> "Explore and enable APIs"

3. Navigate to the Google Sheets API details page by clicking on "Google Sheets API"

4. Click on "Credentials" on the navigation bar to reveal the credentials menu.

5. If you do not already have an OAuth client ID available, click on the "+ Create Credentials" button and select "OAuth Client ID" from the dropdown menu.

  - Fill in the required information for the OAuth client, such as application type and client name.

  - Click the "Create" button. This will generate a new service account and download a JSON file containing the credentials.

6. If you already have an OAuth client ID available, you can click the download button under the "Actions" column, corresponding to your desired client ID.

Usage: `-PcredentialsUrl="<credentials URL>"`

#### Agent Path

A portion of the URL specifying the path to the desired agent under test. The agent path format is as follows:

```
projects/<project name>/locations/<location>/agents/<agent UUID>
```

Usage: `-PagentPath="<agent path>"`, e.g. `-PagentPath="projects/project/locations/global/agents/00000000-0000-0000-0000-000000000000"`

#### Dialogflow CX Endpoint

The service endpoint to use for Dialogflow CX sessions.

This must correspond with the region specified in the agent path.  See [here](https://cloud.google.com/dialogflow/cx/docs/reference/rest/v3beta1-overview#service-endpoint) for a list of available endpoints. These will typically be suffixed with port 443.

Defaults to us-east-1 if not specified.

Usage: `-PdfcxEndpoint="<endpoint URL>"`, e.g. `-PdfcxEndpoint="us-east1-dialogflow.googleapis.com:443"`

#### Spreadsheet ID

The ID for your desired spreadsheet on Google Sheets. This can be derived from the URL as follows:

```
https://docs.google.com/spreadsheets/d/<spreadsheetId>
```

Usage: `-PspreadsheetId="<spreadsheet ID>"`

### Optional Arguments

#### Matching Ratio

Determines the percentage matching threshold to use to consider agent responses sufficiently matching expectations.

- Usage: `-PmatchingRatio=[<number>]`
- Default if not supplied: `80`


#### Matching Mode

Determines how agent response strings are matched.

- Options:
  - Normal: Matches the full agent response against the full expected response wholesale
  - Adaptive: Matches the agent response against the expected response line by line
- Usage: `-PmatchingMode=[normal, adaptive]`
- Default if not supplied: `normal`

#### Orchestration Mode

Determines how to run tests generated from the spreadsheet.

- Options:
  - Simple: Takes only the first message from all user inputs in a given scenario
  - Comprehensive: Tests every possible message for all user inputs at least once
- Usage: `-PorchestrationMode=[simple, comprehensive]`
- Default if not supplied: `simple`

### Test Filtering

#### Include Tags

Determines which tests should be run, based on their tags.

- Options:
  - e2e: End-to-end tests.
  - smoke: Smoke tests.
- Usage: `-PincludeTags="e2e|smoke"`
- Default if not supplied: `"e2e|smoke"` (runs both)

#### Exclude Tags

Determines which tests should not be run, based on their tags.

- Options:
  - e2e: End-to-end tests.
  - smoke: Smoke tests.
- Usage: `-PexcludeTags="e2e|smoke"`
- Default if not supplied: nothing
