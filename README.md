# Documentation

You can find it [here.](https://docs.google.com/document/d/1nCaHwozTwkgax9N5siI7DvcW7FRELL-InAxOYApc4Dw/edit#heading=h.cf31udh89iqe)

# Building
Please run the build command without executing tests, as cx-tests will try to run a test suite.

# How to create the `credentials.json` file

Instructions [here.](https://developers.google.com/workspace/guides/configure-oauth-consent)

# Running the Agent Generator

Main class: `io.nuvalence.cx.tools.cxagent.MainKt`

Parameters:
* Google Sheet ID - this string here: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* Your Dialogflow project ID (e.g. 291878529993)
* Directory where the agent and agent.zip should be created
* URL where to find the `credentials.json` file granting access to the Google Sheet above

# Running the Export / Import

Main class: `io.nuvalence.cx.tools.phrases.MainKt`

## Export
Parameters:
* The word `export`
* Google Sheet ID (see above)
* Path to where the exploded agent is (i.e. download agent.zip and unzip it)
* URL where to find the `credentials.json` file granting access to the Google Sheet above

## Import
Parameters:
* The word `import`
* Google Sheet ID (see above)
* Path to where the exploded agent is
* Path to where the resulting agent should be created (must be an empty directory,
we make no assumption whether we can go ahead and delete things...) 
* URL where to find the `credentials.json` file granting access to the Google Sheet above

# Running Tests
Please see cx-test README.md for instructions.