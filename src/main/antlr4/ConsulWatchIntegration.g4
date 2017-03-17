grammar ConsulWatchIntegration;
 
prog: role* logDirective? consulServiceChange+;

role
  : 'role' id
  ;

logDirective
  : 'log' (variable | stringLiteral)
  ;

// -------------------------------------------------------------
// Service change
// -------------------------------------------------------------

consulServiceChange
  : 'consul_service_change' '{' onDef+ '}'
  ;

onDef
  : 'on' (variable | integer | literalStar) (variable | stringLiteral| id) '{' whenRoleDef+ '}'
  ;

// -------------------------------------------------------------
// When In Role
// -------------------------------------------------------------

whenRoleDef
  : 'when_role' (variable | stringLiteral| id | literalStar) '{' ( exec | template | systemService | consulServiceRegister )+ '}'
  ;

// -------------------------------------------------------------
// Service handling
// -------------------------------------------------------------

systemService
  : systemServiceAction (stringLiteral | variable | id)
  ;

systemServiceAction
  : ( 'system_service_restart' | 'system_service_start' | 'system_service_stop' | 'system_service_enable' | 'system_service_disable' )
  ;

// -------------------------------------------------------------
// Template handling
// -------------------------------------------------------------

template
  : 'template' (variable | stringLiteral) (variable | stringLiteral) obj via?
  ;

via
  : 'via' exec
  ;

consulServiceRegister
  : 'consul_service_register' (variable | stringLiteral | id) obj
  ;

// -------------------------------------------------------------
// Exec
// -------------------------------------------------------------

exec
  : 'exec' (variable | stringLiteral) onlyIf?
  ;

onlyIf
  : 'only_if' (variable | stringLiteral)
  ;

// -------------------------------------------------------------
// Common
// -------------------------------------------------------------

obj
  : '{' pair (pair)* '}'
  | '{' '}'
  ;

pair
  : id ':' value
  ;

array
  : '[' value (',' value)* ']'
  | '[' ']'
  ;

value
  : stringLiteral
  | variable
  | number
  | integer
  | array
  | obj
  | id
  ;

variable
  : scopedVariable
  | unscopedVariable
  ;

scopedVariable
  : '$' id '.' id
  ;

unscopedVariable
  : '$' id
  ;

integer
  : INT
  ;

number
  : NUMBER
  ;

stringLiteral
  : STRING_LITERAL
  ;

id
  : ID
  ;

literalStar
  : '*'
  ;

// -------------------------------------------------------------
// Basics
// -------------------------------------------------------------

STRING_LITERAL
  : '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"'
  ;

ID
  : LETTER (LETTER)*
  ;

LETTER
  : [a-zA-Z\u0080-\u00FF_-]
  ;

NUMBER
  : '-'? INT '.' [0-9]+ EXP?
  | '-'? INT EXP
  ;

EXP
   : [Ee] [+\-]? INT
   ;

INT
  : '-'? '0' | [1-9] [0-9]*
  ;

LINE_COMMENT
  : '#' ~[\r\n]* -> channel(HIDDEN)
  ;

WS
  : [ \t\r\n]+ -> skip
  ;