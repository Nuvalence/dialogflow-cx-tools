{
  "trainingPhrases": [
  <#list trainingPhrases as trainingPhrase>{
    "id": "${trainingPhrase.id}",
    "parts": [
      <#list trainingPhrase.parts as part>
        {
          "text": "${part.text}",
          "auto": ${part.auto?c}
        }<#sep>,</#list>
    ],
    "repeatCount": ${trainingPhrase.repeatCount},
    "languageCode": "${trainingPhrase.languageCode}"
  }<#sep>, </#list>
  ]
}
