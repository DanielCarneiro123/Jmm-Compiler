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

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
EXTENDS : 'extends' ;


INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDeclaration EOF
    ;

importDeclaration : 'import' ID ( '.' ID )* SEMI ;


classDeclaration
    : CLASS className=ID (EXTENDS classExtends=ID)? '{' (varDeclaration)* (methodDeclaration)* '}' #ClassStatement
    ;

varDeclaration
    : type name=ID SEMI
    | type name=ID op=LSTRAIGHT op=RSTRAIGHT SEMI
    ;


type
    : name= INT ;

methodDeclaration locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDeclaration* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : expr op= MUL expr #BinaryExpr //
    | expr op= ADD expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;



