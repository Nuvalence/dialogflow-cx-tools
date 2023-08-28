# Configuration
Regexes, URL separators, and an allowlist of short tokens can be found in the `defaults.conf` file. Tokens in a fulfillment phrase
that match these items will be surrounded with appropriate ssml tags during import. Short tokens appearing in the allowlist will be read
verbatim. To overwrite these values, add custom values to the `config.conf` file as shown in the example `.conf` file below:

```
{
    include required(file("cx-phrases/defaults"))
    
    # Override values specified in defaults.conf by defining a custom value like below:
    SHORT_TOKEN_WHITELIST: [ "com", "org", "net" ]

    # If you want to skip processing a certain type of token, override the value as an empty set/string:
    URL_VERBATIM_TOKENS: []
    MATCH_URL_REGEX: ""

    # Custom matchers can be defined using regexes and replacements starting with "CUSTOM_MATCH_":
    CUSTOM_MATCH_REPLACE_LIST: [ 
       {
            match: "^.*$",
            replace: "hello 1"
        }, {
            match: "^.*$",
            replace: "hello 2"
        } 
    ]
}
```

# Running Tests
If running tests through Junit/IntelliJ, you need to specify the working directory in the test's run configuration. The default is `$MODULE_WORKING_DIR`,
but using this will lead to an error when it can't find the `.conf` files required. Specify `$ProjectFileDir$` instead and it will run from the directory
up. 