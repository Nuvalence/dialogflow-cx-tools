ChatGPT Intent Generator
========================

This tool uses ChatGPT to create training phrases based on responses. It combines
`cx-suggestions` and `cx-variations` to automatically generate intents based on
a single question and answer. It also takes a spreadsheet with the same format as
the Agent Generator expects, and it adds a new tab - `Questions - Generated` with
the generated training phrases.

The process is as follows: first, it calls ChatGPT to generate variations on the
single question from the input spreadsheet. Then it uses the variations and the
response to ask ChatGPT to generate the final list of questions. This process was
determined to produce the best results based on experimentation.

# Running the Tool

Main class: `io.nuvalence.cx.tools.intentgen.MainKt`

```
cd cx-suggestions
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* URL where to find the `credentials.json` file granting access to the Google Sheet above
* A ChatGPT API Key - the generator will call ChatGPT to generate training phrases variations
* The question you want to ask ChatGPT to generate the first set of variations
* The question you want to ask ChatGPT to generate the intents

For the first question, you may pass something along these lines:

> "Provide 3 different ways to ask the following question: [question]"

Where `[question]` will be replaced with the question from the input spreadsheet.

For the second question, you may pass something like this:

> "I am building a conversational virtual agent. I have these frequently 
> asked questions of "[question]".  Given the below intent response to these
> questions, can you give me 15 alternative training phrases that can be used 
> to train our dialogflow agent given this response: "

`[question]` is replaced with the generated variations from question 1. The
answer to the question is attached to the end of the second questions to produce
the final list of intents. You can and you should be very specific with the second
question, to give ChatGPT a good context to generate meaningful intents. For
example, you can tell ChatGPT the type of user you expect will be accessing the
virtual agent, what the virtual agent is about, etc.

We use a different tab (instead of in-place changes) to give you an opportunity to
review what ChatGPT generated and make adjustments, and we strongly recommend that
you do so.