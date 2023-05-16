{
  "name": "${model.getUuid()}",
  "displayName": "has-web-site",
  "form": {

  },
  "entryFulfillment": {
    "messages": [ {
      "text": {
        "text": [ "${model.smalltalks.fulfillment("website")}" ]
      },
      "languageCode": "en"
    } ]
  },
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {

    },
    "targetFlow": "flow.what-else",
    "name": "${model.getUuid()}"
  } ]
}
