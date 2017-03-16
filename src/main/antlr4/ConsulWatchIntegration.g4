grammar ConsulWatchIntegration;
 
prog: role* consulServiceChange+;

role
  : 'role' id
  ;

// -------------------------------------------------------------
// Service change
// -------------------------------------------------------------

consulServiceChange
  : 'consul_service_change' BLOCK_START onDef+ BLOCK_END
  ;

onDef
  : 'on' (variable | integer | literalStar) (variable | stringLiteral| id) BLOCK_START whenRoleDef+ BLOCK_END
  ;

// -------------------------------------------------------------
// When In Role
// -------------------------------------------------------------

whenRoleDef
  : 'when_role' (variable | stringLiteral| id | literalStar) BLOCK_START ( exec | template | systemService | consulServiceRegister )+ BLOCK_END
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
  : 'template' (variable | stringLiteral) (variable | stringLiteral) BLOCK_START hashLikeParam* BLOCK_END via?
  ;

via
  : 'via' exec
  ;

consulServiceRegister
  : 'consul_service_register' (variable | stringLiteral | id) BLOCK_START hashLikeParam* BLOCK_END
  ;

// -------------------------------------------------------------
// Exec
// -------------------------------------------------------------

exec
  : 'exec' (variable | stringLiteral)
  ;

// -------------------------------------------------------------
// Common
// -------------------------------------------------------------

hashLikeParam
  : id ':' stringLiteral
  | id ':' variable
  | id ':' integer
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

BLOCK_START
  : '{'
  ;

BLOCK_END
  : '}'
  ;

ID
  : LETTER (LETTER)*
  ;

LETTER
  : [a-zA-Z\u0080-\u00FF_-]
  ;

INT
  : [0-9]+
  ;

LINE_COMMENT
  : '#' ~[\r\n]* -> channel(HIDDEN)
  ;

WS
  : [ \t\r\n]+ -> skip
  ;