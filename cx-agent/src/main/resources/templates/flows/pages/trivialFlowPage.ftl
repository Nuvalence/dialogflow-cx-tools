{
  "name": "${model.getUuid()}",
  "displayName": "${displayName}-message",
  "form": {

  },
  "entryFulfillment": {
    "messages": [ {
      "text": {
        "text": [ "${fulfillment}" ]
      },
      "languageCode": "en"
    } ]
  },
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {
      <#if parameters?has_content>
        "setParameterActions": [ <#list parameters as name, value>{
          "parameter": "${name}",
          "value": "${value}"
        }<#sep>, </#list> ]
      </#if>
    },
    <#if targetFlow??>"targetFlow": "${targetFlow}",</#if><#if targetPage??>"targetPage": "${targetPage}",</#if>
    "name": "${model.getUuid()}"
  } ]
}
