{
  "name" : "${name}",
  "displayName": "rg-${displayName}",
  "transitionRoutes": [
  <#list flowGroups as flowGroup>
	  <#list flowGroup.flows as flow>
	{
	  "intent": "${flow.flowKey.displayName}",
	  "condition": "true",
	  "triggerFulfillment": {
	    "setParameterActions": [ {
	      "parameter": "user-request",
	      "value": "${flow.flowKey.displayName}"
	    }, {
		  "parameter": "web-site",
          <#if !flow.webSite?? || flow.webSite == "null">
          "value": null
          <#else>
	      "value": "${flow.webSite}"
          </#if>
		} ]
	  },
	  "targetFlow": "${flowGroup.displayName}",
	  "name": "${flow.name}"
	}<#sep>,
</#list><#sep>,
  </#list>

  ]
}