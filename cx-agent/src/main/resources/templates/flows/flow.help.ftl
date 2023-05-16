{
  "name": "${model.getUuid()}",
  "displayName": "flow.help",
  "transitionRoutes": [ {
    "condition": "$session.params.user-request = \"generic\\.smalltalk\\.help\"",
    "triggerFulfillment": {

    },
    "targetPage": "help",
    "name": "${model.getUuid()}"
  }, {
    "condition": "$session.params.user-request = \"generic\\.smalltalk\\.more-help\"",
    "triggerFulfillment": {

    },
    "targetPage": "more-help",
    "name": "${model.getUuid()}"
  } ],
  <#include "eventHandlers.ftl" parse=true>,
  "nluSettings": {
    "modelType": "MODEL_TYPE_ADVANCED",
    "classificationThreshold": 0.30000001,
    "modelTrainingMode": "MODEL_TRAINING_MODE_AUTOMATIC",
    "multiIntentSettings": {

    }
  },
  "advancedSettings": {
    "enabled": true
  }
}
