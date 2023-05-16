# CX-TEST

This is a test suite setup for Dialogflow CX testing for the UI CCAI project.

## Test Execution

Run the following command:

```
./gradlew build --info \
    -DcredentialsUrl="<credentials URL>" \
    -DagentPath="<agent path, e.g. projects/prospect-dol-ccai/locations/global/agents/8c8b3d82-3b05-4726-8991-06f7b2fd4204>" \
    -DspreadsheetId="<spreadsheet ID, e.g. 14880YAMX8JQ3DP0rdiZfbt3oBbYVKL5Bl_244zwDPVM>"
```

### Optional Arguments

#### Matching Ratio

Determines the percentage matching threshold to use to consider agent responses sufficiently matching expectations.

- Usage: `-DmatchingRatio=[<number>]`
- Default if not supplied: `80`


#### Matching Mode

Determines how agent response strings are matched.

- Options:
  - Normal: Matches the full agent response against the full expected response wholesale
  - Adaptive: Matches the agent response against the expected response line by line
- Usage: `-DmatchingMode=[normal, adaptive]`
- Default if not supplied: `normal`

#### Orchestration Mode

Determines how to run tests generated from the spreadsheet.

- Options:
  - Simple: Takes only the first message from all user inputs in a given scenario
  - Comprehensive: Tests every possible message for all user inputs at least once
- Usage: `-DorchestrationMode=[simple, comprehensive]`
- Default if not supplied: `simple`
