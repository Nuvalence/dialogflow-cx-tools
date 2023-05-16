{
  "name": "${model.getUuid()}",
  "displayName": "welcome-message",
  "form": {

  },
  "entryFulfillment": {
    "messages": [ {
      "text": {
        "text": [ "${model.smalltalks.fulfillment("Welcome Message")}" ]
      },
      "languageCode": "en"
    } ]
  },
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {
      "setParameterActions": [ {
        "parameter": "web-site",
        "value": null
      }, {
        "parameter": "user-request",
        "value": "generic.smalltalk.help"
      }, {
        "parameter": "question-type",
        "value": "faq-question"
      } ]
    },
    "targetFlow": "flow.routing",
    "name": "${model.getUuid()}"
  } ]
}
