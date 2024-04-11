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
ELLIPSIS : '...' ;
TRUE : 'true' ;
FALSE : 'false' ;


CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
EXTENDS : 'extends' ;
LENGTH : 'length' ;
THIS : 'this' ;
VOID : 'void';

FOR : 'for' ;
WHILE : 'while' ;
IF : 'if';
ELSE : 'else';

IMPORT : 'import' ;
STATIC : 'static' ;
NEW : 'new' ;

INTEGER : [0] | ([1-9][0-9]*);
ID : [a-zA-Z_$]([a-zA-Z_0-9$])* ;

SINGLE_COMMENT : '//' .*? '\n' -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* c=classDeclaration EOF
    ;

importDeclaration
    : IMPORT value+=ID ('.' value+=ID)* SEMI
    ;

classDeclaration
    : CLASS className=ID (EXTENDS extendedClass=ID)? LCURLY (varDecl)* (methodDecl)* RCURLY SEMI?
    ;

varDecl
    : type name=ID SEMI
    ;

type locals[boolean isArray=false]
    : value=INT LSTRAIGHT RSTRAIGHT {$isArray=true;} #Array
    | value=BOOLEAN            #Boolean
    | value=INT                #Int
    | value=ID                  #Id
    | value=VOID                 #Void
    ;

argument
    : type name=ID
    | type ELLIPSIS name=ID
    ;

returnStmt
    : RETURN expr SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN (argument (COMMA argument)*)? RPAREN LCURLY (varDecl)* (stmt)* returnStmt RCURLY
    | (PUBLIC {$isPublic=true;})? STATIC type name=ID LPAREN type LSTRAIGHT RSTRAIGHT argName=ID RPAREN LCURLY (varDecl)* (stmt)* RCURLY
    ;


stmt
    : expr SEMI #ExprStmt
    | LCURLY ( stmt )* RCURLY #Brackets
    | ifexpr (elseexpr) #IfStmt
    | FOR LPAREN stmt expr SEMI expr RPAREN stmt #ForStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | var=ID EQUALS expr SEMI #Assignment
    | var=ID LSTRAIGHT expr RSTRAIGHT EQUALS expr SEMI #ArrayAssign
    ;

ifexpr
    : IF LPAREN expr RPAREN stmt;


elseexpr
    : ELSE stmt;

expr
    : LPAREN expr RPAREN #Parentesis
    | NEW type LSTRAIGHT expr RSTRAIGHT #ArrayDeclaration
    | NEW classname=ID LPAREN (expr (COMMA expr) *)? RPAREN #NewClass
    | expr LSTRAIGHT expr RSTRAIGHT #ArraySubscript
    | LSTRAIGHT (expr (COMMA expr) *)? RSTRAIGHT #Arraydefinition //perguntar ao luis onde tem
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
    ;

