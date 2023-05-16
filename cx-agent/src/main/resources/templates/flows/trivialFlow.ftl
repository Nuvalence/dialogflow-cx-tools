{
  "name": "${model.getUuid()}",
  "displayName": "flow.${displayName}",
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {

    },
    "targetPage": "${displayName}-message",
    "name": "${model.getUuid()}"
  } ],
  <#include "eventHandlers.ftl" parse=true>,
  "nluSettings": {
    "modelType": "MODEL_TYPE_ADVANCED",
    "classificationThreshold": 0.30000001,
    "modelTrainingMode": "MODEL_TRAINING_MODE_AUTOMATIC"
  }
}