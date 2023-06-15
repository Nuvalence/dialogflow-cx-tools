Large Agent Generator
=====================

This is a simple utility to generate an agent with a large number of flows. This
was used to test the impact of increasing the number of flows above the default
limit of 50. It will create the Questions tab as expected by the Agent Generator,
so you can add the Smalltalk tab to the spreadsheet and use it to create a working
Agent.

# Running the Large Agent Generator

Main class: `io.nuvalence.cx.tools.large.MainKt`

```
cd cx-large
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* URL where to find the `credentials.json` file granting access to the Google Sheet above
* A ChatGPT API Key - the generator will call ChatGPT to generate training phrases
variations - total of 5.

Note that calls to ChatGPT slow down the generation process.
