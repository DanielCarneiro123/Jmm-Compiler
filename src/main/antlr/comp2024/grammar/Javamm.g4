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
    : CLASS className=ID (EXTENDS classExtends=ID)? LCURLY (varDeclaration)* (methodDeclaration)* RCURLY #ClassStatement
    ;

varDeclaration
    : type name=ID SEMI
    | type name=ID op=LSTRAIGHT op=RSTRAIGHT SEMI
    ;

argument
    : type argName=ID (COMMA argument)*
    ;

returnStmt
    : RETURN expression SEMI
    ;

methodDeclaration
    : (PUBLIC)? type methodName=ID LPAREN (argument)* RPAREN LCURLY (varDeclaration)* (statement)* returnStmt RCURLY
    | (PUBLIC)? STATIC VOID MAIN LPAREN STRING LSTRAIGHT LSTRAIGHT RSTRAIGHT argName=ID RPAREN LCURLY (varDeclaration)* (statement)* RCURLY
    ;

type
    : type LSTRAIGHT RSTRAIGHT  #Array
    | value= DOUBLE             #Double
    | value= FLOAT              #Float
    | value= BOOLEAN            #Boolean
    | value= INT                #Int
    | value= STRING             #String
    | value=ID                  #Id
    ;

statement
    : expression SEMI #ExprStmt
    | LCURLY ( statement )* RCURLY #Brackets
    | ifExpression (elseifExpression)* (elseExpression)? #IfStmt
    | FOR LPAREN statement expression SEMI expression RPAREN statement #ForStmt
    | WHILE LPAREN expression RPAREN statement #WhileStmt
    | var=ID EQUALS expression SEMI #Assignment
    | var=ID LSTRAIGHT expression RSTRAIGHT EQUALS expression SEMI #ArrayAssign
    ;

ifExpression
    : IF LPAREN expression RPAREN statement;

elseifExpression
    : ELSEIF LPAREN expression RPAREN statement;

elseExpression
    : ELSE statement;

expression
    : LPAREN expression RPAREN #Parentesis
    | NEW INT LSTRAIGHT expression RSTRAIGHT #ArrayDeclaration
    | NEW classname=ID LPAREN (expression (COMMA expression) *)? RPAREN #NewClass
    | expression LSTRAIGHT expression RSTRAIGHT #ArraySubscript
    | className=ID expression   #ClassInstantiation
    | expression '.' value=ID LPAREN (expression (COMMA expression) *)? RPAREN #FunctionCall
    | expression '.' LENGTH #Length
    | value = THIS #Object
    | value = '!' expression #Negation
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>') expression #BinaryOp
    | expression op=('<=' | '>=' | '==' | '!=' | '+=' | '-=' | '*=' | '/=') expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | expression op='||' expression #BinaryOp
    | value=INTEGER #Integer
    | value = TRUE #Identifier
    | value = FALSE #Identifier
    | value=ID #Identifier
    | value=ID op=('++' | '--') #Increment
    ;



