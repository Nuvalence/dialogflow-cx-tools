{
  "name": "${model.getUuid()}",
  "displayName": "flow.repeat",
  "transitionRoutes": [ {
      "condition": "$session.params.user-request : \"generic.smalltalk\"",
      "triggerFulfillment": {

      },
      "targetFlow": "flow.help",
      "name": "${model.getUuid()}"
    }, <#list model.flowGroups as flowGroup>{
      "condition": "$session.params.user-request : \"${flowGroup.escapedDisplayName()}\"",
      "triggerFulfillment": {

      },
      "targetFlow": "${flowGroup.displayName}",
      "name": "${model.getUuid()}"
    }<#sep>, </#list>
  ],
  <#include "eventHandlers.ftl" parse=true>,
  "nluSettings": {
    "modelType": "MODEL_TYPE_ADVANCED",
    "classificationThreshold": 0.30000001,
    "modelTrainingMode": "MODEL_TRAINING_MODE_AUTOMATIC"
  }
}
