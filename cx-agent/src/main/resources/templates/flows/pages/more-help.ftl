{
  "name": "${model.getUuid()}",
  "displayName": "more-help",
  "form": {

  },
  "entryFulfillment": {
    "messages": [ {
      "text": {
        "text": [ "${model.smalltalks.fulfillment("more-help")}" ]
      },
      "languageCode": "en"
    } ]
  },
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {
      "setParameterActions": [ {
        "parameter": "question-type",
        "value": "faq-question"
      } ]
    },
    "targetFlow": "flow.routing",
    "name": "${model.getUuid()}"
  } ]
}