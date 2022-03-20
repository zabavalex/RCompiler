grammar R;
prog:   (   expr_or_assign (';'|NL)*
        |   NL
        )*
        EOF
    ;

expr_or_assign returns[String type, String additionalParam]
    :   l1=leftAssign {$type = $l1.type; $additionalParam = $l1.additionalParam;}
    |   r1=rightAssign {$type = $r1.type; $additionalParam = $r1.additionalParam;}
    |   l2=leftGlobalAssign {$type = $l2.type; $additionalParam = $l2.additionalParam;}
    |   r2=rightGlobalAssign {$type = $r2.type; $additionalParam = $r2.additionalParam;}
    |   e1=expr {$type = $e1.type; $additionalParam = $e1.additionalParam;}
    ;

leftAssign returns[String type, String additionalParam]: s1=symbol ('<-'|'=') ex1=expr_or_assign {$type = $ex1.type; $additionalParam = $ex1.additionalParam;};
leftGlobalAssign returns[String type, String additionalParam]: s1=symbol '<<-' ex1=expr_or_assign {$type = $ex1.type; $additionalParam = $ex1.additionalParam;};

rightAssign returns[String type, String additionalParam]: e1=expr '->' (n1=nextRightAssign {$n1.additionalParam = $e1.additionalParam; $n1.type = $e1.type;}|n2=nextRightGlobalAssign {$n2.additionalParam = $e1.additionalParam; $n2.type = $e1.type;})
                                {
                                    $type = $e1.type;
                                    $additionalParam = $e1.additionalParam;
                                };
nextRightAssign returns[String type, String additionalParam]: s1=symbol '->' (n1=nextRightAssign {$n1.additionalParam = $additionalParam; $n1.type = $type;}|n2=nextRightGlobalAssign {$n2.additionalParam = $additionalParam; $n2.type = $type;})
                                    | s1=symbol;


rightGlobalAssign returns [String type, String additionalParam]: e1=expr '->>' (n1=nextRightAssign {$n1.additionalParam = $e1.additionalParam; $n1.type = $e1.type;}|n2=nextRightGlobalAssign {$n2.additionalParam = $e1.additionalParam; $n2.type = $e1.type;})
                                {
                                    $type = $e1.type;
                                    $additionalParam = $e1.additionalParam;
                                };
nextRightGlobalAssign returns[String type, String additionalParam]: s1=symbol '->>' (n1=nextRightAssign {$n1.additionalParam = $additionalParam; $n1.type = $type;}|n2=nextRightGlobalAssign {$n2.additionalParam = $additionalParam; $n2.type = $type;})
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
    |   f1=function {$type = "function"; $additionalParam = $f1.additionalParam;} // define function
    |   i1=ifR {$type = $i1.type;}
    |   f2=forR {$type = $f2.type;}
    |   w1=whileR {$type = $w1.type;}
    |   r1=repeatR {$type = $r1.type;}
    |   h1=helpR {$type = $h1.type;}
    |   n1=nextR {$type = $n1.type;}
    |   b1=breakR {$type = $b1.type;}
    |   fromTo {$type = "array";}
    |   s1=symbol {$type = "symbol"; $additionalParam = $s1.text;}
    |   con1=constant {$type = $con1.type;}
    |   expr1=expression {$type = $expr1.type;}
    ;
exprWithoutSymbol returns [String type, String additionalParam]
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
        |   i1=ifR {$type = $i1.type;}
        |   f2=forR {$type = $f2.type;}
        |   w1=whileR {$type = $w1.type;}
        |   r1=repeatR {$type = $r1.type;}
        |   h1=helpR {$type = $h1.type;}
        |   n1=nextR {$type = $n1.type;}
        |   b1=breakR {$type = $b1.type;}
        |   fromTo {$type = "array";}
        |   con1=constant {$type = $con1.type;}
        |   expr1=expression {$type = $expr1.type;}
        ;
callFunction returns [String additionalParam]: s1=symbol '(' s2=sublist ')' {$additionalParam = $s1.text;};

function returns [String type, String additionalParam]: 'function' '(' parameters? ')' f1=functionBody {$type = "function"; $additionalParam = $f1.type;};

functionBody returns [String type]
    :   '{' NL? e1=exprlist NL?'}' {$type = $e1.type;}
    |   expr_or_assign {$type = "-";}
    ;

exprlist returns [String type]
    :   expr_or_assign ((';'|NL) expr_or_assign)* {$type = "-";}
    |   (expr_or_assign (';'|NL))* r1=returnFromFunction {$type = $r1.type;}
    ;

returnFromFunction returns [String type] : 'return' e1=expr {$type = $e1.type;};
parameters : symbol (',' symbol)* ;

sublist returns[String type]: s1=sub (',' sub)* {$type = $s1.type;};

sub returns [String type] :   e1=expr {$type = $e1.type;}
    |   l1=leftAssign {$type = $l1.type;}
    |   r1=rightAssign {$type = $r1.type;}
    |   '...' {$type = "-";}
    |
    ;
fromTo: (symbol|constant) ':' (symbol|constant);
symbol : ID
       | ID '['(e1=expression)?']'
       ;

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

ifR returns [String type]:   'if' '(' execCondition ')' (b1=body{$type = $b1.type;} NL? elseR?|{$type = "boolean";} ) ;
elseR:   'else' body|expr_or_assign;
execCondition: expr_or_assign;
forR returns [String type]:   'for' '(' cycleCondition ')' body {$type = "-";};
whileR returns [String type]:   'while' '(' cycleCondition ')' body {$type = "-";};
cycleCondition: (s1=symbol 'in')? e1=expr_or_assign;
body returns [String type]: '{' NL? expr_or_assign ((';'|NL) expr_or_assign)* ((';'|NL) e1=expr_or_assign {$type = $e1.type;})? NL?'}'  | e2=expr_or_assign {$type = $e2.type;};
repeatR returns [String type]:   'repeat' expr_or_assign {$type = "-";};
helpR returns [String type]:   '?' expr_or_assign {$type = "string";};
nextR returns [String type]:   'next' {$type = "-";};
breakR returns [String type]:  'break' {$type = "-";};

expression returns [String type, String additionalParams]: l1=lsum {$type = $l1.type; $additionalParams = $l1.additionalParams;};
lsum returns [String type, String additionalParams]: lmult s1=('||'|'|') expression {$type="boolean";} | l1=lmult {$type = $l1.type; $additionalParams = $l1.additionalParams;};
lmult returns [String type, String additionalParams]: lnot s1=('&&'|'&') lmult {$type = "boolean";} | l1=lnot {$type = $l1.type; $additionalParams = $l1.additionalParams;};
comp returns [String type, String additionalParams]: sum s1=('>'|'>='|'<'|'<='|'=='|'!=') comp {$type = "boolean";} | s2=sum {$type = $s2.type; $additionalParams = $s2.additionalParams;};
sum returns [String type, String additionalParams]: m1=mult s1=('+'|'-') e1=expression {$type = "number"; if($m1.type.equals("boolean") && $e1.type.equals("boolean")) {$type = "boolean";} if($m1.type.equals("symbol") && $e1.type.equals("symbol")) {$type = "symbol"; $additionalParams = $m1.additionalParams;}}| m2=mult {$type = $m2.type; $additionalParams = $m2.additionalParams;};
mult returns [String type, String additionalParams]: p2=pow s1=('*'|'/'|'%%'|'%/%') m1=mult {$type = "number"; if($p2.type.equals("boolean") && $m1.type.equals("boolean")) {$type = "boolean";} if($p2.type.equals("symbol") && $m1.type.equals("symbol")) {$type = "symbol"; $additionalParams = $p2.additionalParams;}}| p1=pow {$type = $p1.type; $additionalParams = $p1.additionalParams;};
pow returns [String type, String additionalParams]: par s1=('**'|'^') pow {$type = "number";} | p1=par {$type = $p1.type; $additionalParams = $p1.additionalParams;};
lnot returns [String type, String additionalParams]: '!'lnot {$type = "boolean";}| c1=comp {$type = $c1.type; $additionalParams = $c1.additionalParams;};
par returns [String type, String additionalParams]:s1='('(e1=expression)s2=')' {$type = $e1.type; $additionalParams = $e1.additionalParams;}|(c1=constant{$type = $c1.type;}|s3=symbol{$type = "symbol"; $additionalParams = $s3.text;});

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
