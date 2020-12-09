package miniplc0java.tokenizer;

public enum TokenType {
    /** 空 */
    None,
//    /** 无符号整数 */
//    Uint,
//    /** 标识符 */
//    Ident,
//    /** Begin */
//    Begin,
//    /** End */
//    End,
//    /** Var */
//    Var,
//    /** Const */
//    Const,
//    /** Print */
//    Print,
    /** 无符号整数 */
    Uint,
    /** 浮点数常量 */
    Double,
    /** 标识符 */
    Ident,
    /** fn */
    FN_KW,
    /** let */
    LET_KW,
    /** const */
    CONST_KW,
    /** as */
    AS_KW,
    /** while */
    WHILE_KW,
    /** if */
    IF_KW,
    /** else */
    ELSE_KW,
    /** return */
    RETURN_KW,
    /** break */
    BREAK_KW,
    /** continue */
    CONTINUE_KW,
    /** String */
    String,
    /** 加号 */
    Plus,
    /** 减号 */
    Minus,
    /** 乘号 */
    Mult,
    /** 除号 */
    Div,
    /** 两个等号 */
    Equal,
    /** 等号（赋值） */
    ASSIGN,
    /** != */
    NEQ,
    /** < */
    LT,
    /** > */
    GT,
    /** <= */
    LE,
    /** >= */
    GE,
    /** 分号 */
    Semicolon,
    /** 左括号 */
    LParen,
    /** 右括号 */
    RParen,
    /** { */
    L_BRACE,
    /** } */
    R_BRACE,
    /** -> */
    ARROW,
    /** , */
    COMMA,
    /** : */
    COLON,
    /** char */
    Char,
    /** 文件尾 */
    EOF,
    /** void */
    VOID,
    /** Type_int*/
    TYPE_INT,
    TYPE_DOUBLE,
    /** 文件尾 */
    TYPE;

    @Override
    public String toString() {
        switch (this) {
            case None:
                return "none";
            case Uint:
                return "uint";
            case Char:
                return "char";
            case Double:
                return "double";
            case FN_KW:
                return "fn";
            case LET_KW:
                return "let";
            case CONST_KW:
                return "const";
            case WHILE_KW:
                return "while";
            case IF_KW:
                return "if";
            case ELSE_KW:
                return "else";
            case RETURN_KW:
                return "return";
            case BREAK_KW:
                return "break";
            case CONTINUE_KW:
                return "continue";
            case AS_KW:
                return "as";
            case String:
                return "string";
            case Div:
                return "DivisionSign";
            case Equal:
                return "Equal";
            case Ident:
                return "Identifier";
            case LParen:
                return "LeftBracket";
            case Minus:
                return "MinusSign";
            case Mult:
                return "MultiplicationSign";
            case Plus:
                return "PlusSign";
            case RParen:
                return "RightBracket";
            case Semicolon:
                return "Semicolon";
            case ASSIGN:
                return "Assign";
            case NEQ:
                return "Not Equal";
            case LT:
                return "Less than";
            case GT:
                return "Greater than";
            case LE:
                return "Less or equal";
            case GE :
                return "Greater or equal";
            case L_BRACE:
                return "L_Brace";
            case R_BRACE :
                return "R_Brace";
            case ARROW :
                return "Arrow";
            case COMMA :
                return "Comma";
            case COLON :
                return "Colon";
            case VOID:
                return "void";
            case TYPE_DOUBLE:
                return "type_double";
            case TYPE_INT:
                return "type_int";

            default:
                return "InvalidToken";
        }
    }
}
