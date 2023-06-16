ChatGPT Generated Training Phrases
==================================

This tool uses ChatGPT to create training phrases based on responses. It takes 
a spreadsheet with the same format as the Agent Generator expects, and it adds
a new Tab - `Questions - Suggestions` - that contains the ChatGPT generated 
questions to your answers.

# Running the Tool

Main class: `io.nuvalence.cx.tools.suggestions.MainKt`

```
cd cx-suggestions
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* URL where to find the `credentials.json` file granting access to the Google Sheet above
* A ChatGPT API Key - the generator will call ChatGPT to generate training phrases variations
* The question you want to ask ChatGPT to generate training phrases

The answer will be attached to the end of the question. An example question you
may pass as a parameter is as follows:

> I am building a conversational virtual agent. Given the below response, 
> give me all the question someone might ask that should trigger this response:

The response will be attached to the question above.

We use a different tab (instead of in-place changes) to give you an opportunity to
review what ChatGPT generated and make adjustments, and we strongly recommend that
you do so.