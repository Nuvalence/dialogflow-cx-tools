{
  "name": "${flow.name}",
  "displayName": "${flow.flowKey.displayName}",
  "form": {

  },
  "entryFulfillment": {
  <#if flow.fulfillment?has_content>
    "messages": [ {
      "text": {
        "text": [ "${flow.fulfillment}" ]
      },
      "languageCode": "en"
    } ]
  </#if>
  },
  <#if flow.followUp??>
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {
      "setParameterActions": [ {
        "parameter": "question-type",
        "value": <#if flow.followUp.displayName?starts_with("smalltalk")> "${flow.followUp.displayName?remove_beginning("smalltalk.")}" <#else> "follow-up" </#if>
      }, {
        "parameter": "user-request-fwd",
        "value": "${flow.followUp.displayName}"
      }, {
        "parameter": "web-site-fwd",
        "value": <#if (model.getFlow(flow.followUp).webSite)??>"${model.getFlow(flow.followUp).webSite}"<#else>null</#if>
      } ]
    },
    "targetFlow": "flow.routing",
    "name": "${model.getUuid()}"
  } ]
  <#else>
  "transitionRoutes": [ {
    "condition": "true",
    "triggerFulfillment": {

    },
    "targetFlow": "flow.confirmation",
    "name": "${model.getUuid()}"
  } ]
  </#if>
}
