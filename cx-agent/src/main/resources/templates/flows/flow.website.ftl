{
  "name": "${model.getUuid()}",
  "displayName": "flow.website",
  "transitionRoutes": [ {
    "condition": "$session.params.web-site = null",
    "triggerFulfillment": {

    },
    "targetPage": "no-web-site",
    "name": "${model.getUuid()}"
  }, {
    "condition": "$session.params.web-site != null",
    "triggerFulfillment": {

    },
    "targetPage": "has-web-site",
    "name": "${model.getUuid()}"
  } ],
  <#include "eventHandlers.ftl" parse=true>,
  "nluSettings": {
    "modelType": "MODEL_TYPE_STANDARD",
    "classificationThreshold": 0.30000001
  }
}
