{
  "name": "${model.getUuid()}",
  "displayName": "no-web-site",
  "form": {

  },
  "entryFulfillment": {
    "messages": [ {
      "text": {
        "text": [ "${model.smalltalks.fulfillment("website-missing")}" ]
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
