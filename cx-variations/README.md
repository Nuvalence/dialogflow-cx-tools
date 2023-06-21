ChatGPT Generated Training Phrases
==================================

This tool uses ChatGPT to create variations of training phrases, so you can
better train your agent. It takes a spreadsheet with the same format as the
Agent Generator expects, and it adds a new Tab - `Questions - Variations` -
that contains the ChatGPT generated variations to your training phrases.

# Running the Tool

Main class: `io.nuvalence.cx.tools.variations.MainKt`

```
cd cx-variations
../gradlew run --args="<arguments as described below>"
```

**Arguments**:
* Google Sheet ID - the string between `/d/` and `/edit#` from your Sheet URL: docs.google.com/spreadsheets/d/<mark>1vxyvOCGqh_382_ZpEWcI1rGLjzjJa4pRRXM64KjcTxU</mark>/edit#gid=1799424559
* URL where to find the `credentials.json` file granting access to the Google Sheet above
* A ChatGPT API Key - the generator will call ChatGPT to generate training phrases variations
* The question you want to ask ChatGPT to generate the new training phrases

The question mentioned above should follow a format similar to this:

"Provide 5 different ways to ask the following question: [question]"

[question] will be replaced with each of your training phrases. For example, if you
have 10 training phrases, the question above will generate an additional 5 * 10 = 50
training phrases.

We use a different tab (instead of in-place changes) to give you an opportunity to
review what ChatGPT generated and make adjustments. 

If your original training phrase cell contained this:

```
```

The new cell will contain something like this:

```
Am I eligible?
Can I apply?

1. Are my qualifications sufficient for eligibility?
2. Can I be accepted?
3. Do my qualifications make me suitable?
4. Am I allowed to apply?
5. Am I able to qualify?


1. Is it possible for me to apply?
2. Could I submit an application?
3. Am I able to apply?
4. Is there an opportunity to apply?
5. Do you have applications available?
```

As you can see, the ChatGPT generated phrases are clearly identified to make it
easier for you to review them. We strongly recommend that you review the variations
created by ChatGPT.
