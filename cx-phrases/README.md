# CX-PHRASES
This module includes the `export` and `import` functionality used to update agent fulfillments, including for translations, and generate
SSML for the fulfillments. The Agent can first be exported to a Google Sheet, modified, and imported back into the 
expected Agent project structure consisting of JSON files.

# Table of Contents
1. [Introduction](#cx-phrases)
2. [Setup](#setup)
3. [Agent Export](#agent-export)
    - [Running the Agent Export](#running-the-agent-export)
4. [Agent Import](#agent-import)
    - [Running the Agent Import](#running-the-agent-import)
    - [SSML Processing](#ssml-processing)
    - [SSML Configuration](#ssml-configuration)
5. [Running Tests](#running-tests)

## Setup
### Authentication
#### Google Sheets
To obtain the necessary credentials for Google Sheets, see our 
[Google Sheets documentation](../documentation/google-sheets.md).

### Dialogflow CX API
#### Local Authentication
To log in with the gcloud CLI, see our
[Local Authentication documentation](../documentation/local-authentication.md).

## Agent Export
The purpose of the Agent Export process is to transfer all the Agent’s messages and training phrases to a Google Sheet.
This Google Sheet can then be used by the business to modify the training phrases and messages by making direct changes
in the sheet. After the changes have been made, the Google Sheet can the be used to rebuild the Agent's JSON files
through the import process.

### Running the Agent Export
Main class: `io.nuvalence.cx.tools.phrases.MainKt`

From the root directory:
```
./gradlew cx-phrases:run --args="<arguments as described below>"
```

#### Arguments
| Argument # | Argument Description                                                                                                                                                                                                          | Argument Required | Argument Example                              |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|-----------------------------------------------|
| 1         | The specified function to run the agent export process. This should be `export`.                                                                                                                                              | true              | `export`                                      |
| 2         | Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: <br> docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559                                | true              | `1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU` |
| 3         | The full path to a local agent directory to export                                                                                                                                                                            | true              | `/Users/usr/source/agent`                     |
| 4         | URL referencing the desired credentials file for access to the Google Sheets API. This can be a local file, indicated by the file URL protocol (`file:///`). To obtain the necessary credentials, see [here](#google-sheets). | true              | `file:///Users/usr/credentials/gcreds.json`   |
      
#### Example
```
./gradlew cx-phrases:run --args="export \
1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU \
/Users/usr/source/agent \
file:///Users/usr/credentials/gcreds.json"
```

## Agent Import
The purpose of the Agent Import process is to rebuild the Agent's JSON files from an Agent that was previously exported
to a Google Sheet. This process not only allows users to update fulfillments and training phrases, but to also generate
SSML for the fulfillments as well. The SSML generation rules can be configured via a `.conf` file.

### Running the Agent Import
Main class: `io.nuvalence.cx.tools.phrases.MainKt`

From the root directory:
```
./gradlew cx-phrases:run --args="<arguments as described below>"
```

#### Arguments
| Argument # | Argument Description                                                                                                                                                                                                          | Argument Required | Argument Example                               |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|------------------------------------------------|
| 1          | The specified function to run the agent import process. This should be `import`.                                                                                                                                              | true              | `import`                                       |
| 2          | Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: <br> docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559                                | true              | `1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU` |
| 3          | The full path to a local agent directory to merge the exported agent Google Sheet to the existing agent source JSON files.                                                                                                    | true              | `/Users/usr/source/agent`                      |
| 4          | The full path to a local directory to create the output of the JSON agent files as well as a zipped version.                                                                                                                  | true              | `/Users/usr/source/agent/import`               |
| 5          | URL referencing the desired credentials file for access to the Google Sheets API. This can be a local file, indicated by the file URL protocol (`file:///`). To obtain the necessary credentials, see [here](#google-sheets). | true              | `file:///Users/usr/credentials/gcreds.json`    |

#### Example
```
./gradlew cx-phrases:run --args="import \
1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU \
/Users/usr/source/agent \
/Users/usr/source/agent/import \
file:///Users/usr/credentials/gcreds.json"
```

### SSML Processing
SSML (Speech Synthesis Markup Language) is used to customize how fulfillments are pronounced by the Dialogflow CX Agent.
For more information on the different SSML elements, check out the 
[Google Documentation](https://cloud.google.com/text-to-speech/docs/ssml).

We process a number of token types to surround with SSML tags in the Output Audio Text section of an agent fulfillment. 
These include:
- URLs
- phone numbers
- percentages
- numbers

We've defined some custom rules as well:
- letter sequences that don't make sense in English
- tokens that we specifically want to pronounce versus spell out
- custom matching rules to replace very specific strings with desired SSML 

These are all done using regexes or matching within a list, and therefore are overridable and customizable.

### SSML Configuration
Regexes, URL separators, and an allowlist of short tokens can be found in the [defaults.conf](./defaults.conf) file. Tokens in a fulfillment phrase
that match these items will be surrounded with appropriate ssml tags during import. Short tokens appearing in the allowlist will be read
verbatim. To overwrite these values, add custom values to the `config.conf` file as shown in the example `.conf` file below:

```
{
    include required(file("cx-phrases/defaults"))
    
    # Override values specified in defaults.conf by defining a custom value like below:
    SHORT_TOKEN_WHITELIST: [ "com", "org", "net" ]

    # If you want to skip processing a certain type of token, override the value as an empty set/string:
    URL_VERBATIM_TOKENS: []
    MATCH_URL_REGEX: ""

    # Custom match rules can be defined using regexes and replacements in the CUSTOM_MATCH_REPLACE_LIST", along with language code values
    # to indicate the languages in which to perform the match/replace:
    CUSTOM_MATCH_REPLACE_LIST: [ 
       {
            languages: "en",
            match: "^.*$",
            replace: "hello"
        }, {
            languages: "en,es",
            match: "^.*$",
            replace: "hola"
        }, {
            languages: "all",
            match: "^.*$",
            replace: ":)"
        } 
    ]
}
```

## Running Tests
If running tests through Junit/IntelliJ, you need to specify the working directory in the test's run configuration. The default is `$MODULE_WORKING_DIR`,
but using this will lead to an error when it can't find the `.conf` files required. Specify `$ProjectFileDir$` instead and it will run from the directory
up. 