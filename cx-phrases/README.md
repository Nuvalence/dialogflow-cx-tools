# SSML Processing
We process a number of token types to surround with SSML tags in the Output Audio Text section of an agent fulfillment. These include by default
<i>URLs, phone numbers, percentages,</i> and <i>numbers</i>. We've defined some custom rules as well - letter sequences that don't make sense
in English, tokens that we specifically want to pronounce versus spell out, and some custom matching rules to replace very specific strings with desired
SSML. These are all done using regexes or matching within a list, and therefore are overridable and customizable.

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

    # Custom match rules can be defined using regexes and replacements in the CUSTOM_MATCH_REPLACE_LIST", along with language code values
    # to indicate the languages in which to perform the match/replace:
    CUSTOM_MATCH_REPLACE_LIST: [ 
       {
            languages: "en",
            match: "^.*$",
            replace: "hello"
        }, {
            languages: "en,es",
            match: "^.*$",
            replace: "hola"
        }, {
            languages: "all",
            match: "^.*$",
            replace: ":)"
        } 
    ]
}
```

# Running Tests
If running tests through Junit/IntelliJ, you need to specify the working directory in the test's run configuration. The default is `$MODULE_WORKING_DIR`,
but using this will lead to an error when it can't find the `.conf` files required. Specify `$ProjectFileDir$` instead and it will run from the directory
up. 