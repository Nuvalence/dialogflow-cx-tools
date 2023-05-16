"eventHandlers": [ <#list model.smalltalks.defaultEventHandlers as eventName, eventContents>{
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
  }<#sep>, </#list> ]