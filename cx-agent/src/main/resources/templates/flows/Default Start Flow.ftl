{
  "name": "00000000-0000-0000-0000-000000000000",
  "displayName": "Default Start Flow",
  "description": "A start flow created along with the agent",
  "transitionRoutes": [ {
    "intent": "Default Welcome Intent",
    "triggerFulfillment": {

    },
    "targetPage": "welcome-message",
    "name": "${model.getUuid()}"
  } ],
<#include "eventHandlers.ftl" parse=true>,
  "nluSettings": {
    "modelType": "MODEL_TYPE_ADVANCED",
    "classificationThreshold": 0.30000001,
    "modelTrainingMode": "MODEL_TRAINING_MODE_AUTOMATIC"
  }
}
