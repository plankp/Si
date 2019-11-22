grammar Si;

KW_INT: 'int';
KW_DOUBLE: 'double';
KW_BOOL: 'bool';
KW_CHAR: 'char';
KW_STRING: 'string';

KW_EXPR: 'expr';
KW_VAL: 'val';
KW_VAR: 'var';
KW_IN: 'in';
KW_DO: 'do';
KW_END: 'end';
KW_ALIAS: 'alias';
KW_EXPORT: 'export';
KW_DATA: 'data';
KW_INTERFACE: 'interface';
KW_NAMESPACE: 'namespace';

SYM_LPAREN: '(';
SYM_RPAREN: ')';
SYM_LCURLY: '{';
SYM_RCURLY: '}';

SYM_ARROW: '->';
SYM_COMMA: ',';
SYM_SEMI: ';';
SYM_DEFINE: '=';

SYM_TYPE_EQ: '::';
SYM_TYPE_FROM: '<:';
SYM_TYPE_TO: ':>';

SYM_LT: '<';
SYM_GT: '>';

SYM_ADD: '+';
SYM_SUB: '-';
SYM_MUL: '*';
SYM_DIV: '/';

SYM_AND: '&';
SYM_OR: '|';

IMM_INT:
    '0'
    | [1-9][0-9]*
    | '0b' [01]+
    | '0c' [0-7]+
    | '0d' [0-9]+
    | '0x' [0-9a-fA-F]+;

IMM_DOUBLE: ('0' | [1-9][0-9]*) '.' [0-9]+;

IDENTIFIER: [$_a-zA-Z][$_a-zA-Z0-9]* [?!]?;

COMMENT: '#' ~[\r\n]* -> skip;
WHITESPACE: [ \t\r\n] -> skip;

baseLevel:
    (KW_INT | KW_DOUBLE | KW_BOOL | KW_CHAR | KW_STRING) # coreNomialType
    | IDENTIFIER                                         # userDefType
    | SYM_LPAREN SYM_RPAREN                              # coreUnitType
    | SYM_LPAREN e = coreTypes SYM_RPAREN                # typeParenthesis;
tupleLevel: t += baseLevel (SYM_MUL t += baseLevel)*;
extensionLevel: t += tupleLevel (SYM_AND t += tupleLevel)*;
variantLevel: t += extensionLevel (SYM_OR t += extensionLevel)*;
parametrizeGeneric:
    base = variantLevel (
        SYM_LCURLY args += coreTypes (
            SYM_COMMA args += coreTypes
        )* SYM_RCURLY
    )?;
coreTypes: in = parametrizeGeneric (SYM_ARROW out = coreTypes)?;

genericParam:
    name = IDENTIFIER                                   # paramFreeType
    | name = IDENTIFIER SYM_TYPE_EQ bound = coreTypes   # paramEquivType
    | name = IDENTIFIER SYM_TYPE_FROM bound = coreTypes # paramAssignableFromType
    | name = IDENTIFIER SYM_TYPE_TO bound = coreTypes   # paramAssignableToType;
declGeneric:
    SYM_LCURLY args += genericParam (
        SYM_COMMA args += genericParam
    )* SYM_RCURLY;

declType:
    KW_ALIAS name = IDENTIFIER generic = declGeneric? type = coreTypes # declTypeAlias;

declVar: (form = KW_VAL | KW_VAR | KW_EXPR) name = IDENTIFIER type = coreTypes;

funcSig:
    SYM_LPAREN (in += declVar (SYM_COMMA in += declVar)*)? SYM_RPAREN out = coreTypes;
declFunc:
    evalImm = KW_EXPR? name = IDENTIFIER generic = declGeneric? sig = funcSig SYM_DEFINE e = expr;

topLevelDecl: (declType | declFunc) SYM_SEMI;

file: decls += topLevelDecl+;

expr:
    name = IDENTIFIER                                           # exprBinding
    | (IMM_INT | IMM_DOUBLE)                                    # exprImmValue
    | SYM_LPAREN (e += expr (SYM_COMMA e += expr)*)? SYM_RPAREN # exprParenthesis
    | KW_DO e += expr (SYM_SEMI e += expr)* SYM_SEMI? KW_END    # exprDoEnd
    | binding = declVar KW_IN e = expr                          # exprVarDecl
    | lhs = expr (SYM_MUL | SYM_DIV) rhs = expr                 # exprMulDiv
    | lhs = expr (SYM_ADD | SYM_SUB) rhs = expr                 # exprAddSub
    | base = expr arg = expr                                    # exprFuncCall;
