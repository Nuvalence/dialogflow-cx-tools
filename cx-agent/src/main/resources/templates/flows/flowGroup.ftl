{
  "name": "${flowGroup.name}",
  "displayName": "${flowGroup.displayName}",
  "transitionRoutes": [
  <#list flowGroup.flows as flow>
  {
    "condition": "$session.params.user-request = \"${flow.flowKey.displayName}\"",
    "triggerFulfillment": {

    },
    "targetPage": "${flow.flowKey.displayName}",
    "name": "${flow.name}"
  }<#sep>,
  </#list>
  ],
  <#include "eventHandlers.ftl" parse=true>,
  "nluSettings": {
    "modelType": "MODEL_TYPE_ADVANCED",
    "classificationThreshold": 0.30000001,
    "modelTrainingMode": "MODEL_TRAINING_MODE_AUTOMATIC"
  }
}
