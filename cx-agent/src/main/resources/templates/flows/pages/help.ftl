{
  "name": "${model.getUuid()}",
  "displayName": "help",
  "form": {

  },
  "entryFulfillment": {
    "messages": [ {
      "text": {
        "text": [ "${model.smalltalks.fulfillment("help")}" ]
      },
      "languageCode": "en"
    } ]
  },
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {
      "setParameterActions": [ {
        "parameter": "user-request-fwd",
        "value": "generic.smalltalk.more-help"
      }, {
        "parameter": "web-site-fwd",
        "value": null
      }, {
        "parameter": "question-type",
        "value": "follow-up"
      } ]
    },
    "targetFlow": "flow.routing",
    "name": "${model.getUuid()}"
  } ]
}