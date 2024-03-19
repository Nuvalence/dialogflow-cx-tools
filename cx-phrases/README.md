# CX-PHRASES
This module includes the `export` and `import` functionality used to update agent fulfillments, including for translations, and generate
SSML for the fulfillments. The Agent can first be exported to a Google Sheet, modified, and imported back into the 
expected Agent project structure consisting of JSON files.

# Table of Contents
1. [Introduction](#cx-phrases)
2. [Setup](#setup)
3. [Agent Export](#agent-export)
    - [Running the Agent Export](#running-the-agent-export)
    - [Agent Export Google Sheet Structure](#agent-export-google-sheet-structure)
    - [Adding New Languages](#adding-new-languages)
    - [Fulfillment Highlighting](#fulfillment-highlighting)
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

### Agent Export Google Sheet Structure
The export process creates four tabs on a given Google Sheet:

1. Training Phrases: intent names and their associated training phrases.
2. Entities: entity types and values and their synonyms.
3. Transitions: messages associated with transitions.
4. Fulfillments: messages associated with page fulfillments.


#### Training Phrases
This tab contains:
* An **Intent Name** column, with the different intent names.
* A column for each language:
   *   The top cell contains the language code (e.g. en or es) for that column.
   *   The remaining cells contain the training phrases for a given intent/language, one per line.

#### Entities 
This tab contains:
* An **Entity Type** column, with the different entities
* A **Value** column, with associated values to each entity
* A column for each language, with the synonyms for the entity, one per line

#### Transitions
This tab contains:

*   A **Flow Name** column, with the name of the flow to where the transition belongs.
*   A **Page** column, with the name of the page to where the transition belongs.
*   A **Transition Type** column, identifying the type of transition. For example, event when the transition is associated with an **event** like sys.no-input-default.
*   A **Value** column, with the condition or name of the event triggering the transition, such as sys.no-input-default.
*   A **Type** column, with the type of fulfillment associated to the transition, i.e. **message** or **button**.
*   A **Channel** column, with the agent output channel specified, i.e. **audio** or **DF_MESSENGER** for a chatbot implementation.
*   One column per language, as previously described, with the fulfillment message.

#### Fulfillments
This tab contains:

*   A **Flow Name** column, identifying the name of the flow that contains the fulfillment.
*   A **Page Name** column, with the name of the page within the flow that contains the fulfillment.
*   A **Type** column, identifying the type of fulfillment. For example, **message** or **html**.
*   A **Channel** column, with the agent output channel specified, i.e. **audio** or **DF_MESSENGER** for a chatbot implementation.
*   One column per language, as previously described, with the fulfillment message.

#### Adding New Languages
Just add a new column to each of the tabs above, where the topmost cell contains the language code, and the other cells 
the appropriate translations. The Import process will add the new language to the Agent.

#### Fulfillment Highlighting
Because this Agent Export process can be used as a source file for generating associated translations for each 
fulfillment/training phrase, it is important to indicate what parts of the fulfillments/training phrases should **not**
be translated. To accomplish this, text that is <b><span style="color: #0000cc;">highlighted blue and bolded</span></b> indicates that the text should **not** be
translated.

#### Missing Fulfillments
Fulfillments or training phrases that are missing translations for existing language column headers will have its non-
language specific cell values highlighted in red. 

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