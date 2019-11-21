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
KW_NEWTYPE: 'newtype';
KW_EXPORT: 'export';
KW_DATA: 'data';
KW_INTERFACE: 'interface';
KW_NAMESPACE: 'namespace';

SYM_LPAREN: '(';
SYM_RPAREN: ')';
SYM_ARROW: '->';
SYM_COMMA: ',';
SYM_SEMI: ';';
SYM_DEFINE: '=';

SYM_EQ_TYPE: '::';
SYM_SUBTYPE: '<:';
SYM_SUPERTYPE: ':>';

SYM_LT: '<';
SYM_GT: '>';

SYM_ADD: '+';
SYM_SUB: '-';
SYM_MUL: '*';
SYM_DIV: '/';

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

coreTypes:
    (KW_INT | KW_DOUBLE | KW_BOOL | KW_CHAR | KW_STRING)                # coreNomialType
    | IDENTIFIER                                                        # userDefType
    | SYM_LPAREN (t += coreTypes (SYM_MUL t += coreTypes)*)? SYM_RPAREN # typeParenthesis
    | base = coreTypes SYM_LT args += coreTypes (
        SYM_COMMA args += coreTypes
    )* SYM_GT                                                  # parametrizeGeneric
    | <assoc = right> in = coreTypes SYM_ARROW out = coreTypes # coreFunc;

genericParam:
    name = IDENTIFIER                                   # paramFreeType
    | name = IDENTIFIER SYM_EQ_TYPE bound = coreTypes   # paramEquivType
    | name = IDENTIFIER SYM_SUBTYPE bound = coreTypes   # paramAssignableToType
    | name = IDENTIFIER SYM_SUPERTYPE bound = coreTypes # paramAssignableFromType;
declGeneric:
    SYM_LT args += genericParam (SYM_COMMA args += genericParam)* SYM_GT;

declType:
    KW_ALIAS generic = declGeneric? name = IDENTIFIER type = coreTypes     # declTypeAlias
    | KW_NEWTYPE generic = declGeneric? name = IDENTIFIER type = coreTypes # declNewType;

declVar: (form = KW_VAL | KW_VAR | KW_EXPR) name = IDENTIFIER type = coreTypes;

namedFunc:
    name = IDENTIFIER SYM_LPAREN (
        in += declVar (SYM_COMMA in += declVar)*
    )? SYM_RPAREN out = coreTypes;
declFunc:
    evalImm = KW_EXPR? generic = declGeneric? sig = namedFunc SYM_DEFINE val = expr;

topLevelDecl: (declType | declFunc) SYM_SEMI;

file: decls += topLevelDecl+;

exprSeq: e += expr SYM_COMMA (e += expr SYM_COMMA)* t += expr?;
expr:
    IDENTIFIER                                               # exprBinding
    | (IMM_INT | IMM_DOUBLE)                                 # exprImmValue
    | SYM_LPAREN e = expr SYM_RPAREN                         # exprParenthesis
    | SYM_LPAREN el = exprSeq? SYM_RPAREN                    # exprTuple
    | KW_DO e += expr (SYM_SEMI e += expr)* SYM_SEMI? KW_END # exprDoEnd
    | binding = declVar KW_IN e = expr                       # exprVarDecl
    | lhs = expr (SYM_ADD | SYM_SUB) rhs = expr              # exprAddSub
    | lhs = expr (SYM_MUL | SYM_DIV) rhs = expr              # exprMulDiv;
