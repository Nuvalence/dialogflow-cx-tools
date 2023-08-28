# Configuration
Regexes, URL separators, and an allowlist of short tokens can be found in the `defaults.conf` file. Tokens in a fulfillment phrase
that match these items will be surrounded with appropriate ssml tags during import. Short tokens appearing in the allowlist will be read
verbatim. To overwrite these values, add custom values to the `config.conf` file as shown in the commented out SHORT_TOKEN_WHITELIST value.

~ placeholder for docs about custom regex matching ~

# Running Tests
If running tests through Junit/IntelliJ, you need to specify the working directory in the test's run configuration. The default is `$MODULE_WORKING_DIR`,
but using this will lead to an error when it can't find the `.conf` files required. Specify `$ProjectFileDir$` instead and it will run from the directory
up. 