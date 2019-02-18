CodeMirror.defineOption("localNames", [], undefined);
CodeMirror.defineMode("manchestersyntax", config => {

    var tokenTypes = {
        "some": "manchester-rest",
        "only": "manchester-rest",
        "value": "manchester-rest",
        "min": "manchester-rest",
        "max": "manchester-rest",
        "exactly": "manchester-rest",
        "and": "manchester-expr",
        "or": "manchester-expr",
        "not": "manchester-expr",
        "(": "open-par",
        ")": "close-par",
        "{": "open-par",
        "}": "close-par",
        "integer": "builtin-kw",
        "double": "builtin-kw",
        "float": "builtin-kw",
        "decimal": "builtin-kw",
        ",": "comma",
        "<": "facet",
        "<=": "facet",
        ">": "facet",
        ">=": "facet",
        "length": "facet",
        "minLength": "facet",
        "maxLength": "facet",
        "pattern": "facet",
        "langRange": "facet",
        "true" : "manchester-lit",
        "false" : "manchester-lit",
        "owl:Thing" : "builtin-kw owl-thing",
        "owl:Nothing" : "builtin-kw owl-nothing",
    };
    var tokenRegexes = {
        '<[^>]*>': "iri",
        '"(\\"|[^"])*"': "manchester-lit",
        '\\^\\^[^ ]+': "literal-datatype",
        '@[^ ]+': "lang-tag",
        '(\\+|\\-)?(\\d+(\\.\\d+)?((e|E)(\\+|\\-)?\\d+)?|\\.\\d+((e|E)(\\+|\\-)?\\d+)?)(f|F)': "manchester-lit",
        '(\\+|\\-)?\\d+\\.\\d+': "manchester-lit",
        '(\\+|\\-)?\\d+': "manchester-lit",
    };
    var delimeters = {
        "(" : "delim",
        ")" : "delim",
        "{" : "delim",
        "}" : "delim",
        "[" : "delim",
        "]" : "delim",
        " " : "delim",
        "\t" : "delim",
        "\r" : "delim",
        "\n" : "delim",
        "," : "delim"
    };

    function consumeUntilTerminator(stream, state, terminatingCharacter, inStateFlag) {
        state[inStateFlag] = true;
        var buffer = stream.next();
        var nextCharIsEscaped = false;
        while (true) {
            var ch = stream.next();
            if (ch == '\\') {
                state.nextCharIsEscaped = true;
            }
            if (!state.nextCharIsEscaped && ch == terminatingCharacter) {
                buffer += ch;
                state[inStateFlag] = false;
                return buffer;
            }
            if (ch != '\\') {
                state.nextCharIsEscaped = false;
            }
            if (ch == null) {
                // Not reached the end of the state
                return buffer;
            }
        }
    }
    function isDelimeter(ch) {
        return delimeters[ch] != null;
    }
    function peekDelimeter(stream, state) {
        return isDelimeter(stream.peek());
    }
    function nextToken(stream, state) {
        if (state.inString) {
            return new Token(consumeUntilTerminator(stream, state, '"', 'inString'), "string");
        }
        if (stream.peek() == '"') {
            return new Token(consumeUntilTerminator(stream, state, '"', 'inString'), "string");
        }
        if (peekDelimeter(stream, state)) {
            var delimeter = stream.next();
            var additionalStyle = tokenTypes[delimeter];
            return new Token(delimeter, "delim " + additionalStyle == null ? "" : additionalStyle);
        }
        var tokenBuffer = "";
        while (!peekDelimeter(stream, state)) {
            if (stream.peek() == null) {
                break;
            }
            tokenBuffer += stream.next();
        }
        var type = tokenTypes[tokenBuffer];
        if (type == null) {
            for (var regex in tokenRegexes) {
                if (tokenRegexes.hasOwnProperty(regex)) {
                    var regExp = new RegExp(regex);
                    var match = regExp.exec(tokenBuffer);
                    if (match != null) {
                        var firstMatch = match[0];
                        if (firstMatch == tokenBuffer) {
                            type = tokenRegexes[regex];
                            break;
                        }
                    }
                }
            }
        }
        if (type == null) {
            if (config.localNames.length && !config.localNames.includes(tokenBuffer)) {
                type = "manchester-error";
            } else {
                type = "word";
            }
        }
        return new Token(tokenBuffer, type);
    }
    function Token(literal, type) {
        this.literal = literal;
        this.type = type;
    }

    return {
        startState: () => {
            return {inString: false, inIRI: false, loc: "start", nextCharIsEscaped: false};
        },
        token: (stream, state) => {
            var token = nextToken(stream, state);
            return token.type;
        }
    }
});

CodeMirror.defineMIME("text/omn", "manchestersyntax");