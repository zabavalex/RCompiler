grammar R;
prog:   (   expr_or_assign (';'|NL)*
        |   NL
        )*
        EOF
    ;

expr_or_assign
    :   leftAssign
    |   rightAssign
    |   leftGlobalAssign
    |   rightGlobalAssign
    |   equation
    |   logequation

    |   expr
    ;

leftAssign: symbol ('<-'|'=') expr_or_assign;
leftGlobalAssign: symbol '<<-' expr_or_assign;

rightAssign: (leftAssign|leftGlobalAssign|rightGlobalAssign|expr) '->' nextRightAssign;
nextRightAssign: s1=symbol '->' nextRightAssign
            | s1=symbol;


rightGlobalAssign: (leftAssign|leftGlobalAssign|expr) '->>' nextRightGlobalAssign rightGlobalAssign1
                 | (leftAssign|leftGlobalAssign|expr) '->' nextRightAssign '->>' nextRightGlobalAssign rightGlobalAssign1
                 | (leftAssign|leftGlobalAssign|expr) '->>' nextRightGlobalAssign
                 | (leftAssign|leftGlobalAssign|expr) '->' nextRightAssign '->>' nextRightGlobalAssign;
rightGlobalAssign1: '->' nextRightAssign '->>' nextRightGlobalAssign rightGlobalAssign1
                  | '->' nextRightAssign '->>' nextRightGlobalAssign
                  ;
nextRightGlobalAssign: s1=symbol '->>' nextRightGlobalAssign
                  | s1=symbol;

expr returns [String type, String additionalParam]
    :   expr '[[' sublist ']' ']' {$type = "-";} // '[[' follows R's yacc grammar
    |   expr '[' sublist ']' {$type = "-";}
    |   expr ('::'|':::') expr {$type = "-";}
    |   expr ('$'|'@') expr {$type = "-";}
    |   expr ':' expr {$type = "-";}
    |   expr USER_OP expr {$type = "-";} // anything wrappedin %: '%' .* '%'
    |   '~' expr {$type = "-";}
    |   expr '~' expr {$type = "-";}
    |   f1=function {$type = "function"; $additionalParam = $f1.additionalParam;} // define function
    |   c1=callFunction {$type = "callFunction"; $additionalParam = $c1.additionalParam;} // call function
    |   s1=symbol {$type = "symbol"; $additionalParam = $s1.text;}
    |   con1=constant {$type = $con1.type;}
    |   if
    |   ifelse
    |   for
    |   while
    |   repeat
    |   help
    |   next
    |   break
    ;
exprWithoutSymbol returns [String type, String additionalParam]
        :   expr '[[' sublist ']' ']' {$type = "-";} // '[[' follows R's yacc grammar
        |   expr '[' sublist ']' {$type = "-";}
        |   expr ('::'|':::') expr {$type = "-";}
        |   expr ('$'|'@') expr {$type = "-";}
        |   <assoc=right> expr '^' expr {$type = "number";}
        |   ('-'|'+') e1=expr {$type = $e1.type;}
        |   expr ':' expr {$type = "-";}
        |   expr USER_OP expr {$type = "-";} // anything wrappedin %: '%' .* '%'
        |   expr ('*'|'/') expr {$type = "number";}
        |   expr ('+'|'-') expr {$type = "number";}
        |   expr ('>'|'>='|'<'|'<='|'=='|'!=') expr {$type = "boolean";}
        |   '!' expr {$type = "boolean";}
        |   expr ('&'|'&&') expr {$type = "boolean";}
        |   expr ('|'|'||') expr {$type = "boolean";}
        |   '~' expr {$type = "-";}
        |   expr '~' expr {$type = "-";}
        |   f1=function {$type = "function"; $additionalParam = $f1.additionalParam;} // define function
        |   c1=callFunction {$type = "callFunction"; $additionalParam = $c1.additionalParam;} // call function
        |   'if' '(' expr ')' e1=expr {$type = $e1.type;}
        |   'if' '(' expr ')' e1=expr 'else' expr {$type = $e1.type;}
        |   'for' '(' symbol 'in' expr ')' expr {$type = "-";}
        |   'while' '(' expr ')' expr {$type = "-";}
        |   'repeat' expr {$type = "-";}
        |   '?' expr {$type = "string";} // get help on expr, usually string or ID
        |   'next' {$type = "-";}
        |   'break' {$type = "-";}
        |   '(' e1=expr ')' {$type = $e1.type;}
        |   con1=constant {$type = $con1.type;}
        |   expr equation expr
        |   expr logequation expr
        ;
callFunction returns [String additionalParam]: s1=symbol '(' sublist ')' {$additionalParam = $s1.text;};

function returns [String type, String additionalParam]: 'function' '(' parameters? ')' f1=functionBody {$type = "function"; $additionalParam = $f1.type;};

functionBody returns [String type]
    :   '{' NL? e1=exprlist NL?'}' {$type = $e1.type;}
    |   expr_or_assign {$type = "-";}
    ;

exprlist returns [String type]
    :   expr_or_assign ((';'|NL) expr_or_assign?)* {$type = "-";}
    |   (expr_or_assign (';'|NL))* r1=returnFromFunction {$type = $r1.type;}
    ;
returnFromFunction returns [String type] : 'return' e1=expr {$type = $e1.type;};
parameters : form (',' form)* ;

form:   symbol
    |   rightAssign
    |   leftAssign
    ;

sublist : sub (',' sub)* ;

sub :   expr
    |   leftAssign
    |   rightAssign
    |   '...'
    |
    ;

symbol : ID;

constant returns [String type] :  STRING {$type = "string";}
            |   HEX {$type = "hex";}
            |   INT {$type = "number";}
            |   FLOAT {$type = "number";}
            |   COMPLEX {$type = "number";}
            |   'NULL' {$type = "-";}
            |   'NA'{$type = "-";}
            |   'Inf' {$type = "-";}
            |   'NaN' {$type = "-";}
            |   'TRUE' {$type = "boolean";}
            |   'FALSE' {$type = "boolean";};

calculable :  ('-'|'+')?HEX
           |   ('-'|'+')?INT
           |   ('-'|'+')?FLOAT
           |   ('-'|'+')?COMPLEX
           |   ('!')? ( 'true' | 'false' )
           |   ('-'|'+')?callFunction
           |   '(' (equation|logequation) ')'
           |   symbol
;
equation : left=term (operator=('-'|'+') right=term)* ;
term     : left=pow (operator=('*'|'/'|'%%'|'%/%') right=pow)* ;
pow      : left=factor (operator=('^'|'**') right=factor)*;
factor   : left=calculable (operator=( '>'|'>='|'<'|'<='|'=='|'!=') right=calculable )* ;

logequation : left=logterm (operator=('||'|'|') right=logterm)* ;
logterm     : left=lognot (operator=('&&'|'&') right=lognot)* ;
lognot      : left=logfactor (('!') right=logfactor)*;
logfactor   : left=calculable (operator=('>'|'>='|'<'|'<='|'=='|'!=') right=calculable )* ;

if:   'if' '(' logequation ')' expr_or_assign ;
ifelse:   'if' '(' logequation ')' (callFunction|equation|logequation) 'else' (callFunction|equation|logequation) ;
for:   'for' '(' symbol 'in' (callFunction|symbol) ')' expr_or_assign ;
while:   'while' '(' logfactor ')' expr_or_assign ;
repeat:   'repeat' expr_or_assign ;
help:   '?' expr_or_assign ; // get help on expr, usually string or ID
next:   'next' ;
break:  'break' ;

HEX :   '0' ('x'|'X') HEXDIGIT+ [Ll]? ;

INT :   DIGIT+ [Ll]? ;

fragment
HEXDIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

FLOAT:  DIGIT+ '.' DIGIT* EXP? [Ll]?
    |   DIGIT+ EXP? [Ll]?
    |   '.' DIGIT+ EXP? [Ll]?
    ;
fragment
DIGIT:  '0'..'9' ; 
fragment
EXP :   ('E' | 'e') ('+' | '-')? INT ;

COMPLEX
    :   INT 'i'
    |   FLOAT 'i'
    ;

STRING
    :   '"' ( ESC | ~[\\"] )*? '"'
    |   '\'' ( ESC | ~[\\'] )*? '\''
    |   '`' ( ESC | ~[\\'] )*? '`'
    ;

fragment
ESC :   '\\' [abtnfrv"'\\]
    |   UNICODE_ESCAPE
    |   HEX_ESCAPE
    |   OCTAL_ESCAPE
    ;

fragment
UNICODE_ESCAPE
    :   '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    |   '\\' 'u' '{' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT '}'
    ;

fragment
OCTAL_ESCAPE
    :   '\\' [0-3] [0-7] [0-7]
    |   '\\' [0-7] [0-7]
    |   '\\' [0-7]
    ;

fragment
HEX_ESCAPE
    :   '\\' HEXDIGIT HEXDIGIT?
    ;

ID  :   '.' (LETTER|'_'|'.') (LETTER|DIGIT|'_'|'.')*
    |   LETTER (LETTER|DIGIT|'_'|'.')*
    ;
    
fragment LETTER  : [a-zA-Z] ;

USER_OP :   '%' .*? '%' ;

COMMENT :   '#' .*? '\r'? '\n' -> type(NL) ;

// Match both UNIX and Windows newlines
NL      :   '\r'? '\n' ;

WS      :   [ \t\u000C]+ -> skip ;
