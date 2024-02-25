grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSTRAIGHT : '[' ;
RSTRAIGHT : ']' ;
MUL : '*' ;
ADD : '+' ;
COMMA : ',' ;
TRUE : 'true' ;
FALSE : 'false' ;


CLASS : 'class' ;
INT : 'int' ;
DOUBLE : 'double' ;
FLOAT : 'float' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
EXTENDS : 'extends' ;
LENGTH : 'length' ;
THIS : 'this' ;


FOR : 'for' ;
WHILE : 'while' ;
IF : 'if';
ELSE : 'else';
ELSEIF : 'else if';

IMPORT : 'import' ;
STATIC : 'static' ;
MAIN : 'main' ;
VOID : 'void' ;
NEW : 'new' ;

INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDeclaration EOF
    ;

importDeclaration : IMPORT ID ( '.' ID )* SEMI ;


classDeclaration
    : CLASS className=ID (EXTENDS classExtends=ID)? LCURLY (varDeclaration)* (methodDecl)* RCURLY
    ;

varDeclaration
    : type name=ID SEMI
    | type name=ID op=LSTRAIGHT op=RSTRAIGHT SEMI
    ;

type
    : type LSTRAIGHT RSTRAIGHT  #Array
    | value=DOUBLE             #Double
    | value=FLOAT              #Float
    | value=BOOLEAN            #Boolean
    | value=INT                #Int
    | value=STRING             #String
    | value=ID                  #Id
    ;

argument
    : type argName=ID (COMMA argument)*
    ;

returnStmt
    : RETURN expr SEMI
    ;

methodDecl
    : (PUBLIC)? type methodName=ID LPAREN (argument)* RPAREN LCURLY (varDeclaration)* (stmt)* returnStmt RCURLY
    | (PUBLIC)? STATIC VOID MAIN LPAREN STRING LSTRAIGHT LSTRAIGHT RSTRAIGHT argName=ID RPAREN LCURLY (varDeclaration)* (stmt)* RCURLY
    ;

stmt
    : expr SEMI #ExprStmt
    | LCURLY ( stmt )* RCURLY #Brackets
    | ifexpr (elseifexpr)* (elseexpr)? #IfStmt
    | FOR LPAREN stmt expr SEMI expr RPAREN stmt #ForStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | var=ID EQUALS expr SEMI #Assignment
    | var=ID LSTRAIGHT expr RSTRAIGHT EQUALS expr SEMI #ArrayAssign
    ;

ifexpr
    : IF LPAREN expr RPAREN stmt;

elseifexpr
    : ELSEIF LPAREN expr RPAREN stmt;

elseexpr
    : ELSE stmt;

expr
    : LPAREN expr RPAREN #Parentesis
    | NEW INT LSTRAIGHT expr RSTRAIGHT #ArrayDeclaration
    | NEW classname=ID LPAREN (expr (COMMA expr) *)? RPAREN #NewClass
    | expr LSTRAIGHT expr RSTRAIGHT #ArraySubscript
    | className=ID expr   #ClassInstantiation
    | expr '.' value=ID LPAREN (expr (COMMA expr) *)? RPAREN #FunctionCall
    | expr '.' LENGTH #Length
    | value=THIS #Object
    | value='!' expr #Negation
    | expr op=('*' | '/') expr #BinaryOp
    | expr op=('+' | '-') expr #BinaryOp
    | expr op=('<' | '>') expr #BinaryOp
    | expr op=('<=' | '>=' | '==' | '!=' | '+=' | '-=' | '*=' | '/=') expr #BinaryOp
    | expr op='&&' expr #BinaryOp
    | expr op='||' expr #BinaryOp
    | value=INTEGER #Integer
    | value=TRUE #Identifier
    | value=FALSE #Identifier
    | value=ID #Identifier
    | value=ID op=('++' | '--') #Increment
    ;



