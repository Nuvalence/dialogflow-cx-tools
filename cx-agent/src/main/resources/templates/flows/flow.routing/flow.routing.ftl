{
  "name": "${model.getUuid()}",
  "displayName": "flow.routing",
  "transitionRoutes": [ {
    "condition": "$session.params.question-type = \"faq-question\"",
    "triggerFulfillment": {

    },
    "targetPage": "faq-routing",
    "name": "${model.getUuid()}"
  }, {
    "condition": "$session.params.type != \"faq-question\"",
    "triggerFulfillment": {

    },
    "targetPage": "yes-no-routing",
    "name": "${model.getUuid()}"
  } ],
  "eventHandlers": [ <#list model.smalltalks.allEventHandlers as eventName, eventContents>{
    "event": "${eventName}",
    "triggerFulfillment": {
      "messages": [ {
        "text": {
          "text": [ ${eventContents} ]
        },
        "languageCode": "en"
      } ]
    },
    "name": "${model.getUuid()}"
  }<#sep>, </#list> ],
  "nluSettings": {
    "modelType": "MODEL_TYPE_ADVANCED",
    "classificationThreshold": 0.30000001,
    "modelTrainingMode": "MODEL_TRAINING_MODE_AUTOMATIC"
  }
}
