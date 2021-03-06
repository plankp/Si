grammar Si;

KW_INT: 'int8' | 'int16' | 'int32' | 'int64';
KW_DOUBLE: 'double';
KW_BOOL: 'bool';
KW_CHAR: 'char';
KW_STRING: 'string';

KW_EXPR: 'expr';
KW_LET: 'let';
KW_MUT: 'mut';
KW_IN: 'in';
KW_DO: 'do';
KW_END: 'end';
KW_ALIAS: 'alias';
KW_NATIVE: 'native';
KW_EXPORT: 'export';
KW_IMPORT: 'import';
KW_DATA: 'data';
KW_INTERFACE: 'interface';
KW_NAMESPACE: 'namespace';
KW_IF: 'if';
KW_THEN: 'then';
KW_ELSE: 'else';
KW_AND: 'and';
KW_OR: 'or';

SYM_LPAREN: '(';
SYM_RPAREN: ')';
SYM_LCURLY: '{';
SYM_RCURLY: '}';

SYM_COMMA: ',';
SYM_SEMI: ';';
SYM_DEFINE: '=';
SYM_INFER: ':=';

SYM_TYPE_EQ: '::';

SYM_SCOPE: '\\';

SYM_LEG: '<=>';
SYM_LE: '<=';
SYM_GE: '>=';
SYM_LT: '<';
SYM_GT: '>';
SYM_EQ: '==';
SYM_NE: '!=';

SYM_NOT: '!';

SYM_ADD: '+';
SYM_SUB: '-';
SYM_MUL: '*';
SYM_DIV: '/';

SYM_AND: '&';
SYM_XOR: '^';
SYM_OR: '|';

fragment DIGIT: [0-9];

fragment DIGIT_WOZ: [1-9];

fragment DIGIT_HEX: DIGIT | [a-fA-F];

fragment UNICODE_ESC:
    'u' DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX
    | 'U' DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX;
fragment ESCAPE: '\\' ([abfnrtv"'\\] | UNICODE_ESC);

fragment INT_LEADING: '0' | DIGIT_WOZ DIGIT*;

IMM_INT:
    INT_LEADING
    | '0b' [01]+
    | '0c' [0-7]+
    | '0d' DIGIT+
    | '0x' DIGIT_HEX+;

IMM_DOUBLE: INT_LEADING '.' DIGIT+;

IMM_TRUE: 'true';
IMM_FALSE: 'false';

IMM_CHR: '\'' (ESCAPE | ~['\r\n\\]) '\'';
IMM_STR: '"' (ESCAPE | ~["\r\n\\])* '"';

IDENTIFIER: [$_a-zA-Z][$_a-zA-Z0-9]* [?!]?;

COMMENT: '#' ~[\r\n]* -> skip;
WHITESPACE: [ \t\r\n] -> skip;

namespacePath:
    root = SYM_SCOPE? parts += IDENTIFIER (
        SYM_SCOPE parts += IDENTIFIER
    )*;

typeParams:
    SYM_LCURLY types += baseLevel (SYM_COMMA types += baseLevel)* SYM_RCURLY;
baseLevel:
    (KW_INT | KW_DOUBLE | KW_BOOL | KW_CHAR | KW_STRING)                    # coreNomialType
    | base = namespacePath                                                  # userDefType
    | base = namespacePath args = typeParams                                # parametrizeGeneric
    | <assoc = right> SYM_LPAREN e = coreTypes? SYM_RPAREN out = baseLevel? # typeParenthesis;
tupleLevel: t += baseLevel (SYM_COMMA t += baseLevel)*;
extensionLevel: t += tupleLevel (SYM_AND t += tupleLevel)*;
coreTypes: t += extensionLevel (SYM_OR t += extensionLevel)*;

genericParam:
    name = IDENTIFIER                                 # paramFreeType
    | name = IDENTIFIER SYM_TYPE_EQ bound = baseLevel # paramEquivType;
declGeneric:
    SYM_LCURLY args += genericParam (
        SYM_COMMA args += genericParam
    )* SYM_RCURLY;

declType:
    KW_ALIAS name = IDENTIFIER generic = declGeneric? type = coreTypes # declTypeAlias;

funcParam: name = IDENTIFIER type = baseLevel;
funcParams:
    SYM_LPAREN (in += funcParam (SYM_COMMA in += funcParam)*)? SYM_RPAREN;

declFunc:
    evalImm = KW_EXPR? name = IDENTIFIER generic = declGeneric? params = funcParams (
        (out = coreTypes SYM_DEFINE)
        | SYM_INFER
    ) e = expr;

declNativeFunc:
    KW_NATIVE SYM_LPAREN nat = IDENTIFIER SYM_RPAREN name = IDENTIFIER params = funcParams out =
        coreTypes;

topLevelDecl:
    vis = KW_EXPORT? (declType | declFunc | declNativeFunc) SYM_SEMI;

namespaceDecl: KW_NAMESPACE ns = namespacePath SYM_SEMI;

importDecl: KW_IMPORT path = IMM_STR SYM_SEMI;

file:
    ns = namespaceDecl? imports += importDecl* decls += topLevelDecl+;

declVar:
    KW_LET mut = KW_MUT? name = IDENTIFIER (
        (type = coreTypes SYM_DEFINE)
        | SYM_INFER
    );

expr:
    base = namespacePath                                             # exprBinding
    | base = namespacePath args = typeParams                         # exprParametrize
    | IMM_INT                                                        # exprImmInt
    | IMM_DOUBLE                                                     # exprImmDouble
    | (IMM_TRUE | IMM_FALSE)                                         # exprImmBool
    | IMM_CHR                                                        # exprImmChr
    | IMM_STR                                                        # exprImmStr
    | SYM_LPAREN (e += expr (SYM_COMMA e += expr)*)? SYM_RPAREN      # exprParenthesis
    | KW_EXPR conv = typeParams e = expr                             # exprTypeCast
    | op = (SYM_NOT | SYM_ADD | SYM_SUB) base = expr                 # exprUnary
    | lhs = expr op = (SYM_MUL | SYM_DIV) rhs = expr                 # exprMulDiv
    | lhs = expr op = (SYM_ADD | SYM_SUB) rhs = expr                 # exprAddSub
    | lhs = expr SYM_LEG rhs = expr                                  # exprThreeWayCompare
    | lhs = expr op = (SYM_LT | SYM_LE | SYM_GE | SYM_GT) rhs = expr # exprRelational
    | lhs = expr op = (SYM_EQ | SYM_NE) rhs = expr                   # exprEquivalence
    | lhs = expr SYM_AND rhs = expr                                  # exprAnd
    | lhs = expr SYM_XOR rhs = expr                                  # exprXor
    | lhs = expr SYM_OR rhs = expr                                   # exprOr
    | base = expr arg = expr                                         # exprFuncCall
    | lhs = expr KW_AND rhs = expr                                   # exprCondAnd
    | lhs = expr KW_OR rhs = expr                                    # exprCondOr
    | KW_IF test = expr KW_THEN ifTrue = expr KW_ELSE ifFalse = expr # exprIfElse
    | KW_DO e += expr (SYM_SEMI e += expr)* SYM_SEMI? KW_END         # exprDoEnd
    | binding = declVar v = expr KW_IN e = expr                      # exprVarDecl;
