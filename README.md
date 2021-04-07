# ParserFilterMap


Язык описания операций над целочисленными массивами задан следующей грамматикой:


    <digit>   ::= “0” | “1" | “2” | “3" | “4” | “5" | “6” | “7" | “8” | “9"
    <number> ::= <digit> | <digit> <number>
    <operation> ::= “+” | “-” | “*” | “>” | “<” | “=” | “&” | “|”
    <constant-expression> ::= “-” <number> | <number>
    <binary-expression> ::= “(” <expression> <operation> <expression> “)”
    <expression> ::= “element” | <constant-expression> | <binary-expression>
    <map-call> ::= “map{” <expression> “}”
    <filter-call> ::= “filter{” <expression> “}”
    <call> ::= <map-call> | <filter-call>
    <call-chain> ::= <call> | <call> “%>%” <call-chain>
    
    
Реализован преобразователь выражений описываемых правилом `<call-chain>` в выражения вида `<filter-call> “%>%” <map-call>`, эквивалентные исходному.
