package miniplc0java.tokenizer;

import miniplc0java.error.ErrorCode;
import miniplc0java.error.TokenizeError;
import miniplc0java.util.Pos;

public class Tokenizer {
    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        }
        else if(peek=='\"'){
            return lexStringLiteral();
        }
        else if (peek=='\''){
            return lexCharLiteral();
        }
        else if (Character.isLetter(peek)||peek=='_') {
            return lexIdentOrKeyword();
        }

        else {
            return lexOperatorOrUnknown();
        }
    }
    private Token lexCharLiteral() throws TokenizeError{
        Pos st = it.currentPos();
        Token token;
        it.nextChar();
        if (it.peekChar()=='\\'){
            it.nextChar();
            switch (it.nextChar()){
                case 'n':
                    token = new Token(TokenType.UINT_LITERAL,(int)'\n',st, it.currentPos());
                    break;
                case 't':
                    token = new Token(TokenType.UINT_LITERAL,(int)'\t',st, it.currentPos());
                    break;
                case 'r':
                    token =  new Token(TokenType.UINT_LITERAL,(int)'\r',st, it.currentPos());
                    break;
                case '\'':
                    token =  new Token(TokenType.UINT_LITERAL,(int)'\'',st, it.currentPos());
                    break;
                case '\\':
                    token =  new Token(TokenType.UINT_LITERAL,(int)'\\',st, it.currentPos());
                    break;
                case '\"':
                    token =  new Token(TokenType.UINT_LITERAL,(int)'\"',st, it.currentPos());
                    break;
                default:
                    throw new TokenizeError(ErrorCode.InvalidChar, it.currentPos());
            }
        }
        else {
            token =  new Token(TokenType.UINT_LITERAL,(int)it.nextChar(),st, it.currentPos());
        }
        if (it.nextChar()!='\''){
            throw new TokenizeError(ErrorCode.InvalidChar, it.currentPos());
        }
        return token;
    }


    private Token lexUInt() throws TokenizeError {
        boolean isDouble = false;
        StringBuilder stringBuilder = new StringBuilder();
        Pos start = it.currentPos();
        while(!it.isEOF()) {
            char peek = it.peekChar();
            if (Character.isDigit(peek)) {
                stringBuilder.append(it.nextChar());
            } else if (peek == '.' && !isDouble) {
                stringBuilder.append(it.nextChar());
                if (Character.isDigit(it.peekChar())) {
                    isDouble = true;
                } else {
                    throw new TokenizeError(ErrorCode.InvalidDouble ,it.currentPos());
                }
            } else if ((peek == 'e' || peek == 'E') && isDouble) {
                stringBuilder.append(it.nextChar());
                peek = it.peekChar();
                if (peek == '+' || peek == '-') {
                    stringBuilder.append(it.nextChar());
                }
                if (Character.isDigit(it.peekChar())) {
                    isDouble = true;
                } else {
                    throw new TokenizeError(ErrorCode.InvalidDouble ,it.currentPos());
                }
            } else {
                break;
            }
        }
        String result = stringBuilder.toString();
        Pos end = it.currentPos();
        if (isDouble) {
            try {
                return new Token(TokenType.DOUBLE_LITERAL, Double.parseDouble(result), start, end);
            } catch (NumberFormatException e) {
                throw new TokenizeError(ErrorCode.DoubleOverflow ,it.currentPos());
            }
        } else {
            try {
                return new Token(TokenType.UINT_LITERAL, Integer.parseInt(result), start, end);
            } catch (NumberFormatException e) {
                return new Token(TokenType.UINT_LITERAL, Long.parseLong(result), start, end);
//                throw new TokenizeError(ErrorCode.IntegerOverflow ,it.currentPos());
            }

        }}
    private Token lexIdentOrKeyword() throws TokenizeError {

            Pos start = new Pos(it.currentPos().row,it.currentPos().col);
            String s = "";
            char c;
            do {
                s += it.nextChar();
                c = it.peekChar();
            }while (Character.isLetterOrDigit(c)||c=='_');
            if (s.equals("fn")) return new Token(TokenType.FN_KW,s,start,it.currentPos());
            if (s.equals("let")) return new Token(TokenType.LET_KW,s,start,it.currentPos());
            if (s.equals("const")) return new Token(TokenType.CONST_KW,s,start,it.currentPos());
            if (s.equals("as")) return new Token(TokenType.AS_KW,s,start,it.currentPos());
            if (s.equals("while")) return new Token(TokenType.WHILE_KW,s,start,it.currentPos());
            if (s.equals("if")) return new Token(TokenType.IF_KW,s,start,it.currentPos());
            if (s.equals("else")) return new Token(TokenType.ELSE_KW,s,start,it.currentPos());
            if (s.equals("return")) return new Token(TokenType.RETURN_KW,s,start,it.currentPos());
            if (s.equals("break")) return new Token(TokenType.BREAK_KW,s,start,it.currentPos());
            if (s.equals("continue")) return new Token(TokenType.CONTINUE_KW,s,start,it.currentPos());
            return new Token(TokenType.IDENT,s,start, it.currentPos());

    }
    private Token lexStringLiteral() throws TokenizeError {
        StringBuilder stringBuilder = new StringBuilder();
        Pos start = it.currentPos();
        it.nextChar();
        while(!it.isEOF()) {
            char peek = it.peekChar();
            if (peek == '\\') {
//                stringBuilder.append(it.nextChar());
                it.nextChar();
                peek = it.peekChar();
                switch (peek) {
                    case '\'':
                        stringBuilder.append('\'');
                        break;
                    case '\"':
                        stringBuilder.append('\"');
                        break;
                    case '\\':
                        stringBuilder.append('\\');
                        break;
                    case 'n':
                        stringBuilder.append('\n');
                        break;
                    case 't':
                        stringBuilder.append('\t');
                        break;
                    case 'r':
                        stringBuilder.append('\r');
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidStringLiteral ,it.currentPos());
                }
                it.nextChar();
            } else if (peek == '\"') {
                it.nextChar();
                break;
            } else {
                stringBuilder.append(it.nextChar());
            }
        }
        String result = stringBuilder.toString();
        Pos end = it.currentPos();
        return new Token(TokenType.STRING_LITERAL, result, start, end);
    }
    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '-':
                if (it.peekChar()=='>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW,"->", it.previousPos(), it.currentPos());
                }

                return new Token(TokenType.MINUS,'-', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MUL,'*', it.previousPos(), it.currentPos());
            case '/':
                if (it.peekChar()=='/'){
                    while (it.nextChar()!='\n'){
                    }
                    return nextToken();
                }
                return new Token(TokenType.DIV,'/', it.previousPos(), it.currentPos());
            case '=':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.EQ,"==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN,'=', it.previousPos(), it.currentPos());
            case '!':
                if (it.nextChar()!='=')
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.NEQ,"!=", it.previousPos(), it.currentPos());
            case '<':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.LE,"<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT,'<', it.previousPos(), it.currentPos());
            case '>':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.GE,">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT,'>', it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN,'(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN,')', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE,'{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE,'}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA,',', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON,';', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON,':', it.previousPos(), it.currentPos());
            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
